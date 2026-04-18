package org.example.corepayproductservice.product.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayproductservice.product.application.ProductStock;
import org.example.corepayproductservice.product.infrastructure.kafka.event.OrderCreatedEvent;
import org.example.corepayproductservice.product.infrastructure.kafka.event.StockIncreaseEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProductStock productStock;

    @KafkaListener(topics = "order-created-topic", groupId = "product-group")
    public void consumeOrderCreatedEvent(String message) {
        try {
            // 1. 수신한 JSON 문자열을 DTO 객체로 역직렬화
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

            log.info("[카프카 수신] 주문 ID: {}, 상품 ID: {}, 요청 수량: {}",
                    event.orderId(), event.productId(), event.amount());

            // 2. 레디스 원자적 재고 차감 비즈니스 로직 호출
            productStock.deductStock(event);

        } catch (JsonProcessingException e) {
            log.error("주문 생성 메시지 파싱 실패. 원본 메시지: {}", message, e);
        } catch (Exception e) {
            // 비즈니스 로직 내부에서 발생하는 예외를 잡아 컨슈머가 죽지 않도록 방어
            log.error("재고 차감 처리 중 예기치 않은 시스템 에러 발생", e);
        }
    }

    @KafkaListener(topics = "stock-increase-topic", groupId = "product-group")
    public void consumeStockIncreaseEvent(String message) {
        try {
            // 1. 수신한 JSON 문자열을 DTO 객체로 역직렬화
            StockIncreaseEvent event = objectMapper.readValue(message, StockIncreaseEvent.class);

            log.info("[카프카 수신] 상품 ID: {}, 복구 수량: {}", event.productId(), event.amount());

            // 2. 레디스 원자적 재고 차감 비즈니스 로직 호출
            productStock.increaseStock(event);

        } catch (JsonProcessingException e) {
            log.error("주문 생성 메시지 파싱 실패. 원본 메시지: {}", message, e);
        } catch (Exception e) {
            // 비즈니스 로직 내부에서 발생하는 예외를 잡아 컨슈머가 죽지 않도록 방어
            log.error("재고 차감 처리 중 예기치 않은 시스템 에러 발생", e);
        }
    }

}
