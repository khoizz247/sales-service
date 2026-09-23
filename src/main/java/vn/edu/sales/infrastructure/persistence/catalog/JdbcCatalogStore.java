package vn.edu.sales.infrastructure.persistence.catalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import vn.edu.sales.application.port.out.CatalogStore;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcCatalogStore implements CatalogStore {
    private final JdbcTemplate jdbc;
    public JdbcCatalogStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<Category> CATEGORY = (rs, row) -> new Category(
            rs.getLong("id"), rs.getString("category_code"), rs.getString("name"),
            rs.getString("description"), nullableLong(rs, "parent_id"), rs.getString("status"));
    private static final RowMapper<Supplier> SUPPLIER = (rs, row) -> new Supplier(
            rs.getLong("id"), rs.getString("supplier_code"), rs.getString("name"),
            rs.getString("contact_name"), rs.getString("email"), rs.getString("phone"), rs.getString("status"));
    private static final RowMapper<Source> SOURCE = (rs, row) -> new Source(
            rs.getLong("product_id"), rs.getLong("supplier_id"), rs.getString("supplier_sku"),
            rs.getBigDecimal("purchase_price"), rs.getInt("lead_time_days"), rs.getBoolean("is_preferred"));

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
    @Override public List<Category> categories() {
        return jdbc.query("SELECT id,category_code,name,description,parent_id,status FROM product_categories ORDER BY id", CATEGORY);
    }
    @Override public Optional<Category> category(Long id) {
        return jdbc.query("SELECT id,category_code,name,description,parent_id,status FROM product_categories WHERE id=?", CATEGORY, id)
                .stream().findFirst();
    }
    @Override public Category createCategory(String code, String name, String description, Long parentId) {
        jdbc.update("INSERT INTO product_categories(category_code,name,description,parent_id) VALUES(?,?,?,?)",
                code, name, description, parentId);
        return jdbc.query("SELECT id,category_code,name,description,parent_id,status FROM product_categories WHERE category_code=?",
                CATEGORY, code).getFirst();
    }
    @Override public Category updateCategory(Long id, String name, String description, Long parentId, String status) {
        jdbc.update("UPDATE product_categories SET name=?,description=?,parent_id=?,status=? WHERE id=?",
                name, description, parentId, status, id);
        return category(id).orElseThrow();
    }
    @Override public boolean categoryCodeExists(String code) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM product_categories WHERE category_code=?", Integer.class, code) > 0;
    }
    @Override public boolean categoryNameExists(String name, Long exceptId) {
        if (exceptId == null)
            return jdbc.queryForObject("SELECT COUNT(*) FROM product_categories WHERE name=?",
                    Integer.class, name) > 0;
        return jdbc.queryForObject("SELECT COUNT(*) FROM product_categories WHERE name=? AND id<>?",
                Integer.class, name, exceptId) > 0;
    }
    @Override public List<Long> productCategoryIds(Long productId) {
        return jdbc.queryForList("SELECT category_id FROM product_category_assignments WHERE product_id=? ORDER BY category_id",
                Long.class, productId);
    }
    @Override public void assignCategory(Long productId, Long categoryId) {
        jdbc.update("INSERT INTO product_category_assignments(product_id,category_id) VALUES(?,?)", productId, categoryId);
    }
    @Override public void removeCategory(Long productId, Long categoryId) {
        jdbc.update("DELETE FROM product_category_assignments WHERE product_id=? AND category_id=?", productId, categoryId);
    }
    @Override public List<Supplier> suppliers() {
        return jdbc.query("SELECT id,supplier_code,name,contact_name,email,phone,status FROM suppliers ORDER BY id", SUPPLIER);
    }
    @Override public Optional<Supplier> supplier(Long id) {
        return jdbc.query("SELECT id,supplier_code,name,contact_name,email,phone,status FROM suppliers WHERE id=?", SUPPLIER, id)
                .stream().findFirst();
    }
    @Override public Supplier createSupplier(String code, String name, String contactName, String email, String phone) {
        jdbc.update("INSERT INTO suppliers(supplier_code,name,contact_name,email,phone) VALUES(?,?,?,?,?)",
                code, name, contactName, email, phone);
        return jdbc.query("SELECT id,supplier_code,name,contact_name,email,phone,status FROM suppliers WHERE supplier_code=?",
                SUPPLIER, code).getFirst();
    }
    @Override public Supplier updateSupplier(Long id, String name, String contactName, String email, String phone, String status) {
        jdbc.update("UPDATE suppliers SET name=?,contact_name=?,email=?,phone=?,status=? WHERE id=?",
                name, contactName, email, phone, status, id);
        return supplier(id).orElseThrow();
    }
    @Override public boolean supplierCodeExists(String code) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM suppliers WHERE supplier_code=?", Integer.class, code) > 0;
    }
    @Override public List<Source> productSources(Long productId) {
        return jdbc.query("SELECT product_id,supplier_id,supplier_sku,purchase_price,lead_time_days,is_preferred " +
                "FROM product_suppliers WHERE product_id=? ORDER BY supplier_id", SOURCE, productId);
    }
    @Override public void saveSource(Source source) {
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM product_suppliers WHERE product_id=? AND supplier_id=?",
                Integer.class, source.productId(), source.supplierId());
        if (existing != null && existing > 0) {
            jdbc.update("UPDATE product_suppliers SET supplier_sku=?,purchase_price=?,lead_time_days=?,is_preferred=? " +
                            "WHERE product_id=? AND supplier_id=?", source.supplierSku(), source.purchasePrice(),
                    source.leadTimeDays(), source.preferred(), source.productId(), source.supplierId());
        } else {
            jdbc.update("INSERT INTO product_suppliers(product_id,supplier_id,supplier_sku,purchase_price,lead_time_days,is_preferred) " +
                            "VALUES(?,?,?,?,?,?)", source.productId(), source.supplierId(), source.supplierSku(),
                    source.purchasePrice(), source.leadTimeDays(), source.preferred());
        }
    }
    @Override public void removeSource(Long productId, Long supplierId) {
        jdbc.update("DELETE FROM product_suppliers WHERE product_id=? AND supplier_id=?", productId, supplierId);
    }
}
