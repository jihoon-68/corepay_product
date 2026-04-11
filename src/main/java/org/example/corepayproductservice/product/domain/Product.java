package org.example.corepayproductservice.prouduct.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer discount;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column
    private LocalDateTime updatedAt;

    @Builder
    public Product(String name, Integer price, Category category, Integer discount, Integer amount){
        this.name = name;
        this.price = price;
        this.category = category;
        this.discount = discount;
        this.amount = amount;
    }

    public void updateCategory(@NotNull Category category){this.category = category;}

    public boolean decreaseAmount(@NotNull Integer amount){
        if(this.amount - amount <0) {return false;}
        this.amount -= amount;
        return true;
    }

    public void updateInfo(String name, Integer price, Integer discount, Integer amount){
        if(name != null && !Objects.equals(this.name, name)){
            this.name = name;
        }

        if(price != null && !Objects.equals(this.price, price)){
            this.price = price;
        }

        if(discount != null && !Objects.equals(this.discount, discount)){
            this.discount = discount;
        }

        if(amount != null && !Objects.equals(this.amount, amount)){
            this.amount = amount;
        }
    }
}
