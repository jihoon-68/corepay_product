package org.example.corepayproductservice.prouduct.presentation.dto;

import lombok.RequiredArgsConstructor;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductCreatReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductInfoUpdateReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductUpdateAmountReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductUpdateCategoryReq;
import org.example.corepayproductservice.prouduct.presentation.dto.res.ProductDto;
import org.example.corepayproductservice.prouduct.application.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductCreatReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.creat(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.get(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getProductList() {
        return ResponseEntity.ok(productService.getList());
    }

    @PatchMapping("/{id}/info")
    public ResponseEntity<ProductDto> updateProductInfo(@PathVariable Long id, @RequestBody ProductInfoUpdateReq req) {
        return ResponseEntity.ok(productService.updateInfo(id,req));
    }

    @PatchMapping("/{id}/amount")
    public ResponseEntity<Boolean> updateProductAmount(@PathVariable Long id, @RequestBody ProductUpdateAmountReq req) {
        return ResponseEntity.ok(productService.updateAmount(id,req));
    }

    @PatchMapping("/{id}/state")
    public ResponseEntity<Void> updateProductCategory(@PathVariable Long id, @RequestBody ProductUpdateCategoryReq req) {
        productService.updateCategory(id,req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
