package vn.edu.sales.infrastructure.persistence.payment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import vn.edu.sales.application.port.out.PaymentStore;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcPaymentStore implements PaymentStore {
    private static final String COLUMNS = "id,order_id,payment_code,provider_transaction_id,method,status,amount,paid_at,created_at";
    private static final RowMapper<Payment> MAPPER = (rs, row) -> new Payment(rs.getLong("id"),
            rs.getLong("order_id"), rs.getString("payment_code"), rs.getString("provider_transaction_id"),
            rs.getString("method"), rs.getString("status"), rs.getBigDecimal("amount"),
            rs.getTimestamp("paid_at") == null ? null : rs.getTimestamp("paid_at").toLocalDateTime(),
            rs.getTimestamp("created_at").toLocalDateTime());
    private final JdbcTemplate jdbc;
    public JdbcPaymentStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public List<Payment> byOrder(Long orderId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM payments WHERE order_id=? ORDER BY id", MAPPER, orderId);
    }
    @Override public Optional<Payment> findById(Long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM payments WHERE id=?", MAPPER, id).stream().findFirst();
    }
    @Override public Payment create(Long orderId, String code, String method, BigDecimal amount) {
        jdbc.update("INSERT INTO payments(order_id,payment_code,method,amount) VALUES(?,?,?,?)", orderId, code, method, amount);
        return jdbc.query("SELECT " + COLUMNS + " FROM payments WHERE payment_code=?", MAPPER, code).getFirst();
    }
    @Override public Payment changeStatus(Long id, String status, String providerTransactionId) {
        jdbc.update("UPDATE payments SET status=?,provider_transaction_id=COALESCE(?,provider_transaction_id)," +
                        "paid_at=CASE WHEN ?='PAID' THEN CURRENT_TIMESTAMP(6) ELSE paid_at END WHERE id=?",
                status, providerTransactionId, status, id);
        return findById(id).orElseThrow();
    }
    @Override public BigDecimal paidTotal(Long orderId) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM payments WHERE order_id=? AND status='PAID'",
                BigDecimal.class, orderId);
    }
}
