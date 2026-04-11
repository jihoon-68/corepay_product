package org.example.corepayproductservice.prouduct.application.command;

import lombok.Builder;
import org.example.corepayproductservice.prouduct.domain.Category;

@Builder
public record CreatedProductCommand(
        String name,
        Integer price,
        Category category,
        Integer discount,
        Integer amount
) {
}
