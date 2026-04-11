package org.example.corepayproductservice.product.presentation.dto.req;

public record ProductInfoUpdateReq(
        String name,
        Integer price,
        Integer discount,
        Integer amount
) {
}
