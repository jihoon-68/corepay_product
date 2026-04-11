package org.example.corepayproductservice.product.application.command;

import lombok.Builder;
import org.example.corepayproductservice.product.domain.Category;

@Builder
public record UpdateCategoryCommand(
        Long id,
        Category category
) {
}
