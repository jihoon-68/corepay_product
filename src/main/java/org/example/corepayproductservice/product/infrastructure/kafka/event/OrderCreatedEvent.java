package org.example.corepayproductservice.product.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        Long productId,
        Integer totalPrice,
        Integer amount
) {
}
