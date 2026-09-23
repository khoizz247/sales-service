package vn.edu.sales.infrastructure.persistence.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import vn.edu.sales.application.port.out.CustomerProfileStore;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcCustomerProfileStore implements CustomerProfileStore {
    private static final String SELECT = "SELECT cp.user_id,u.email,u.full_name,cp.phone,cp.date_of_birth,cp.credit_limit " +
            "FROM customer_profiles cp JOIN users u ON u.id=cp.user_id";
    private static final RowMapper<Profile> MAPPER = (rs, row) -> new Profile(
            rs.getLong("user_id"), rs.getString("email"), rs.getString("full_name"), rs.getString("phone"),
            rs.getDate("date_of_birth") == null ? null : rs.getDate("date_of_birth").toLocalDate(),
            rs.getBigDecimal("credit_limit"));
    private final JdbcTemplate jdbc;
    public JdbcCustomerProfileStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public List<Profile> all() {
        return jdbc.query(SELECT + " WHERE u.role='CUSTOMER' ORDER BY cp.user_id", MAPPER);
    }
    @Override public Optional<Profile> findByUserId(Long userId) {
        return jdbc.query(SELECT + " WHERE cp.user_id=? AND u.role='CUSTOMER'", MAPPER, userId)
                .stream().findFirst();
    }
    @Override public void ensureProfile(Long userId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM customer_profiles WHERE user_id=?", Integer.class, userId);
        if (count == null || count == 0) jdbc.update("INSERT INTO customer_profiles(user_id) VALUES(?)", userId);
    }
    @Override public void updatePersonal(Long userId, String phone, java.time.LocalDate dateOfBirth) {
        jdbc.update("UPDATE customer_profiles SET phone=?,date_of_birth=? WHERE user_id=?", phone, dateOfBirth, userId);
    }
    @Override public void updateCreditLimit(Long userId, BigDecimal creditLimit) {
        jdbc.update("UPDATE customer_profiles SET credit_limit=? WHERE user_id=?", creditLimit, userId);
    }
}
