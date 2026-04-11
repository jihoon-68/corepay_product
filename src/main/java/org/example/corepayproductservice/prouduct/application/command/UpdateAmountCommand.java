package org.example.corepayproductservice.prouduct.application.command;

import lombok.Builder;

@Builder
public record UpdateAmountCommand(
        Long id,
        Integer amount
) {
}
