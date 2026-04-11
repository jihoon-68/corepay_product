package org.example.corepayproductservice.prouduct.presentation.dto.req;

public record ProductInfoUpdateReq(
        String name,
        Integer price,
        Integer discount,
        Integer amount
) {
}
