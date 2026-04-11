package org.example.corepayproductservice.prouduct.infrastructure.kafka.event;

public record OrderCancelEvent(
        Long orderId,
        String reason
) {
}
