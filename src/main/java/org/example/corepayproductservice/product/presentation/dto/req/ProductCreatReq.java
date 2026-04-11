package org.example.corepayproductservice.product.presentation.dto.req;

import org.example.corepayproductservice.product.domain.Category;

public record ProductCreatReq(
        String name,
        Integer price,
        Category category,
        Integer discount,
        Integer amount
) {
}
