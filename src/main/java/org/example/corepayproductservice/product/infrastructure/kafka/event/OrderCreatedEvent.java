package org.example.corepayproductservice.product.infrastructure.kafka.event;

import lombok.Builder;

import java.util.List;

@Builder
public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        Integer totalPrice,
        List<OrderItemDto> items
) {
}
