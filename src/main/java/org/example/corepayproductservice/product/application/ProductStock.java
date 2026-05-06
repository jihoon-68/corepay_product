package org.example.corepayproductservice.product.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayproductservice.product.application.enums.ActionType;
import org.example.corepayproductservice.product.application.enums.CancelReason;
import org.example.corepayproductservice.product.domain.Product;
import org.example.corepayproductservice.product.infrastructure.db.ProductRepository;
import org.example.corepayproductservice.product.infrastructure.kafka.event.OrderCancelEvent;
import org.example.corepayproductservice.product.infrastructure.kafka.event.OrderCreatedEvent;
import org.example.corepayproductservice.product.infrastructure.kafka.event.OrderItemDto; // 💡 2단계에서 만든 공통 DTO
import org.example.corepayproductservice.product.infrastructure.kafka.event.StockDecrementedEvent;
import org.example.corepayproductservice.product.infrastructure.kafka.event.StockIncreaseEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStock {

    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher publisher;
    private final ProductRepository productRepository;

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    @Transactional
    public void deductStock(OrderCreatedEvent event) {
        if (isDuplicate(ActionType.DEDUCT, event.orderId())) {
            log.warn("중복된 재고 차감 요청 무시: orderId={}", event.orderId());
            return;
        }

        // 1. 데드락 방지: 상품 ID 오름차순 정렬
        List<OrderItemDto> sortedItems = sortItems(event.items());

        // 2. 롤백을 위해 차감 성공한 상품 목록을 추적
        List<OrderItemDto> deductedItems = new ArrayList<>();

        try {
            // 3. 정렬된 상품들을 순회하며 Redis 및 DB 차감 진행
            for (OrderItemDto item : sortedItems) {
                processSingleItemDeduction(event.orderId(), item);
                deductedItems.add(item); // 성공 시 리스트에 추가
            }

            // 4. 모든 상품 차감 성공 시, 결제 진행 이벤트 발행
            publishSuccessEvent(event.orderId());

        } catch (Exception e) {
            // 5. 다건 처리 중 하나라도 실패 시 (Redis 수동 롤백 + 취소 이벤트 발행)
            log.error("[재고 차감 실패] OrderId: {}, 사유: {}", event.orderId(), e.getMessage());

            for(OrderItemDto item : deductedItems){
                processSingleItemRestore(item);
            }

            publishCancelEvent(event.orderId(), CancelReason.OUT_OF_STOCK);

            log.info("[Redis 롤백 완료] 총 {}개의 상품 재고를 원상 복구했습니다.", deductedItems.size());
        }
    }

    @Transactional
    public void increaseStock(StockIncreaseEvent event) {
        if (isDuplicate(ActionType.RESTORE, event.orderId())) {
            log.warn("중복된 재고 복구 요청 무시: orderId={}", event.orderId());
            return;
        }

        // 데드락 방지를 위해 복구할 때도 정렬해서 진행
        List<OrderItemDto> sortedItems = sortItems(event.items());

        for (OrderItemDto item : sortedItems) {
            processSingleItemRestore(item);
        }

        log.info("[재고 복구 최종 완료] 주문 ID: {}", event.orderId());
    }

    // =========================================================================
    // 헬퍼 메서드 (분리된 로직들)
    // =========================================================================

    // 데드락 방지를 위한 정렬 메서드
    private List<OrderItemDto> sortItems(List<OrderItemDto> items) {
        return items.stream()
                .sorted(Comparator.comparing(OrderItemDto::productId))
                .toList();
    }

    // 단일 상품의 재고를 차감하는 세부 로직 (Redis -> DB 순서)
    private void processSingleItemDeduction(Long orderId, OrderItemDto item) {
        String stockKey = "product:stock:" + item.productId();

        // 1. 캐시 동기화
        if (!syncCacheFromDb(item.productId(), stockKey)) {
            throw new RuntimeException("상품을 찾을 수 없습니다. ProductId: " + item.productId());
        }

        // 2. Redis 원자적 차감
        Long remainStock = redisTemplate.opsForValue().decrement(stockKey, item.amount());
        if (remainStock != null && remainStock < 0) {
            // Redis 재고 부족 시 즉시 원상복구 후 예외 발생
            redisTemplate.opsForValue().increment(stockKey, item.amount());
            throw new RuntimeException("재고가 부족합니다. ProductId: " + item.productId());
        }

        // 3. DB 비관적 락 및 차감
        Product product = productRepository.findByIdWithLock(item.productId())
                .orElseThrow(() -> new RuntimeException("DB 상품 조회 실패. ProductId: " + item.productId()));

        if (!product.decreaseAmount(item.amount())) {
            // DB 상으로 재고가 부족할 경우 Redis 롤백 후 예외 발생
            redisTemplate.opsForValue().increment(stockKey, item.amount());
            throw new RuntimeException("DB 재고가 부족합니다. ProductId: " + item.productId());
        }

        productRepository.save(product);
        log.info("[단건 차감 성공] 상품 ID: {}, 남은 재고: {}", item.productId(), product.getAmount());
    }

    // 단일 상품의 재고를 복구하는 세부 로직
    private void processSingleItemRestore(OrderItemDto item) {
        String stockKey = "product:stock:" + item.productId();

        if (syncCacheFromDb(item.productId(), stockKey)) {
            redisTemplate.opsForValue().increment(stockKey, item.amount());
        }

        productRepository.findByIdWithLock(item.productId()).ifPresent(product -> {
            product.increaseAmount(item.amount());
            productRepository.save(product);
        });
    }

    // DB -> Redis 캐시 적재 로직 (기존 로직 유지)
    private boolean syncCacheFromDb(Long productId, String stockKey) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(stockKey))) {
            return true;
        }

        log.info("[캐시 미스] DB에서 상품 조회 시도. 상품 ID: {}", productId);
        Product product = productRepository.findById(productId).orElse(null);

        if (product == null) {
            log.error("[상품 찾을 수 없음] 상품 ID: {}", productId);
            return false;
        }

        redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(product.getAmount()));
        log.info("[레디스 캐시 로드 완료] 상품 ID: {}, 초기 재고: {}", product.getId(), product.getAmount());
        return true;
    }

    // 차감 성공 시 결제 진행 이벤트 발행
    private void publishSuccessEvent(Long orderId) {
        StockDecrementedEvent event = StockDecrementedEvent.builder()
                .orderId(orderId)
                .build();
        publisher.publishEvent(event);
        log.info("[재고 차감 최종 성공 이벤트 발행] OrderId: {}", orderId);
    }

    // 중복 체크 로직
    public boolean isDuplicate(ActionType type, Long orderId) {
        String key = String.format("lock:order:%s:%d", type.getPrefix(), orderId);
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", DEFAULT_TTL);
        return Boolean.FALSE.equals(result);
    }

    // 취소 이벤트 발행 로직
    private void publishCancelEvent(Long orderId, CancelReason reason) {
        OrderCancelEvent orderCancelEvent = OrderCancelEvent.builder()
                .orderId(orderId)
                .reason(reason)
                .build();
        publisher.publishEvent(orderCancelEvent);
    }
}