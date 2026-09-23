package vn.edu.sales.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentStore {
    record Payment(Long id, Long orderId, String paymentCode, String providerTransactionId,
                   String method, String status, BigDecimal amount, LocalDateTime paidAt,
                   LocalDateTime createdAt) {}
    List<Payment> byOrder(Long orderId);
    Optional<Payment> findById(Long id);
    Payment create(Long orderId, String code, String method, BigDecimal amount);
    Payment changeStatus(Long id, String status, String providerTransactionId);
    BigDecimal paidTotal(Long orderId);
}
