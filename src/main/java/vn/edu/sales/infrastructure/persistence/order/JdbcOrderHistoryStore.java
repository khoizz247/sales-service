package vn.edu.sales.infrastructure.persistence.order;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.sales.application.port.out.OrderHistoryStore;

import java.util.List;

@Repository
public class JdbcOrderHistoryStore implements OrderHistoryStore {
    private final JdbcTemplate jdbc;
    public JdbcOrderHistoryStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<Change> byOrder(Long orderId) {
        return jdbc.query("SELECT id,order_id,from_status,to_status,changed_by_user_id,note,changed_at " +
                "FROM order_status_history WHERE order_id=? ORDER BY changed_at,id", (rs, row) -> {
            return new Change(rs.getLong("id"), rs.getLong("order_id"), rs.getString("from_status"),
                    rs.getString("to_status"), rs.getObject("changed_by_user_id", Long.class), rs.getString("note"),
                    rs.getTimestamp("changed_at").toLocalDateTime());
        }, orderId);
    }
}
