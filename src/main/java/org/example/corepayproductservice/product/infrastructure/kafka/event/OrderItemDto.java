package org.example.corepayproductservice.product.infrastructure.kafka.event;

public record OrderItemDto(
        Long productId,
        Integer amount
) {}