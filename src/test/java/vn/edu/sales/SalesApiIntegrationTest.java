package vn.edu.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

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
        mvc.perform(withToken(delete("/api/products/{id}", id), admin)).andExpect(status().isNoContent());
        mvc.perform(get("/api/products/{id}", id)).andExpect(status().isNotFound());
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
        mvc.perform(withToken(get("/api/orders/{id}", orderId), secondCustomer))
                .andExpect(status().isNotFound());
        mvc.perform(withToken(get("/api/orders/{id}/payments", orderId), secondCustomer))
                .andExpect(status().isNotFound());
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
