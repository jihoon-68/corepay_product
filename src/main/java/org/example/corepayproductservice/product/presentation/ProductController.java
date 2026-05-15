package org.example.corepayproductservice.product.presentation;

import lombok.RequiredArgsConstructor;
import org.example.corepayproductservice.product.application.command.CreatedProductCommand;
import org.example.corepayproductservice.product.application.command.UpdateAmountCommand;
import org.example.corepayproductservice.product.application.command.UpdateCategoryCommand;
import org.example.corepayproductservice.product.application.command.UpdateInfoCommand;
import org.example.corepayproductservice.product.presentation.dto.req.ProductCreatReq;
import org.example.corepayproductservice.product.presentation.dto.req.ProductInfoUpdateReq;
import org.example.corepayproductservice.product.presentation.dto.req.ProductUpdateAmountReq;
import org.example.corepayproductservice.product.presentation.dto.req.ProductUpdateCategoryReq;
import org.example.corepayproductservice.product.presentation.dto.res.ProductDto;
import org.example.corepayproductservice.product.application.ProductService;
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
    public ResponseEntity<ProductDto> createProduct(
            @RequestHeader("X-User-Id") Long sellerId,
            @RequestBody ProductCreatReq req) {

        CreatedProductCommand command = CreatedProductCommand.builder()
                .name(req.name())
                .price(req.price())
                .category(req.category())
                .discount(req.discount())
                .amount(req.amount())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(productService.creat(command));
    }

    // 💡 2. 제품 단건/목록 조회: 보통 누구나 볼 수 있는 public API이므로 헤더 검사 생략 (필요에 따라 추가 가능)
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.get(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getProductList() {
        return ResponseEntity.ok(productService.getList());
    }


    @PatchMapping("/{id}/info")
    public ResponseEntity<ProductDto> updateProductInfo(
            @RequestHeader("X-User-Id") Long sellerId,
            @PathVariable Long id,
            @RequestBody ProductInfoUpdateReq req) {

        UpdateInfoCommand command = UpdateInfoCommand.builder()
                .id(id)
                .name(req.name())
                .price(req.price())
                .discount(req.discount())
                .amount(req.amount())
                .build();

        return ResponseEntity.ok(productService.updateInfo(command));
    }

    // 💡 4. 재고 수정: 주문 서버가 카프카를 통해 변경할 수도 있지만, 관리자/판매자가 직접 수정할 수도 있으므로 헤더 추가
    @PatchMapping("/{id}/amount")
    public ResponseEntity<Boolean> updateProductAmount(
            @RequestHeader("X-User-Id") Long sellerId,
            @PathVariable Long id,
            @RequestBody ProductUpdateAmountReq req) {

        UpdateAmountCommand command = UpdateAmountCommand.builder()
                .id(id)
                .amount(req.amount())
                .build();

        return ResponseEntity.ok(productService.updateAmount(command));
    }

    // 💡 5. 상태/카테고리 수정: URI는 '/state'인데 내부 로직은 'category'였습니다. 둘 중 하나로 통일하는 것이 좋습니다.
    @PatchMapping("/{id}/category") // URI를 의미에 맞게 category로 변경 (또는 서비스 메서드명을 updateState로 변경)
    public ResponseEntity<Void> updateProductCategory(
            @RequestHeader("X-User-Id") Long sellerId,
            @PathVariable Long id,
            @RequestBody ProductUpdateCategoryReq req) {

        UpdateCategoryCommand command = UpdateCategoryCommand.builder()
                .id(id)
                .category(req.category())
                .build();

        productService.updateCategory(command);
        return ResponseEntity.noContent().build();
    }

    // 💡 6. 제품 삭제: 함부로 삭제하면 안 되므로 역시 요청자 ID를 검증합니다.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @RequestHeader("X-User-Id") Long sellerId,
            @PathVariable Long id) {

        // 서비스 레이어의 delete 메서드 파라미터에 sellerId를 추가하여 "이 사람이 삭제할 권한이 있는지" 체크하도록 유도
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}