package org.example.corepayproductservice.product.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record ProductCreatedEvent(
        Long productId,
        String name,
        int price,
        int discount
) {
}
