package vn.edu.sales.infrastructure.persistence.address;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import vn.edu.sales.application.port.out.AddressStore;

import java.util.List;
import java.util.Optional;
import java.sql.PreparedStatement;

@Repository
public class JdbcAddressStore implements AddressStore {
    private static final String COLUMNS = "id,customer_user_id,label,recipient_name,recipient_phone,address_line1,address_line2,ward,district,city,state_province,postal_code,country_code,is_default";
    private static final RowMapper<Address> MAPPER = (rs, row) -> new Address(rs.getLong("id"), rs.getLong("customer_user_id"),
            rs.getString("label"), rs.getString("recipient_name"), rs.getString("recipient_phone"),
            rs.getString("address_line1"), rs.getString("address_line2"), rs.getString("ward"),
            rs.getString("district"), rs.getString("city"), rs.getString("state_province"),
            rs.getString("postal_code"), rs.getString("country_code"), rs.getBoolean("is_default"));
    private final JdbcTemplate jdbc;
    public JdbcAddressStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public List<Address> findByUserId(Long userId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM customer_addresses WHERE customer_user_id=? ORDER BY is_default DESC,id",
                MAPPER, userId);
    }
    @Override public Optional<Address> findById(Long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM customer_addresses WHERE id=?", MAPPER, id).stream().findFirst();
    }
    @Override public Address create(Address a) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO customer_addresses(customer_user_id,label,recipient_name,recipient_phone,address_line1,address_line2,ward,district,city,state_province,postal_code,country_code,is_default) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    new String[] { "id" });
            statement.setLong(1, a.customerUserId());
            statement.setString(2, a.label());
            statement.setString(3, a.recipientName());
            statement.setString(4, a.recipientPhone());
            statement.setString(5, a.addressLine1());
            statement.setString(6, a.addressLine2());
            statement.setString(7, a.ward());
            statement.setString(8, a.district());
            statement.setString(9, a.city());
            statement.setString(10, a.stateProvince());
            statement.setString(11, a.postalCode());
            statement.setString(12, a.countryCode());
            statement.setBoolean(13, a.isDefault());
            return statement;
        }, keys);
        return findById(keys.getKey().longValue()).orElseThrow();
    }
    @Override public Address update(Address a) {
        jdbc.update("UPDATE customer_addresses SET label=?,recipient_name=?,recipient_phone=?,address_line1=?,address_line2=?,ward=?,district=?,city=?,state_province=?,postal_code=?,country_code=?,is_default=? WHERE id=?",
                a.label(), a.recipientName(), a.recipientPhone(), a.addressLine1(), a.addressLine2(), a.ward(), a.district(),
                a.city(), a.stateProvince(), a.postalCode(), a.countryCode(), a.isDefault(), a.id());
        return findById(a.id()).orElseThrow();
    }
    @Override public void delete(Long id) { jdbc.update("DELETE FROM customer_addresses WHERE id=?", id); }
    @Override public void ensureProfile(Long userId) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM customer_profiles WHERE user_id=?", Integer.class, userId) == 0)
            jdbc.update("INSERT INTO customer_profiles(user_id) VALUES(?)", userId);
    }
    @Override public void clearDefault(Long userId) {
        jdbc.update("UPDATE customer_addresses SET is_default=FALSE WHERE customer_user_id=? AND is_default=TRUE", userId);
    }
}
