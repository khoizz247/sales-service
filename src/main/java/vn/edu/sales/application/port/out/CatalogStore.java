package vn.edu.sales.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CatalogStore {
    record Category(Long id, String code, String name, String description, Long parentId, String status) {}
    record Supplier(Long id, String code, String name, String contactName, String email, String phone, String status) {}
    record Source(Long productId, Long supplierId, String supplierSku, BigDecimal purchasePrice,
                  int leadTimeDays, boolean preferred) {}

    List<Category> categories();
    Optional<Category> category(Long id);
    Category createCategory(String code, String name, String description, Long parentId);
    Category updateCategory(Long id, String name, String description, Long parentId, String status);
    boolean categoryCodeExists(String code);
    boolean categoryNameExists(String name, Long exceptId);
    List<Long> productCategoryIds(Long productId);
    void assignCategory(Long productId, Long categoryId);
    void removeCategory(Long productId, Long categoryId);

    List<Supplier> suppliers();
    Optional<Supplier> supplier(Long id);
    Supplier createSupplier(String code, String name, String contactName, String email, String phone);
    Supplier updateSupplier(Long id, String name, String contactName, String email, String phone, String status);
    boolean supplierCodeExists(String code);
    List<Source> productSources(Long productId);
    void saveSource(Source source);
    void removeSource(Long productId, Long supplierId);
}
