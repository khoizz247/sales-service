package vn.edu.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Runs only in Maven's verify phase; Docker is required for the disposable MySQL instance. */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql")
@TestPropertySource(properties = {
        "app.seed.enabled=false",
        "spring.flyway.baseline-on-migrate=false",
        "BOOTSTRAP_ADMIN_EMAIL=test-admin@example.com",
        "BOOTSTRAP_ADMIN_PASSWORD=TestAdmin@12345"
})
class MySqlContainerIT {
    @Container
    @ServiceConnection
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("sales_service")
            .withCommand("--log-bin-trust-function-creators=1");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void flywaySchemaAndOrderCancellationWorkOnRealMySql() throws Exception {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema = DATABASE()",
                Integer.class)).isEqualTo(3);

        String adminToken = adminToken();
        String customerToken = customerToken();
        long productId = createProduct(adminToken, 3);
        JsonNode order = request(post("/api/orders"), customerToken,
                Map.of("recipientName", "Test customer", "recipientPhone", "0901234567",
                        "shippingAddress", "MySQL integration test",
                        "items", List.of(Map.of("productId", productId, "quantity", 2))), 201);
        assertThat(order.path("totalAmount").decimalValue()).isEqualByComparingTo("200.00");
        assertThat(stock(productId)).isEqualTo(1);

        JsonNode cancelled = request(patch("/api/orders/{id}/cancel", order.path("id").asLong()),
                customerToken, null, 200);
        assertThat(cancelled.path("status").asText()).isEqualTo("CANCELLED");
        assertThat(stock(productId)).isEqualTo(3);
        request(patch("/api/orders/{id}/cancel", order.path("id").asLong()),
                customerToken, null, 409);
        assertThat(stock(productId)).isEqualTo(3);
    }

    @Test
    void concurrentCustomersCannotOversellSameMySqlProduct() throws Exception {
        long productId = createProduct(adminToken(), 1);
        String firstToken = customerToken();
        String secondToken = customerToken();
        Map<String, Object> body = Map.of(
                "recipientName", "Test customer", "recipientPhone", "0901234567",
                "shippingAddress", "MySQL concurrency test",
                "items", List.of(Map.of("productId", productId, "quantity", 1)));
        String payload = json.writeValueAsString(body);
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> placeOrderAtOnce(firstToken, payload, start));
            var second = pool.submit(() -> placeOrderAtOnce(secondToken, payload, start));
            start.countDown();
            assertThat(List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(stock(productId)).isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM order_items WHERE product_id = ?", Integer.class, productId))
                    .isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private int placeOrderAtOnce(String token, String payload, CountDownLatch start) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        return mvc.perform(post("/api/orders").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andReturn().getResponse().getStatus();
    }

    private long createProduct(String token, int stock) throws Exception {
        return request(post("/api/products"), token,
                Map.of("sku", "TC-" + UUID.randomUUID(), "name", "MySQL test product",
                        "price", 100, "stockQuantity", stock), 201).path("id").asLong();
    }

    private String customerToken() throws Exception {
        return request(post("/api/auth/register"), null,
                Map.of("fullName", "MySQL test customer", "email", "tc-" + UUID.randomUUID() + "@example.com",
                        "password", "Test@12345"), 201).path("accessToken").asText();
    }

    private String adminToken() throws Exception {
        return request(post("/api/auth/login"), null,
                Map.of("email", "test-admin@example.com", "password", "TestAdmin@12345"), 200)
                .path("accessToken").asText();
    }

    private int stock(long productId) throws Exception {
        return request(get("/api/products/{id}", productId), null, null, 200)
                .path("stockQuantity").asInt();
    }

    private JsonNode request(MockHttpServletRequestBuilder builder, String token, Object body,
                             int expectedStatus) throws Exception {
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (body != null) builder.contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body));
        var response = mvc.perform(builder).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
        return json.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }
}
