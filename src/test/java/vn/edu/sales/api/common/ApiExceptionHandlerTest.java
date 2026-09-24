package vn.edu.sales.api.common;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/products");

    @Test
    void constraintFailureReturnsConflictWithoutInternalSql() {
        var response = handler.handleDataConflict(new DataIntegrityViolationException(
                "Duplicate entry for secret_table.uk_private"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("DATA_CONFLICT");
        assertThat(response.getBody().message()).doesNotContain("secret_table", "uk_private");
        assertThat(response.getBody().path()).isEqualTo("/api/products");
    }

    @Test
    void unavailableDatabaseReturnsSafeServerError() {
        var response = handler.handleDatabaseFailure(new DataAccessResourceFailureException(
                "jdbc:mysql://private-host:3306/sales_service"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().code()).isEqualTo("DATABASE_ERROR");
        assertThat(response.getBody().message()).doesNotContain("private-host", "jdbc:mysql");
    }
}
