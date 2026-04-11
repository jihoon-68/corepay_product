package org.example.corepayproductservice.product.application.command;

import lombok.Builder;

@Builder
public record UpdateInfoCommand(
        Long id,
        String name,
        Integer price,
        Integer discount,
        Integer amount
) {
}
