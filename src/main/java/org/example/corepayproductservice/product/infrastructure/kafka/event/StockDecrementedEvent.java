package org.example.corepayproductservice.product.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record StockDecrementedEvent(
        Long orderId
) {
}
