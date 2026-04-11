package org.example.corepayproductservice.prouduct.infrastructure.db;

import org.example.corepayproductservice.prouduct.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
