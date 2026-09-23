package vn.edu.sales.api.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.sales.application.port.out.PaymentStore;
import vn.edu.sales.application.service.PaymentService;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) { this.service = service; }

    @GetMapping("/api/orders/{orderId}/payments")
    public List<PaymentStore.Payment> mine(Authentication authentication, @PathVariable Long orderId) {
        return service.mine(authentication.getName(), orderId);
    }

    @GetMapping("/api/admin/orders/{orderId}/payments")
    public List<PaymentStore.Payment> byOrder(@PathVariable Long orderId) {
        return service.byOrder(orderId);
    }

    @PostMapping("/api/admin/orders/{orderId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentStore.Payment create(@PathVariable Long orderId, @Valid @RequestBody CreatePaymentRequest request) {
        return service.create(orderId, request.method(), request.amount());
    }

    @PatchMapping("/api/admin/payments/{paymentId}/status")
    public PaymentStore.Payment changeStatus(@PathVariable Long paymentId,
                                              @Valid @RequestBody ChangePaymentStatusRequest request) {
        return service.changeStatus(paymentId, request.status(), request.providerTransactionId());
    }

    public record CreatePaymentRequest(
            @NotNull @Pattern(regexp = "COD|BANK_TRANSFER|CARD|E_WALLET") String method,
            @NotNull @DecimalMin("0.01") BigDecimal amount) {}

    public record ChangePaymentStatusRequest(
            @NotNull @Pattern(regexp = "PAID|FAILED|CANCELLED|REFUNDED") String status,
            @Size(max = 100) String providerTransactionId) {}
}
