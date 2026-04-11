package org.example.corepayproductservice.prouduct.application.command;

import lombok.Builder;
import org.example.corepayproductservice.prouduct.domain.Category;

@Builder
public record UpdateCategoryCommand(
        Long id,
        Category category
) {
}
