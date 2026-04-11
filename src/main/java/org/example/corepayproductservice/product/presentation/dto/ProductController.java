package org.example.corepayproductservice.prouduct.presentation.dto;

import lombok.RequiredArgsConstructor;
import org.example.corepayproductservice.prouduct.application.command.CreatedProductCommand;
import org.example.corepayproductservice.prouduct.application.command.UpdateAmountCommand;
import org.example.corepayproductservice.prouduct.application.command.UpdateCategoryCommand;
import org.example.corepayproductservice.prouduct.application.command.UpdateInfoCommand;
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
        CreatedProductCommand command = CreatedProductCommand.builder()
                .name(req.name())
                .price(req.price())
                .category(req.category())
                .discount(req.discount())
                .amount(req.amount())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(productService.creat(command));
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
        UpdateInfoCommand command = UpdateInfoCommand.builder()
                .id(id)
                .name(req.name())
                .price(req.price())
                .discount(req.discount())
                .amount(req.amount())
                .build();

        return ResponseEntity.ok(productService.updateInfo(command));
    }

    @PatchMapping("/{id}/amount")
    public ResponseEntity<Boolean> updateProductAmount(@PathVariable Long id, @RequestBody ProductUpdateAmountReq req) {
        UpdateAmountCommand command = UpdateAmountCommand.builder()
                .id(id)
                .amount(req.amount())
                .build();

        return ResponseEntity.ok(productService.updateAmount(command));
    }

    @PatchMapping("/{id}/state")
    public ResponseEntity<Void> updateProductCategory(@PathVariable Long id, @RequestBody ProductUpdateCategoryReq req) {
        UpdateCategoryCommand command =UpdateCategoryCommand.builder()
                .id(id)
                .category(req.category())
                .build();

        productService.updateCategory(command);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
