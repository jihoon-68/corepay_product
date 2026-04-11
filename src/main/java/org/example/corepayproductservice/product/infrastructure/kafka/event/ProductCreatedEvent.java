package org.example.corepayproductservice.prouduct.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record ProductCreatedEvent(
        Long productId,
        String name,
        int price,
        int discount
) {
}
