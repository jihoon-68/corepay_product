package org.example.corepayproductservice.product.presentation.dto.req;

import org.example.corepayproductservice.product.domain.Category;

public record ProductUpdateCategoryReq(
        Category category
) {
}
