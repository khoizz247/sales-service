package vn.edu.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.seed.enabled=true")
class SalesApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void customerCannotCreateUpdateOrDeleteProduct() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        long id = createProduct(admin, 5, "100.00").path("id").asLong();

        mvc.perform(withToken(post("/api/products"), customer).contentType(MediaType.APPLICATION_JSON)
                .content(productBody(5, "100.00"))).andExpect(status().isForbidden());
        mvc.perform(withToken(put("/api/products/{id}", id), customer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sửa trái phép\",\"price\":100,\"stockQuantity\":5}"))
                .andExpect(status().isForbidden());
        mvc.perform(withToken(delete("/api/products/{id}", id), customer))
                .andExpect(status().isForbidden());
        assertThat(stock(id)).isEqualTo(5);
    }

    @Test
    void adminCanManageProductAndChangesAreRecordedInInventory() throws Exception {
        String admin = adminToken();
        long actorId = response(mvc.perform(withToken(get("/api/users/me"), admin))
                .andExpect(status().isOk())).path("id").asLong();
        long id = createProduct(admin, 6, "100.00").path("id").asLong();
        mvc.perform(withToken(put("/api/products/{id}", id), admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sản phẩm mới\",\"price\":120,\"stockQuantity\":6}"))
                .andExpect(status().isOk());
        mvc.perform(withToken(patch("/api/products/{id}/stock", id), admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"stockQuantity\":4}"))
                .andExpect(status().isOk());
        assertThat(stock(id)).isEqualTo(4);
        JsonNode movements = response(mvc.perform(withToken(get("/api/admin/products/{id}/inventory", id), admin))
                .andExpect(status().isOk()));
        assertThat(movements.size()).isEqualTo(2);
        assertThat(movements.get(0).path("type").asText()).isEqualTo("OPENING_BALANCE");
        assertThat(movements.get(1).path("type").asText()).isEqualTo("ADJUSTMENT_OUT");
        assertThat(movements.get(0).path("actorUserId").asLong()).isEqualTo(actorId);
        assertThat(movements.get(1).path("actorUserId").asLong()).isEqualTo(actorId);
        mvc.perform(delete("/api/products/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(withToken(delete("/api/products/{id}", id), admin)).andExpect(status().isNoContent());
        mvc.perform(get("/api/products/{id}", id)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT status FROM products WHERE id=?", String.class, id))
                .isEqualTo("INACTIVE");
    }

    @Test
    void adminCanProvisionAnotherEmployeeOnlyAfterPasswordConfirmation() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        JsonNode office = response(mvc.perform(withToken(post("/api/admin/offices"), admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "officeCode", unique("OFF"), "name", "Văn phòng mới", "phone", "0901234567",
                        "addressLine1", "1 Hà Nội", "city", "Hà Nội", "countryCode", "VN"))))
                .andExpect(status().isCreated()));
        String email = unique("staff") + "@example.com";
        String body = json.writeValueAsString(Map.of(
                "currentPassword", "Admin@123", "fullName", "Nhân viên mới", "email", email,
                "password", "Strong@Pass123", "employeeCode", unique("EMP"),
                "officeId", office.path("id").asLong(), "jobTitle", "Nhân viên", "hireDate", "2024-01-01"));
        mvc.perform(withToken(post("/api/admin/employee-accounts"), customer)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(withToken(post("/api/admin/employee-accounts"), admin)
                .contentType(MediaType.APPLICATION_JSON).content(body.replace("Admin@123", "Wrong@123")))
                .andExpect(status().isUnauthorized());
        ObjectNode invalidOffice = (ObjectNode) json.readTree(body);
        invalidOffice.put("officeId", Long.MAX_VALUE);
        mvc.perform(withToken(post("/api/admin/employee-accounts"), admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(invalidOffice)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", "Strong@Pass123"))))
                .andExpect(status().isUnauthorized());
        JsonNode created = response(mvc.perform(withToken(post("/api/admin/employee-accounts"), admin)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()));
        assertThat(created.path("role").asText()).isEqualTo("ADMIN");
        assertThat(created.path("employee").path("userId").asLong())
                .isEqualTo(created.path("userId").asLong());
        assertThat(created.has("password")).isFalse();
        assertThat(jdbc.queryForObject("SELECT actor_user_id FROM admin_account_audit WHERE created_user_id=?",
                Long.class, created.path("userId").asLong())).isNotNull();
        JsonNode loggedIn = response(mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", "Strong@Pass123"))))
                .andExpect(status().isOk()));
        assertThat(loggedIn.path("role").asText()).isEqualTo("ADMIN");
        String newToken = loggedIn.path("accessToken").asText();
        mvc.perform(withToken(post("/api/users/me/password"), newToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Strong@Pass123\",\"newPassword\":\"New@Strong456\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", "Strong@Pass123"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", "New@Strong456"))))
                .andExpect(status().isOk());
    }

    @Test
    void searchIsPagedAndCartCheckoutIsAtomic() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        String otherCustomer = customerToken();
        String prefix = unique("PAGE");
        long chosenId = 0;
        for (int i = 0; i < 3; i++) {
            JsonNode product = response(mvc.perform(withToken(post("/api/products"), admin)
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                            "sku", prefix + i, "name", "Sản phẩm trang", "price", 100,
                            "stockQuantity", 5)))).andExpect(status().isCreated()));
            if (i == 0) chosenId = product.path("id").asLong();
        }
        JsonNode firstPage = response(mvc.perform(get("/api/products/search")
                .param("q", prefix).param("page", "0").param("size", "1"))
                .andExpect(status().isOk()));
        assertThat(firstPage.path("totalElements").asLong()).isEqualTo(3);
        assertThat(firstPage.path("items").size()).isEqualTo(1);
        mvc.perform(get("/api/products/search").param("size", "101"))
                .andExpect(status().isBadRequest());

        mvc.perform(withToken(put("/api/cart/items/{id}", chosenId), customer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":2}"))
                .andExpect(status().isOk());
        mvc.perform(withToken(get("/api/cart"), admin)).andExpect(status().isForbidden());
        assertThat(response(mvc.perform(withToken(get("/api/cart"), otherCustomer))
                .andExpect(status().isOk())).size()).isZero();
        JsonNode order = response(mvc.perform(withToken(post("/api/cart/checkout"), customer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"recipientName\":\"A\"," +
                        "\"recipientPhone\":\"0901234567\",\"shippingAddress\":\"1 Hà Nội\"}"))
                .andExpect(status().isCreated()));
        assertThat(order.path("totalAmount").decimalValue()).isEqualByComparingTo("200.00");
        assertThat(stock(chosenId)).isEqualTo(3);
        JsonNode cart = response(mvc.perform(withToken(get("/api/cart"), customer))
                .andExpect(status().isOk()));
        assertThat(cart.size()).isZero();
        mvc.perform(withToken(post("/api/cart/checkout"), customer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientName\":\"A\",\"recipientPhone\":\"0901234567\"," +
                        "\"shippingAddress\":\"1 Hà Nội\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cartRemainsIntactWhenStockChangesBeforeCheckout() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        long productId = createProduct(admin, 3, "100.00").path("id").asLong();
        mvc.perform(withToken(put("/api/cart/items/{id}", productId), customer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":3}"))
                .andExpect(status().isOk());
        mvc.perform(withToken(patch("/api/products/{id}/stock", productId), admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"stockQuantity\":1}"))
                .andExpect(status().isOk());
        mvc.perform(withToken(post("/api/cart/checkout"), customer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"recipientName\":\"A\"," +
                        "\"recipientPhone\":\"0901234567\",\"shippingAddress\":\"1 Hà Nội\"}"))
                .andExpect(status().isConflict());
        assertThat(stock(productId)).isEqualTo(1);
        assertThat(response(mvc.perform(withToken(get("/api/cart"), customer))
                .andExpect(status().isOk())).size()).isEqualTo(1);
    }

    @Test
    void duplicateSkuAndRepeatedProductLineAreRejected() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        String sku = unique("DUP");
        String product = json.writeValueAsString(Map.of("sku", sku, "name", "Sản phẩm thử",
                "price", 100, "stockQuantity", 4));
        JsonNode created = response(mvc.perform(withToken(post("/api/products"), admin)
                .contentType(MediaType.APPLICATION_JSON).content(product)).andExpect(status().isCreated()));
        mvc.perform(withToken(post("/api/products"), admin)
                .contentType(MediaType.APPLICATION_JSON).content(product)).andExpect(status().isConflict());
        long id = created.path("id").asLong();
        String repeated = json.writeValueAsString(Map.of("recipientName", "A",
                "recipientPhone", "0901234567", "shippingAddress", "1 Hà Nội",
                "items", new Object[] {Map.of("productId", id, "quantity", 1),
                        Map.of("productId", id, "quantity", 1)}));
        mvc.perform(withToken(post("/api/orders"), customer).contentType(MediaType.APPLICATION_JSON)
                .content(repeated)).andExpect(status().isBadRequest());
        assertThat(stock(id)).isEqualTo(4);
    }

    @Test
    void simultaneousOrdersCannotOversellTheSameProduct() throws Exception {
        String admin = adminToken();
        String firstCustomer = customerToken();
        String secondCustomer = customerToken();
        long productId = createProduct(admin, 1, "100.00").path("id").asLong();
        String body = orderBody(productId, 1);
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> {
                start.await();
                return mvc.perform(withToken(post("/api/orders"), firstCustomer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse().getStatus();
            });
            var second = pool.submit(() -> {
                start.await();
                return mvc.perform(withToken(post("/api/orders"), secondCustomer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse().getStatus();
            });
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(stock(productId)).isZero();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void creatingOrderDecreasesStock() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        long id = createProduct(admin, 10, "100.00").path("id").asLong();
        createOrder(customer, id, 3);
        assertThat(stock(id)).isEqualTo(7);
    }

    @Test
    void cannotOrderMoreThanAvailableStock() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        long id = createProduct(admin, 2, "100.00").path("id").asLong();
        mvc.perform(withToken(post("/api/orders"), customer).contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(id, 3))).andExpect(status().isConflict());
        assertThat(stock(id)).isEqualTo(2);
    }

    @Test
    void cancellingOrderRestoresStock() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        long productId = createProduct(admin, 8, "100.00").path("id").asLong();
        long orderId = createOrder(customer, productId, 3).path("id").asLong();
        assertThat(stock(productId)).isEqualTo(5);
        changeOrderStatus(admin, orderId, "CANCELLED").andExpect(status().isOk());
        assertThat(stock(productId)).isEqualTo(8);
    }

    @Test
    void cancellingTwiceDoesNotRestoreStockTwice() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        long productId = createProduct(admin, 8, "100.00").path("id").asLong();
        long orderId = createOrder(customer, productId, 3).path("id").asLong();
        changeOrderStatus(admin, orderId, "CANCELLED").andExpect(status().isOk());
        changeOrderStatus(admin, orderId, "CANCELLED").andExpect(status().isOk());
        assertThat(stock(productId)).isEqualTo(8);
        JsonNode movements = response(mvc.perform(withToken(
                get("/api/admin/products/{id}/inventory", productId), admin)).andExpect(status().isOk()));
        assertThat(movements.size()).isEqualTo(3); // opening balance, sale, one reversal
    }

    @Test
    void invalidOrderStatusTransitionReturnsConflict() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        long productId = createProduct(admin, 4, "100.00").path("id").asLong();
        long orderId = createOrder(customer, productId, 1).path("id").asLong();
        changeOrderStatus(admin, orderId, "COMPLETED").andExpect(status().isConflict());
        JsonNode order = response(mvc.perform(withToken(get("/api/orders/{id}", orderId), customer))
                .andExpect(status().isOk()));
        assertThat(order.path("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void protectedEndpointWithoutJwtReturnsUnauthorized() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(1, 1))).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRoleReturnsForbidden() throws Exception {
        String customer = customerToken();
        String admin = adminToken();
        mvc.perform(withToken(get("/api/admin/orders"), customer)).andExpect(status().isForbidden());
        mvc.perform(withToken(post("/api/orders"), admin).contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(1, 1))).andExpect(status().isForbidden());
    }

    @Test
    void totalAndLineTotalsAreComputedByServer() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        long first = createProduct(admin, 5, "12000.50").path("id").asLong();
        long second = createProduct(admin, 5, "3000.00").path("id").asLong();
        String body = json.writeValueAsString(Map.of(
                "recipientName", "Nguyễn Văn A", "recipientPhone", "0901234567",
                "shippingAddress", "1 Hà Nội", "items", new Object[] {
                        Map.of("productId", first, "quantity", 2),
                        Map.of("productId", second, "quantity", 3)}));
        JsonNode order = response(mvc.perform(withToken(post("/api/orders"), customer)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()));
        assertThat(order.path("totalAmount").decimalValue()).isEqualByComparingTo("33001.00");
        assertThat(order.path("items").get(0).path("lineTotal").decimalValue())
                .isEqualByComparingTo("24001.00");
        assertThat(order.path("items").get(1).path("lineTotal").decimalValue())
                .isEqualByComparingTo("9000.00");
    }

    @Test
    void customerCannotReadAnotherCustomersOrder() throws Exception {
        String admin = adminToken();
        String firstCustomer = customerToken();
        String secondCustomer = customerToken();
        long productId = createProduct(admin, 3, "100.00").path("id").asLong();
        long orderId = createOrder(firstCustomer, productId, 1).path("id").asLong();
        String code = response(mvc.perform(withToken(get("/api/orders/{id}", orderId), firstCustomer))
                .andExpect(status().isOk())).path("orderCode").asText();
        mvc.perform(withToken(get("/api/orders/{id}", orderId), secondCustomer))
                .andExpect(status().isNotFound());
        mvc.perform(withToken(get("/api/orders/{id}/payments", orderId), secondCustomer))
                .andExpect(status().isNotFound());
        JsonNode otherSearch = response(mvc.perform(withToken(get("/api/orders/me/search"), secondCustomer)
                .param("code", code)).andExpect(status().isOk()));
        assertThat(otherSearch.path("totalElements").asLong()).isZero();
        JsonNode adminSearch = response(mvc.perform(withToken(get("/api/admin/orders/search"), admin)
                .param("code", code).param("status", "PENDING").param("size", "1"))
                .andExpect(status().isOk()));
        assertThat(adminSearch.path("totalElements").asLong()).isEqualTo(1);
    }

    @Test
    void paymentMustBeRefundedBeforePaidOrderCanBeCancelled() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        long productId = createProduct(admin, 3, "100.00").path("id").asLong();
        long orderId = createOrder(customer, productId, 1).path("id").asLong();
        JsonNode payment = response(mvc.perform(withToken(post("/api/admin/orders/{id}/payments", orderId), admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"COD\",\"amount\":100}"))
                .andExpect(status().isCreated()));
        long paymentId = payment.path("id").asLong();
        changePaymentStatus(admin, paymentId, "PAID").andExpect(status().isOk());
        changeOrderStatus(admin, orderId, "CANCELLED").andExpect(status().isConflict());
        changePaymentStatus(admin, paymentId, "REFUNDED").andExpect(status().isOk());
        changeOrderStatus(admin, orderId, "CANCELLED").andExpect(status().isOk());
        assertThat(stock(productId)).isEqualTo(3);
    }

    @Test
    void customerProfileAndOrganizationApisRespectRolesAndConstraints() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        JsonNode adminMe = response(mvc.perform(withToken(get("/api/users/me"), admin)).andExpect(status().isOk()));
        JsonNode customerProfile = response(mvc.perform(withToken(get("/api/users/me/profile"), customer))
                .andExpect(status().isOk()));
        long customerId = customerProfile.path("userId").asLong();
        mvc.perform(withToken(put("/api/users/me/profile"), customer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0901234567\",\"dateOfBirth\":\"2000-01-01\"}"))
                .andExpect(status().isOk());
        JsonNode office = response(mvc.perform(withToken(post("/api/admin/offices"), admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "officeCode", unique("OFF"), "name", "Hà Nội", "phone", "0901234567",
                        "addressLine1", "1 Hà Nội", "city", "Hà Nội", "countryCode", "VN"))))
                .andExpect(status().isCreated()));
        String employeeBody = json.writeValueAsString(Map.of(
                "userId", adminMe.path("id").asLong(), "employeeCode", unique("EMP"),
                "officeId", office.path("id").asLong(), "jobTitle", "Quản trị viên", "hireDate", "2024-01-01"));
        mvc.perform(withToken(post("/api/admin/employees"), admin).contentType(MediaType.APPLICATION_JSON)
                .content(employeeBody)).andExpect(status().isCreated());
        mvc.perform(withToken(put("/api/admin/offices/{id}", office.path("id").asLong()), admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hà Nội\"," +
                        "\"phone\":\"0901234567\",\"addressLine1\":\"1 Hà Nội\"," +
                        "\"city\":\"Hà Nội\",\"countryCode\":\"VN\",\"status\":\"INACTIVE\"}"))
                .andExpect(status().isConflict());
        mvc.perform(withToken(get("/api/admin/offices"), customer)).andExpect(status().isForbidden());
        JsonNode credited = response(mvc.perform(withToken(
                patch("/api/admin/customers/{id}/credit-limit", customerId), admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"creditLimit\":1000000}"))
                .andExpect(status().isOk()));
        assertThat(credited.path("creditLimit").decimalValue()).isEqualByComparingTo("1000000");
    }

    @Test
    void categoryAndSupplierAssignmentsAreProtected() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        long productId = createProduct(admin, 2, "100.00").path("id").asLong();
        JsonNode category = response(mvc.perform(withToken(post("/api/admin/categories"), admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "code", unique("CAT"), "name", unique("Danh mục")))))
                .andExpect(status().isCreated()));
        long categoryId = category.path("id").asLong();
        mvc.perform(withToken(post("/api/admin/products/{p}/categories/{c}", productId, categoryId), admin))
                .andExpect(status().isNoContent());
        JsonNode categoryIds = response(mvc.perform(get("/api/products/{id}/categories", productId))
                .andExpect(status().isOk()));
        assertThat(categoryIds.toString()).contains(Long.toString(categoryId));
        JsonNode supplier = response(mvc.perform(withToken(post("/api/admin/suppliers"), admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "code", unique("SUP"), "name", "Nhà cung cấp thử"))))
                .andExpect(status().isCreated()));
        mvc.perform(withToken(put("/api/admin/products/{p}/suppliers/{s}", productId,
                supplier.path("id").asLong()), admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"purchasePrice\":50,\"leadTimeDays\":2,\"preferred\":true}"))
                .andExpect(status().isNoContent());
        JsonNode sources = response(mvc.perform(withToken(
                get("/api/admin/products/{id}/suppliers", productId), admin)).andExpect(status().isOk()));
        assertThat(sources.size()).isEqualTo(1);
        mvc.perform(withToken(get("/api/suppliers"), customer)).andExpect(status().isForbidden());
    }

    @Test
    void customerCanManageOnlyTheirOwnAddresses() throws Exception {
        String first = customerToken();
        String second = customerToken();
        JsonNode address = response(mvc.perform(withToken(post("/api/users/me/addresses"), first)
                .contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"Nhà\"," +
                        "\"recipientName\":\"Nguyễn Văn A\",\"recipientPhone\":\"0901234567\"," +
                        "\"addressLine1\":\"1 Hà Nội\",\"city\":\"Hà Nội\"," +
                        "\"countryCode\":\"VN\",\"isDefault\":true}"))
                .andExpect(status().isCreated()));
        long id = address.path("id").asLong();
        JsonNode mine = response(mvc.perform(withToken(get("/api/users/me/addresses"), first))
                .andExpect(status().isOk()));
        assertThat(mine.size()).isEqualTo(1);
        JsonNode others = response(mvc.perform(withToken(get("/api/users/me/addresses"), second))
                .andExpect(status().isOk()));
        assertThat(others.size()).isZero();
        mvc.perform(withToken(delete("/api/users/me/addresses/{id}", id), second))
                .andExpect(status().isNotFound());
        mvc.perform(withToken(delete("/api/users/me/addresses/{id}", id), first))
                .andExpect(status().isNoContent());
    }

    @Test
    void paidTotalCannotExceedOrderTotal() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        long productId = createProduct(admin, 3, "100.00").path("id").asLong();
        long orderId = createOrder(customer, productId, 1).path("id").asLong();
        JsonNode first = response(mvc.perform(withToken(post("/api/admin/orders/{id}/payments", orderId), admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"COD\",\"amount\":80}"))
                .andExpect(status().isCreated()));
        JsonNode second = response(mvc.perform(withToken(post("/api/admin/orders/{id}/payments", orderId), admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"COD\",\"amount\":30}"))
                .andExpect(status().isCreated()));
        changePaymentStatus(admin, first.path("id").asLong(), "PAID").andExpect(status().isOk());
        changePaymentStatus(admin, second.path("id").asLong(), "PAID").andExpect(status().isConflict());
    }

    @Test
    void catalogOrganizationAndInventoryCrudEndpointsWork() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        long productId = createProduct(admin, 5, "100.00").path("id").asLong();
        long customerId = response(mvc.perform(withToken(get("/api/users/me"), customer))
                .andExpect(status().isOk())).path("id").asLong();
        mvc.perform(get("/api/products")).andExpect(status().isOk());
        mvc.perform(withToken(get("/api/admin/customers"), admin)).andExpect(status().isOk());
        mvc.perform(withToken(get("/api/admin/customers/{id}/profile", customerId), admin))
                .andExpect(status().isOk());

        JsonNode office = response(mvc.perform(withToken(post("/api/admin/offices"), admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "officeCode", unique("OFF"), "name", "Văn phòng thử", "phone", "0901234567",
                        "addressLine1", "1 Hà Nội", "city", "Hà Nội", "countryCode", "VN"))))
                .andExpect(status().isCreated()));
        long officeId = office.path("id").asLong();
        mvc.perform(withToken(get("/api/admin/offices"), admin)).andExpect(status().isOk());
        mvc.perform(withToken(get("/api/admin/offices/{id}", officeId), admin)).andExpect(status().isOk());
        mvc.perform(withToken(put("/api/admin/offices/{id}", officeId), admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Văn phòng sửa\"," +
                        "\"phone\":\"0901234567\",\"addressLine1\":\"1 Hà Nội\"," +
                        "\"city\":\"Hà Nội\",\"countryCode\":\"VN\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        long adminId = response(mvc.perform(withToken(post("/api/admin/employee-accounts"), admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "currentPassword", "Admin@123", "fullName", "Nhân viên thử",
                        "email", unique("staff") + "@example.com", "password", "Strong@Pass123",
                        "employeeCode", unique("EMP"), "officeId", officeId,
                        "jobTitle", "Quản trị", "hireDate", "2024-01-01"))))
                .andExpect(status().isCreated())).path("userId").asLong();
        mvc.perform(withToken(get("/api/admin/employees"), admin)).andExpect(status().isOk());
        mvc.perform(withToken(get("/api/admin/employees/{id}", adminId), admin)).andExpect(status().isOk());
        mvc.perform(withToken(put("/api/admin/employees/{id}", adminId), admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "officeId", officeId, "jobTitle", "Quản lý", "hireDate", "2024-01-01",
                        "status", "ACTIVE")))).andExpect(status().isOk());

        JsonNode category = response(mvc.perform(withToken(post("/api/admin/categories"), admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "code", unique("CAT"), "name", unique("Danh mục")))))
                .andExpect(status().isCreated()));
        long categoryId = category.path("id").asLong();
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/categories/{id}", categoryId)).andExpect(status().isOk());
        mvc.perform(withToken(put("/api/admin/categories/{id}", categoryId), admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "name", unique("Danh mục đã sửa"), "description", "Mô tả", "status", "ACTIVE"))))
                .andExpect(status().isOk());
        mvc.perform(withToken(post("/api/admin/products/{p}/categories/{c}", productId, categoryId), admin))
                .andExpect(status().isNoContent());
        mvc.perform(withToken(delete("/api/admin/products/{p}/categories/{c}", productId, categoryId), admin))
                .andExpect(status().isNoContent());

        JsonNode supplier = response(mvc.perform(withToken(post("/api/admin/suppliers"), admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "code", unique("SUP"), "name", "Nhà cung cấp"))))
                .andExpect(status().isCreated()));
        long supplierId = supplier.path("id").asLong();
        mvc.perform(withToken(get("/api/suppliers"), admin)).andExpect(status().isOk());
        mvc.perform(withToken(get("/api/suppliers/{id}", supplierId), admin)).andExpect(status().isOk());
        mvc.perform(withToken(put("/api/admin/suppliers/{id}", supplierId), admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nhà cung cấp sửa\"," +
                        "\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        mvc.perform(withToken(put("/api/admin/products/{p}/suppliers/{s}", productId, supplierId), admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"purchasePrice\":50,\"leadTimeDays\":2,\"preferred\":true}"))
                .andExpect(status().isNoContent());
        mvc.perform(withToken(delete("/api/admin/products/{p}/suppliers/{s}", productId, supplierId), admin))
                .andExpect(status().isNoContent());

        JsonNode adjustment = response(mvc.perform(withToken(
                post("/api/admin/products/{id}/inventory/adjustments", productId), admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantityChange\":2,\"note\":\"Kiểm kho\"}"))
                .andExpect(status().isCreated()));
        assertThat(adjustment.path("type").asText()).isEqualTo("ADJUSTMENT_IN");
        assertThat(stock(productId)).isEqualTo(7);
    }

    @Test
    void remainingCartAddressOrderAndPaymentRoutesWork() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        long productId = createProduct(admin, 5, "100.00").path("id").asLong();
        String addressBody = "{\"label\":\"Nhà\",\"recipientName\":\"Khách thử\"," +
                "\"recipientPhone\":\"0901234567\",\"addressLine1\":\"1 Hà Nội\"," +
                "\"city\":\"Hà Nội\",\"countryCode\":\"VN\",\"isDefault\":true}";
        long addressId = response(mvc.perform(withToken(post("/api/users/me/addresses"), customer)
                .contentType(MediaType.APPLICATION_JSON).content(addressBody))
                .andExpect(status().isCreated())).path("id").asLong();
        mvc.perform(withToken(put("/api/users/me/addresses/{id}", addressId), customer)
                .contentType(MediaType.APPLICATION_JSON).content(addressBody.replace("Khách thử", "Khách sửa")))
                .andExpect(status().isOk());

        mvc.perform(withToken(put("/api/cart/items/{id}", productId), customer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
                .andExpect(status().isOk());
        mvc.perform(withToken(delete("/api/cart/items/{id}", productId), customer))
                .andExpect(status().isNoContent());
        mvc.perform(withToken(put("/api/cart/items/{id}", productId), customer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
                .andExpect(status().isOk());
        mvc.perform(withToken(delete("/api/cart"), customer)).andExpect(status().isNoContent());
        assertThat(response(mvc.perform(withToken(get("/api/cart"), customer))
                .andExpect(status().isOk())).size()).isZero();

        long orderId = createOrder(customer, productId, 1).path("id").asLong();
        mvc.perform(withToken(get("/api/orders/me"), customer)).andExpect(status().isOk());
        mvc.perform(withToken(get("/api/admin/orders"), admin)).andExpect(status().isOk());
        mvc.perform(withToken(get("/api/orders/{id}/history", orderId), customer))
                .andExpect(status().isOk());
        mvc.perform(withToken(get("/api/admin/orders/{id}/history", orderId), admin))
                .andExpect(status().isOk());
        mvc.perform(withToken(get("/api/orders/{id}/payments", orderId), customer))
                .andExpect(status().isOk());
        mvc.perform(withToken(get("/api/admin/orders/{id}/payments", orderId), admin))
                .andExpect(status().isOk());
        changeOrderStatus(admin, orderId, "CONFIRMED").andExpect(status().isOk());
        changeOrderStatus(admin, orderId, "SHIPPING").andExpect(status().isOk());
        changeOrderStatus(admin, orderId, "COMPLETED").andExpect(status().isOk());
        changeOrderStatus(admin, orderId, "CANCELLED").andExpect(status().isConflict());
    }

    private String adminToken() throws Exception {
        return response(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.com\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())).path("accessToken").asText();
    }

    private String customerToken() throws Exception {
        String email = unique("customer") + "@example.com";
        String body = json.writeValueAsString(Map.of("fullName", "Khách kiểm thử",
                "email", email, "password", "Test@1234"));
        return response(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated())).path("accessToken").asText();
    }

    private JsonNode createProduct(String admin, int stock, String price) throws Exception {
        return response(mvc.perform(withToken(post("/api/products"), admin)
                .contentType(MediaType.APPLICATION_JSON).content(productBody(stock, price)))
                .andExpect(status().isCreated()));
    }

    private String productBody(int stock, String price) throws Exception {
        return json.writeValueAsString(Map.of("sku", unique("SKU"), "name", "Sản phẩm thử",
                "description", "Test", "price", new BigDecimal(price), "stockQuantity", stock));
    }

    private JsonNode createOrder(String customer, long productId, int quantity) throws Exception {
        return response(mvc.perform(withToken(post("/api/orders"), customer)
                .contentType(MediaType.APPLICATION_JSON).content(orderBody(productId, quantity)))
                .andExpect(status().isCreated()));
    }

    private String orderBody(long productId, int quantity) throws Exception {
        return json.writeValueAsString(Map.of("recipientName", "Nguyễn Văn A",
                "recipientPhone", "0901234567", "shippingAddress", "1 Hà Nội",
                "items", new Object[] {Map.of("productId", productId, "quantity", quantity)}));
    }

    private int stock(long productId) throws Exception {
        return response(mvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())).path("stockQuantity").asInt();
    }

    private ResultActions changeOrderStatus(String admin, long orderId, String next) throws Exception {
        return mvc.perform(withToken(patch("/api/admin/orders/{id}/status", orderId), admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + next + "\"}"));
    }

    private ResultActions changePaymentStatus(String admin, long paymentId, String next) throws Exception {
        return mvc.perform(withToken(patch("/api/admin/payments/{id}/status", paymentId), admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + next + "\"}"));
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private JsonNode response(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
