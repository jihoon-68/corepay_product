package org.example.corepayproductservice.prouduct.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayproductservice.prouduct.infrastructure.kafka.ProductEventProducer;
import org.example.corepayproductservice.prouduct.infrastructure.kafka.event.OrderCancelEvent;
import org.example.corepayproductservice.prouduct.infrastructure.kafka.event.OrderCreatedEvent;
import org.example.corepayproductservice.prouduct.infrastructure.kafka.event.StockDecrementedEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStock {
    private final StringRedisTemplate redisTemplate;
    private final ProductEventProducer eventProducer;

    public void deductStock(OrderCreatedEvent event) {
        String stockKey = "product:stock:" + event.productId();

        // 1. 레디스의 decrement 연산을 이용한 원자적 재고 차감
        // 멀티 스레드 환경에서도 레디스가 한 줄 세우기로 차감을 보장합니다.
        Long remainStock = redisTemplate.opsForValue().decrement(stockKey, event.amount());

        // 2. 차감된 결과가 0 미만인 경우 (재고 부족 / Overselling)
        if (remainStock != null && remainStock < 0) {
            log.warn("[재고 부족] 주문 ID: {}, 상품 ID: {}", event.orderId(), event.productId());

            // 마이너스 처리된 재고를 원상 복구 (롤백)
            redisTemplate.opsForValue().increment(stockKey, event.amount());

            // 보상 트랜잭션 이벤트 발행 예정 (주문 서버로 취소 요청)
            eventProducer.sendOrderCancelEvent(new OrderCancelEvent(event.orderId(), "OUT_OF_STOCK"));
            return;
        }

        // 3. 재고 차감 성공 시
        log.info("[재고 차감 성공] 주문 ID: {}, 남은 재고: {}", event.orderId(), remainStock);

        // 결제 서버로 결제 진행 요청 이벤트 발행 예정
        eventProducer.sendStockDecrementedEvent(new StockDecrementedEvent(event.orderId()));
    }
}
