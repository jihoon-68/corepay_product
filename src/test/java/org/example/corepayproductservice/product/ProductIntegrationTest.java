package org.example.corepayproductservice.product;

import org.example.corepayproductservice.global.config.TestRedisConfig;
import org.example.corepayproductservice.product.application.ProductService;
import org.example.corepayproductservice.product.application.command.CreatedProductCommand;
import org.example.corepayproductservice.product.domain.Category;
import org.example.corepayproductservice.product.domain.Product;
import org.example.corepayproductservice.product.infrastructure.db.ProductRepository;
import org.example.corepayproductservice.product.infrastructure.kafka.event.StockIncreaseEvent;
import org.example.corepayproductservice.product.presentation.dto.res.ProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@EmbeddedKafka(partitions = 1, ports = {9092})
public class ProductIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    private CountDownLatch latch;
    private String receivedMessage;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        latch = new CountDownLatch(1);
        receivedMessage = null;

        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, 1);
        }
    }

    // 오더 서버 역활(상품 정보 요약 DB 업데이트)
    @KafkaListener(topics = "product-created-topic", groupId = "test-order-sync-group")
    public void listenProductSummary(String message) {
        this.receivedMessage = message;
        this.latch.countDown();
    }

    @Test
    @DisplayName("신규 상품이 생성되면 DB에 저장되고, 레디스에 캐싱되며, 오더 서버로 요약 정보가 발행된다.")
    void createProduct_FullFlow_Success() throws Exception {
        // Given: 신규 상품 생성 명령
        CreatedProductCommand command = CreatedProductCommand.builder()
                .name("고성능 게이밍 마우스")
                .price(89000)
                .category(Category.ELECTRONICS)
                .discount(0)
                .amount(50)
                .build();

        // When: 상품 생성 로직 실행
        ProductDto productDto = productService.creat(command);

        // Then 1: DB 저장 검증
        Product savedProduct = productRepository.findById(productDto.id()).orElseThrow();
        assertThat(savedProduct.getName()).isEqualTo("고성능 게이밍 마우스");

        // Then 2: 레디스 캐싱 검증 (ProductStock 로직 연동)
        String stockKey = "product:stock:" + productDto.id();
        String cachedStock = redisTemplate.opsForValue().get(stockKey);
        assertThat(cachedStock).isEqualTo("50");

        // Then 3: 카프카 메시지 발행 검증 (오더 서버 DB 동기화용)
        boolean messageReceived = latch.await(1, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();
        assertThat(receivedMessage).contains(productDto.id().toString());
        assertThat(receivedMessage).contains(productDto.name());
        assertThat(receivedMessage).contains(productDto.price().toString());
        assertThat(receivedMessage).contains(productDto.discount().toString());
    }
}