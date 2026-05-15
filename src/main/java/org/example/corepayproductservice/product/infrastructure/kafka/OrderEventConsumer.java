package org.example.corepayproductservice.product.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaycommon.log.KafkaMdcHelper;
import org.example.corepayproductservice.product.application.ProductStock;
import org.example.corepayproductservice.product.infrastructure.kafka.event.OrderCreatedEvent;
import org.example.corepayproductservice.product.infrastructure.kafka.event.StockIncreaseEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final KafkaMdcHelper kafkaMdcHelper;
    private final ProductStock productStock;

    @KafkaListener(topics = "order-created-topic", groupId = "product-group")
    public void consumeOrderCreatedEvent(@Payload String message, @Header(value = "X-Trace-Id", required = false) String traceId) {
        kafkaMdcHelper.processEventWithMdc(traceId, message, OrderCreatedEvent.class, productStock::deductStock);
    }

    @KafkaListener(topics = "stock-increase-topic", groupId = "product-group")
    public void consumeStockIncreaseEvent(@Payload String message, @Header(value = "X-Trace-Id", required = false) String traceId) {
        kafkaMdcHelper.processEventWithMdc(traceId, message, StockIncreaseEvent.class, productStock::increaseStock);
    }

}
