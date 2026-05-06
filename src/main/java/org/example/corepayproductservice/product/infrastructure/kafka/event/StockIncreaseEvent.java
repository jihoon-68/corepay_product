package org.example.corepayproductservice.product.infrastructure.kafka.event;

import lombok.Builder;

import java.util.List;

@Builder
public record StockIncreaseEvent(
        Long orderId,
        List<OrderItemDto> items
) {
}
