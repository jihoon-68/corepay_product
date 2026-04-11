package org.example.corepayproductservice.product.infrastructure.db;

import org.example.corepayproductservice.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
