package org.example.corepayproductservice.prouduct.infrastructure.kafka.event;

public record StockDecrementedEvent(
        Long orderId
) {
}
