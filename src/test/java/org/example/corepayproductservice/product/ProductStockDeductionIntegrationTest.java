package org.example.corepayproductservice.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.corepayproductservice.global.config.TestRedisConfig;
import org.example.corepayproductservice.product.domain.Category;
import org.example.corepayproductservice.product.domain.Product;
import org.example.corepayproductservice.product.infrastructure.db.ProductRepository;
import org.example.corepayproductservice.product.infrastructure.kafka.event.OrderCreatedEvent;
import org.example.corepayproductservice.product.infrastructure.kafka.event.OrderItemDto; // 💡 DTO 임포트 추가
import org.example.corepayproductservice.product.infrastructure.kafka.event.StockIncreaseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, ports = {9092})
@Import(TestRedisConfig.class)
public class ProductStockDeductionIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    private CountDownLatch successLatch;
    private CountDownLatch failLatch;
    private String receivedSuccessMessage;
    private String receivedFailMessage;

    // 💡 다건 테스트를 위해 두 개의 상품 ID를 관리합니다.
    private Long productId1;
    private Long productId2;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        successLatch = new CountDownLatch(1);
        failLatch = new CountDownLatch(1);
        receivedSuccessMessage = null;
        receivedFailMessage = null;

        // Given: 테스트용 기초 상품 2개 (각각 재고 10개) 세팅
        Product product1 = Product.builder()
                .name("여름 무지 티셔츠")
                .price(10000)
                .category(Category.CLOTHES)
                .amount(10)
                .discount(0) // 💡 추가: NOT NULL 제약조건 해결
                .build();

        Product product2 = Product.builder()
                .name("겨울 청바지")
                .price(30000)
                .category(Category.CLOTHES)
                .amount(10)
                .discount(0) // 💡 추가: NOT NULL 제약조건 해결
                .build();

        productRepository.saveAll(List.of(product1, product2));
        productId1 = product1.getId();
        productId2 = product2.getId();

        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, 1);
        }
    }

    @KafkaListener(topics = "stock-decremented-topic", groupId = "test-product-group")
    public void listenSuccess(String message) {
        this.receivedSuccessMessage = message;
        this.successLatch.countDown();
    }

    @KafkaListener(topics = "order-cancel-topic", groupId = "test-product-group")
    public void listenFail(String message) {
        this.receivedFailMessage = message;
        this.failLatch.countDown();
    }

    @Test
    @DisplayName("다건 주문 시 모든 상품의 재고가 충분하면 모두 차감하고 진행 이벤트를 발행한다.")
    void deductStock_MultipleItems_Success() throws Exception {
        Long orderId = 100L;
        // Given: 상품1을 2개, 상품2를 3개 주문
        List<OrderItemDto> items = List.of(
                new OrderItemDto(productId1, 2),
                new OrderItemDto(productId2, 3)
        );
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .items(items)
                .build();

        String message = objectMapper.writeValueAsString(event);

        // When: 메시지 발송
        kafkaTemplate.send("order-created-topic", message);

        // Then 1: 카프카 통신 검증
        boolean messageReceived = successLatch.await(5, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();

        // Then 2: DB 검증 (상품1: 8개 남음, 상품2: 7개 남음)
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            Product updatedProduct1 = productRepository.findById(productId1).orElseThrow();
            Product updatedProduct2 = productRepository.findById(productId2).orElseThrow();
            assertThat(updatedProduct1.getAmount()).isEqualTo(8);
            assertThat(updatedProduct2.getAmount()).isEqualTo(7);
        });
    }

    @Test
    @DisplayName("다건 주문 중 특정 상품 재고가 부족하면, 이전에 차감했던 상품의 DB와 Redis 재고를 모두 원상복구한다. (All or Nothing)")
    void deductStock_Fail_PartialOutOfStock_Rollback() throws Exception {
        Long orderId = 200L;
        // Given: 상품1은 2개(성공), 상품2는 15개(실패) 요청
        List<OrderItemDto> items = List.of(
                new OrderItemDto(productId1, 2),
                new OrderItemDto(productId2, 15) // 재고 10개이므로 예외 발생 예정
        );
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .items(items)
                .build();
        String message = objectMapper.writeValueAsString(event);

        // When: 메시지 발송
        kafkaTemplate.send("order-created-topic", message);

        // Then 1: 취소 이벤트 발행 검증
        boolean messageReceived = failLatch.await(5, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();

        // Then 2: 💡 핵심 검증 (상품1은 처음에 차감 성공했지만, 상품2 실패로 인해 상품1도 10개로 롤백되어야 함)
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            Product updatedProduct1 = productRepository.findById(productId1).orElseThrow();
            Product updatedProduct2 = productRepository.findById(productId2).orElseThrow();
            assertThat(updatedProduct1.getAmount()).isEqualTo(10); // 롤백 확인!
            assertThat(updatedProduct2.getAmount()).isEqualTo(10); // 롤백 확인!

            // Redis 캐시도 롤백되었는지 검증
            String stockKey1 = redisTemplate.opsForValue().get("product:stock:" + productId1);
            assertThat(stockKey1).isEqualTo("10");
        });
    }

    @Test
    @DisplayName("동일한 다건 주문 생성 이벤트가 여러 번 들어와도, 재고 차감은 한 번만 수행되어야 한다. (멱등성)")
    void deductStock_MultipleItems_Idempotency() throws Exception {
        Long orderId = 500L;
        List<OrderItemDto> items = List.of(
                new OrderItemDto(productId1, 1),
                new OrderItemDto(productId2, 1)
        );
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .items(items)
                .build();
        String message = objectMapper.writeValueAsString(event);

        // When: 동일한 메시지 연속 발송
        kafkaTemplate.send("order-created-topic", message);
        kafkaTemplate.send("order-created-topic", message);

        // Then: 둘 다 9개씩만 남아있어야 함
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Product updatedProduct1 = productRepository.findById(productId1).orElseThrow();
            Product updatedProduct2 = productRepository.findById(productId2).orElseThrow();
            assertThat(updatedProduct1.getAmount()).isEqualTo(9);
            assertThat(updatedProduct2.getAmount()).isEqualTo(9);
        });
    }

    @Test
    @DisplayName("결제 실패로 인한 주문 취소 이벤트를 받으면, 차감됐던 다건 재고를 다시 복구한다.")
    void restoreStock_MultipleItems_Success() throws Exception {
        // Given: 사전에 각각 2개씩 깎여 있는 상황 (현재 재고 8개)
        Product product1 = productRepository.findById(productId1).orElseThrow();
        Product product2 = productRepository.findById(productId2).orElseThrow();
        product1.decreaseAmount(2);
        product2.decreaseAmount(2);
        productRepository.saveAll(List.of(product1, product2));

        Long orderId = 300L;
        // 복구 이벤트 생성
        List<OrderItemDto> items = List.of(
                new OrderItemDto(productId1, 2),
                new OrderItemDto(productId2, 2)
        );
        StockIncreaseEvent cancelEvent = StockIncreaseEvent.builder()
                .orderId(orderId)
                .items(items)
                .build();
        String message = objectMapper.writeValueAsString(cancelEvent);

        // When: 복구 메시지 발송
        kafkaTemplate.send("stock-increase-topic", message);

        // Then: 둘 다 다시 10개로 롤백되었는지 검증
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Product updatedProduct1 = productRepository.findById(productId1).orElseThrow();
            Product updatedProduct2 = productRepository.findById(productId2).orElseThrow();
            assertThat(updatedProduct1.getAmount()).isEqualTo(10);
            assertThat(updatedProduct2.getAmount()).isEqualTo(10);
        });
    }
}