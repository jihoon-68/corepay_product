package org.example.corepayproductservice.product.application.command;

import lombok.Builder;

@Builder
public record UpdateAmountCommand(
        Long id,
        Integer amount
) {
}
