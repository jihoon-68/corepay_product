package org.example.corepayproductservice.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.corepayproductservice.global.config.TestRedisConfig;
import org.example.corepayproductservice.product.domain.Category;
import org.example.corepayproductservice.product.domain.Product;
import org.example.corepayproductservice.product.infrastructure.db.ProductRepository;
import org.example.corepayproductservice.product.infrastructure.kafka.event.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;

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
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    // 성공/실패 메시지를 각각 받을 대기열
    private CountDownLatch successLatch;
    private CountDownLatch failLatch;
    private String receivedSuccessMessage;
    private String receivedFailMessage;

    private Long productId;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        successLatch = new CountDownLatch(1);
        failLatch = new CountDownLatch(1);
        receivedSuccessMessage = null;
        receivedFailMessage = null;

        // Given: 테스트용 기초 상품 및 재고 10개 세팅
        Product product = Product.builder()
                .name("여름 무지 티셔츠")
                .price(100000)
                .category(Category.CLOTHES)
                .discount(0)
                .amount(10)
                .build();
        productRepository.save(product);
        productId = product.getId();

        // 카프카 컨슈머 준비 대기
        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, 1);
        }
    }

    // 결제 서버 역할 (재고 차감 성공 메시지 수신)
    @KafkaListener(topics = "stock-decremented-topic", groupId = "test-product-group")
    public void listenSuccess(String message) {
        this.receivedSuccessMessage = message;
        this.successLatch.countDown();
    }

    // 오더 서버 역할 (재고 부족으로 인한 취소 메시지 수신)
    @KafkaListener(topics = "order-cancel-topic", groupId = "test-product-group")
    public void listenFail(String message) {
        this.receivedFailMessage = message;
        this.failLatch.countDown();
    }

    @Test
    @DisplayName("재고가 충분하면, 재고를 차감하고 결제 서버로 진행 이벤트를 발행한다.")
    void deductStock_Success() throws Exception {
        Long orderId = 100L;
        // Given: 2개 주문 이벤트 생성 (재고 10개이므로 성공해야 함)
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .productId(productId)
                .amount(2)
                .build();
        String message = objectMapper.writeValueAsString(event);

        // When: 오더 서버인 척 '주문 생성' 메시지 발송
        kafkaTemplate.send("order-created-topic", message);

        // Then 1: 카프카 통신 검증 (결제 서버로 메시지가 나갔는가?)
        boolean messageReceived = successLatch.await(5, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();
        assertThat(receivedSuccessMessage).contains(String.valueOf(orderId));

        // Then 2: DB 검증 (재고가 10개 -> 8개로 깎였는가?)
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            Product updatedProduct = productRepository.findById(productId).orElseThrow();
            assertThat(updatedProduct.getAmount()).isEqualTo(8);
        });
    }

    @Test
    @DisplayName("재고가 부족하면(품절), 재고를 깎지 않고 오더 서버로 취소 이벤트를 발행한다.")
    void deductStock_Fail_OutOfStock() throws Exception {
        Long orderId = 200L;
        // Given: 15개 주문 이벤트 생성 (남은 재고 10개이므로 실패해야 함)
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .productId(productId)
                .amount(15)
                .build();
        String message = objectMapper.writeValueAsString(event);

        // When: 오더 서버인 척 '주문 생성' 메시지 발송
        kafkaTemplate.send("order-created-topic", message);

        // Then 1: 카프카 통신 검증 (오더 서버로 취소/보상 메시지가 나갔는가?)
        boolean messageReceived = failLatch.await(5, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();
        assertThat(receivedFailMessage).contains(String.valueOf(orderId));

        // Then 2: DB 검증 (재고가 깎이지 않고 10개 그대로 유지되었는가?)
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            Product updatedProduct = productRepository.findById(productId).orElseThrow();
            assertThat(updatedProduct.getAmount()).isEqualTo(10);
        });
    }
}