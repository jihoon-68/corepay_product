package org.example.corepayproductservice.product.presentation.dto.res;

import lombok.Builder;
import org.example.corepayproductservice.product.domain.Product;
import org.example.corepayproductservice.product.domain.Category;

import java.time.LocalDateTime;

@Builder
public record ProductDto(
        Long id,
        String name,
        Integer price,
        Integer discount,
        Integer amount,
        Category category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductDto from(Product product){
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .discount(product.getDiscount())
                .amount(product.getAmount())
                .category(product.getCategory())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

}
