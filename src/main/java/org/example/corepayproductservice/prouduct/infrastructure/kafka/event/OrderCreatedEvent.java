package org.example.corepayproductservice.prouduct.infrastructure.kafka.event;

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
