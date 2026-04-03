package org.example.corepayproductservice.prouduct.presentation.dto.req;

import org.example.corepayproductservice.prouduct.domain.Category;

public record ProductCreatReq(
        String name,
        Integer price,
        Category category,
        Integer discount,
        Integer amount
) {
}
