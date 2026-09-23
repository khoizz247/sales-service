package vn.edu.sales.application.service;

import vn.edu.sales.application.port.out.OrderRepository;
import vn.edu.sales.application.port.out.PaymentStore;
import vn.edu.sales.application.port.out.TransactionRunner;
import vn.edu.sales.application.port.out.UserRepository;
import vn.edu.sales.domain.exception.BusinessConflictException;
import vn.edu.sales.domain.exception.ResourceNotFoundException;
import vn.edu.sales.domain.model.Order;
import vn.edu.sales.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PaymentService {
    private final PaymentStore payments;
    private final OrderRepository orders;
    private final UserRepository users;
    private final TransactionRunner transactions;
    public PaymentService(PaymentStore payments, OrderRepository orders, UserRepository users,
                          TransactionRunner transactions) {
        this.payments = payments; this.orders = orders; this.users = users; this.transactions = transactions;
    }
    public List<PaymentStore.Payment> mine(String email, Long orderId) {
        Order order = order(orderId);
        Long userId = users.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng")).id();
        if (!order.userId().equals(userId)) throw new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId);
        return payments.byOrder(orderId);
    }
    public List<PaymentStore.Payment> byOrder(Long orderId) { order(orderId); return payments.byOrder(orderId); }
    public PaymentStore.Payment create(Long orderId, String method, BigDecimal amount) {
        return transactions.execute(() -> {
            Order order = lockedOrder(orderId);
            if (order.status() == OrderStatus.CANCELLED) throw new BusinessConflictException("Đơn đã hủy");
            if (amount == null || amount.signum() <= 0 || amount.compareTo(order.totalAmount()) > 0)
                throw new IllegalArgumentException("Số tiền không hợp lệ");
            String code = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
            return payments.create(orderId, code, method, amount);
        });
    }
    public PaymentStore.Payment changeStatus(Long paymentId, String next, String providerTransactionId) {
        return transactions.execute(() -> {
            PaymentStore.Payment initial = payments.findById(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán: " + paymentId));
            Order order = lockedOrder(initial.orderId());
            PaymentStore.Payment payment = payments.findById(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán: " + paymentId));
            if (payment.status().equals(next)) return payment;
            boolean allowed = switch (payment.status()) {
                case "PENDING" -> next.equals("PAID") || next.equals("FAILED") || next.equals("CANCELLED");
                case "PAID" -> next.equals("REFUNDED");
                default -> false;
            };
            if (!allowed) throw new BusinessConflictException("Không thể chuyển trạng thái thanh toán");
            if (next.equals("PAID")) {
                if (order.status() == OrderStatus.CANCELLED) throw new BusinessConflictException("Đơn đã hủy");
                if (payments.paidTotal(order.id()).add(payment.amount()).compareTo(order.totalAmount()) > 0)
                    throw new BusinessConflictException("Tổng thanh toán vượt tổng đơn hàng");
            }
            return payments.changeStatus(paymentId, next, providerTransactionId);
        });
    }
    private Order order(Long id) { return orders.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + id)); }
    private Order lockedOrder(Long id) { return orders.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + id)); }
}
