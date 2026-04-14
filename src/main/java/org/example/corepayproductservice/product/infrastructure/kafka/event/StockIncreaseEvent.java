package org.example.corepayproductservice.product.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record StockIncreaseEvent(
        Long orderId,
        Long productId,
        Integer amount
) {
}
