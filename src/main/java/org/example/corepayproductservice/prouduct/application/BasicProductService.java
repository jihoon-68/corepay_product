package org.example.corepayproductservice.prouduct.application;

import lombok.RequiredArgsConstructor;
import org.example.corepayproductservice.prouduct.infrastructure.kafka.ProductEventProducer;
import org.example.corepayproductservice.prouduct.infrastructure.kafka.event.ProductCreatedEvent;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductCreatReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductInfoUpdateReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductUpdateAmountReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductUpdateCategoryReq;
import org.example.corepayproductservice.prouduct.presentation.dto.res.ProductDto;
import org.example.corepayproductservice.prouduct.domain.Product;
import org.example.corepayproductservice.prouduct.infrastructure.db.ProductRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BasicProductService implements ProductService{

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final ProductEventProducer eventProducer;

    @Override
    @Transactional
    public ProductDto creat(ProductCreatReq req) {
        Product product = Product.builder()
                .name(req.name())
                .price(req.price())
                .discount(req.discount())
                .amount(req.amount())
                .category(req.category())
                .build();

        Product saveProduct = productRepository.save(product);

        // Redis에 초기 재고 세팅
        String stockKey = "product:stock:" + saveProduct.getId();
        redisTemplate.opsForValue().set(stockKey, String.valueOf(req.amount()));

        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(saveProduct.getId())
                .name(saveProduct.getName())
                .price(saveProduct.getPrice())
                .discount(saveProduct.getDiscount())
                .build();

        eventProducer.sendProductCreatedEvent(event);

        return ProductDto.from(saveProduct);
    }

    @Override
    @Transactional
    public ProductDto updateInfo(Long id, ProductInfoUpdateReq req) {
        Product product = productRepository.findById(id).orElseThrow();
        product.updateInfo(req.name(), req.price(), req.discount(), req.amount());
        Product saveProduct = productRepository.save(product);
        return ProductDto.from(saveProduct);
    }

    @Override
    @Transactional
    public boolean updateAmount(Long id, ProductUpdateAmountReq req) {
        Product product = productRepository.findById(id).orElseThrow();

        if(product.decreaseAmount(req.amount())){
            return false;
        }

        productRepository.save(product);
        return true;
    }

    @Override
    @Transactional
    public void updateCategory(Long id, ProductUpdateCategoryReq req) {
        Product product = productRepository.findById(id).orElseThrow();
        product.updateCategory(req.category());
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto get(Long id) {
        return ProductDto.from(productRepository.findById(id).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getList() {
        return productRepository.findAll().stream()
                .map(ProductDto::from)
                .collect(Collectors.toList());
}

    @Override
    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
