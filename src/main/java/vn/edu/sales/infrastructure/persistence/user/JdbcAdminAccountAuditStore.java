package vn.edu.sales.infrastructure.persistence.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.sales.application.port.out.AdminAccountAuditStore;

@Repository
public class JdbcAdminAccountAuditStore implements AdminAccountAuditStore {
    private final JdbcTemplate jdbc;
    public JdbcAdminAccountAuditStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void record(Long actorUserId, Long createdUserId) {
        jdbc.update("INSERT INTO admin_account_audit(actor_user_id,created_user_id) VALUES(?,?)",
                actorUserId, createdUserId);
    }
}
