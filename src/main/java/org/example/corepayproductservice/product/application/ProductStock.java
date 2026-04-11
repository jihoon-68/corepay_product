package org.example.corepayproductservice.prouduct.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayproductservice.prouduct.domain.Product;
import org.example.corepayproductservice.prouduct.infrastructure.db.ProductRepository;
import org.example.corepayproductservice.prouduct.infrastructure.kafka.event.OrderCancelEvent;
import org.example.corepayproductservice.prouduct.infrastructure.kafka.event.OrderCreatedEvent;
import org.example.corepayproductservice.prouduct.infrastructure.kafka.event.StockDecrementedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStock {

    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher publisher;
    private final ProductRepository productRepository;

    @Transactional
    public void deductStock(OrderCreatedEvent event) {
        String stockKey = "product:stock:" + event.productId();

        // 캐시 동기화 (없으면 DB에서 로드, 아예 없으면 false 반환)
        if (!syncCacheFromDb(event, stockKey)) {
            return;
        }

        // 레디스 원자적 재고 차감
        Long remainStock = redisTemplate.opsForValue().decrement(stockKey, event.amount());

        // 재고 부족 시 롤백 및 실패 처리
        if (remainStock != null && remainStock < 0) {
            handleOutOfStock(event, stockKey);
            return;
        }

        // 차감 성공 시 DB 동기화 및 결제 진행 이벤트 발행
        handleSuccess(event, remainStock);
    }

    // 캐시 미스 시 DB에서 레디스로 끌어오는 로직
    private boolean syncCacheFromDb(OrderCreatedEvent event, String stockKey) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(stockKey))) {
            return true; // 이미 캐시에 있으면 패스
        }

        log.info("[캐시 미스] DB에서 상품 조회 시도. 상품 ID: {}", event.productId());
        Product product = productRepository.findById(event.productId()).orElse(null);

        if (product == null) {
            log.error("[상품 찾을 수 없음] 주문 ID: {}, 상품 ID: {}", event.orderId(), event.productId());
            publishCancelEvent(event.orderId(), "PRODUCT_NOT_FOUND");
            return false;
        }

        redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(product.getAmount()));
        log.info("[레디스 캐시 로드 완료] 상품 ID: {}, 초기 재고: {}", product.getId(), product.getAmount());
        return true;
    }

    // 오버셀링(품절) 발생 시 처리 로직
    private void handleOutOfStock(OrderCreatedEvent event, String stockKey) {
        log.warn("[재고 부족] 주문 ID: {}, 상품 ID: {}", event.orderId(), event.productId());

        // 깎은 재고 다시 복구
        redisTemplate.opsForValue().increment(stockKey, event.amount());

        // 주문 서버로 취소 이벤트 발행
        publishCancelEvent(event.orderId(), "OUT_OF_STOCK");
    }

    // 재고 차감 완벽 성공 시 처리 로직
    private void handleSuccess(OrderCreatedEvent event, Long remainStock) {
        log.info("[재고 차감 성공] 주문 ID: {}, 레디스 남은 재고: {}", event.orderId(), remainStock);

        // DB에 깎인 재고 업데이트
        productRepository.findById(event.productId()).ifPresent(product -> {
            product.decreaseAmount(event.amount());
            productRepository.save(product);
        });

        // 결제 서버로 진행 요청 이벤트 발행
        StockDecrementedEvent stockDecrementedEvent = StockDecrementedEvent.builder()
                .orderId(event.orderId())
                .build();
        publisher.publishEvent(stockDecrementedEvent);
    }

    // 취소 이벤트 발행 공통화
    private void publishCancelEvent(Long orderId, String reason) {
        OrderCancelEvent orderCancelEvent = OrderCancelEvent.builder()
                .orderId(orderId)
                .reason(reason)
                .build();
        publisher.publishEvent(orderCancelEvent);
    }
}