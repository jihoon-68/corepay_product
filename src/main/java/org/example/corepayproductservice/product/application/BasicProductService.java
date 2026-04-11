package org.example.corepayproductservice.prouduct.application;

import lombok.RequiredArgsConstructor;
import org.example.corepayproductservice.prouduct.application.command.CreatedProductCommand;
import org.example.corepayproductservice.prouduct.application.command.UpdateAmountCommand;
import org.example.corepayproductservice.prouduct.application.command.UpdateCategoryCommand;
import org.example.corepayproductservice.prouduct.application.command.UpdateInfoCommand;
import org.example.corepayproductservice.prouduct.infrastructure.kafka.ProductEventProducer;
import org.example.corepayproductservice.prouduct.infrastructure.kafka.event.ProductCreatedEvent;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductCreatReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductInfoUpdateReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductUpdateAmountReq;
import org.example.corepayproductservice.prouduct.presentation.dto.req.ProductUpdateCategoryReq;
import org.example.corepayproductservice.prouduct.presentation.dto.res.ProductDto;
import org.example.corepayproductservice.prouduct.domain.Product;
import org.example.corepayproductservice.prouduct.infrastructure.db.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public ProductDto creat(CreatedProductCommand command) {
        Product product = Product.builder()
                .name(command.name())
                .price(command.price())
                .discount(command.discount())
                .amount(command.amount())
                .category(command.category())
                .build();

        Product saveProduct = productRepository.save(product);

        // Redis에 초기 재고 세팅
        String stockKey = "product:stock:" + saveProduct.getId();
        redisTemplate.opsForValue().set(stockKey, String.valueOf(command.amount()));

        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(saveProduct.getId())
                .name(saveProduct.getName())
                .price(saveProduct.getPrice())
                .discount(saveProduct.getDiscount())
                .build();

        publisher.publishEvent(event);

        return ProductDto.from(saveProduct);
    }

    @Override
    @Transactional
    public ProductDto updateInfo(UpdateInfoCommand command) {
        Product product = productRepository.findById(command.id()).orElseThrow();
        product.updateInfo(command.name(), command.price(), command.discount(), command.amount());
        Product saveProduct = productRepository.save(product);
        return ProductDto.from(saveProduct);
    }

    @Override
    @Transactional
    public boolean updateAmount(UpdateAmountCommand command) {
        Product product = productRepository.findById(command.id()).orElseThrow();

        if(product.decreaseAmount(command.amount())){
            return false;
        }

        productRepository.save(product);
        return true;
    }

    @Override
    @Transactional
    public void updateCategory(UpdateCategoryCommand command) {
        Product product = productRepository.findById(command.id()).orElseThrow();
        product.updateCategory(command.category());
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
