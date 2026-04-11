package org.example.corepayproductservice.product.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepayproductservice.product.infrastructure.kafka.ProductEventProducer;
import org.example.corepayproductservice.product.infrastructure.kafka.event.OrderCancelEvent;
import org.example.corepayproductservice.product.infrastructure.kafka.event.ProductCreatedEvent;
import org.example.corepayproductservice.product.infrastructure.kafka.event.StockDecrementedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventHandler {

    private final ProductEventProducer producer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void ProductCreatedEvent(ProductCreatedEvent event){
        producer.sendProductCreatedEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void StockDecrementedEvent(StockDecrementedEvent event){
        producer.sendStockDecrementedEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void OrderCancelEvent(OrderCancelEvent event){
        producer.sendOrderCancelEvent(event);
    }
}
