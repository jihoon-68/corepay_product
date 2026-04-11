package org.example.corepayproductservice.product.application.command;

import lombok.Builder;
import org.example.corepayproductservice.product.domain.Category;

@Builder
public record CreatedProductCommand(
        String name,
        Integer price,
        Category category,
        Integer discount,
        Integer amount
) {
}
