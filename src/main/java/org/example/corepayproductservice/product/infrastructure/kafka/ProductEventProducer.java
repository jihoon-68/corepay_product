package org.example.corepayproductservice.product.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayproductservice.product.infrastructure.kafka.event.OrderCancelEvent;
import org.example.corepayproductservice.product.infrastructure.kafka.event.ProductCreatedEvent;
import org.example.corepayproductservice.product.infrastructure.kafka.event.StockDecrementedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 1. 상품 등록 및 수정 이벤트 (오더 서버의 CQRS 동기화용)
    public void sendProductCreatedEvent(ProductCreatedEvent event) {
        sendMessage("product-created-topic", event);
    }

    // 2. 재고 차감 성공 이벤트 (결제 서버로 결제 진행 요청)
    public void sendStockDecrementedEvent(StockDecrementedEvent event) {
        sendMessage("stock-decremented-topic", event);
    }

    // 3. 보상 트랜잭션 이벤트 (재고 부족 시 오더 서버로 주문 취소 요청)
    public void sendOrderCancelEvent(OrderCancelEvent event) {
        sendMessage("order-cancel-topic", event);
    }

    // 공통 JSON 직렬화 및 카프카 발송 로직
    private void sendMessage(String topic, Object event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, message);
            log.info("[카프카 발송 성공] 토픽: {}, 메시지: {}", topic, message);
        } catch (JsonProcessingException e) {
            log.error("카프카 메시지 직렬화 에러. 토픽: {}", topic, e);
        }
    }
}
