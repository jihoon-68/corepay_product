package org.example.corepayproductservice.prouduct.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record OrderCancelEvent(
        Long orderId,
        String reason
) {
}
