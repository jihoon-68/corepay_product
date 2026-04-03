package org.example.corepayproductservice.prouduct.presentation.dto.req;

import org.example.corepayproductservice.prouduct.domain.Category;

public record ProductUpdateCategoryReq(
        Category category
) {
}
