package org.example.corepayproductservice.product.infrastructure.kafka.event;

import lombok.Builder;
import org.example.corepayproductservice.product.application.enums.CancelReason;

@Builder
public record OrderCancelEvent(
        Long orderId,
        CancelReason reason
) {
}
