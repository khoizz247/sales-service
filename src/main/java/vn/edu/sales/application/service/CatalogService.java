package vn.edu.sales.application.service;

import vn.edu.sales.application.port.out.CatalogStore;
import vn.edu.sales.application.port.out.ProductRepository;
import vn.edu.sales.domain.exception.BusinessConflictException;
import vn.edu.sales.domain.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public class CatalogService {
    private final CatalogStore store;
    private final ProductRepository products;

    public CatalogService(CatalogStore store, ProductRepository products) {
        this.store = store;
        this.products = products;
    }

    public List<CatalogStore.Category> categories() { return store.categories(); }
    public CatalogStore.Category category(Long id) { return store.category(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục: " + id)); }
    public CatalogStore.Category createCategory(String code, String name, String description, Long parentId) {
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        if (store.categoryCodeExists(normalizedCode) || store.categoryNameExists(name.trim(), null))
            throw new BusinessConflictException("Mã hoặc tên danh mục đã tồn tại");
        if (parentId != null) category(parentId);
        return store.createCategory(normalizedCode, name.trim(), description == null ? "" : description.trim(), parentId);
    }
    public CatalogStore.Category updateCategory(Long id, String name, String description, Long parentId, String status) {
        category(id);
        if (parentId != null) {
            if (parentId.equals(id)) throw new BusinessConflictException("Danh mục không thể là cha của chính nó");
            Long cursor = parentId;
            while (cursor != null) {
                if (cursor.equals(id)) throw new BusinessConflictException("Cấu trúc danh mục tạo vòng lặp");
                cursor = category(cursor).parentId();
            }
        }
        if (store.categoryNameExists(name.trim(), id)) throw new BusinessConflictException("Tên danh mục đã tồn tại");
        return store.updateCategory(id, name.trim(), description == null ? "" : description.trim(), parentId, status);
    }
    public List<Long> productCategoryIds(Long productId) { requireProduct(productId); return store.productCategoryIds(productId); }
    public void assignCategory(Long productId, Long categoryId) {
        requireProduct(productId);
        if (!"ACTIVE".equals(category(categoryId).status()))
            throw new BusinessConflictException("Danh mục không còn hoạt động");
        store.assignCategory(productId, categoryId);
    }
    public void removeCategory(Long productId, Long categoryId) { requireProduct(productId); store.removeCategory(productId, categoryId); }

    public List<CatalogStore.Supplier> suppliers() { return store.suppliers(); }
    public CatalogStore.Supplier supplier(Long id) { return store.supplier(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp: " + id)); }
    public CatalogStore.Supplier createSupplier(String code, String name, String contactName, String email, String phone) {
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        if (store.supplierCodeExists(normalizedCode)) throw new BusinessConflictException("Mã nhà cung cấp đã tồn tại");
        return store.createSupplier(normalizedCode, name.trim(), contactName, email, phone);
    }
    public CatalogStore.Supplier updateSupplier(Long id, String name, String contactName, String email, String phone, String status) {
        supplier(id);
        return store.updateSupplier(id, name.trim(), contactName, email, phone, status);
    }
    public List<CatalogStore.Source> productSources(Long productId) { requireProduct(productId); return store.productSources(productId); }
    public void saveSource(Long productId, Long supplierId, String supplierSku, BigDecimal purchasePrice,
                           int leadTimeDays, boolean preferred) {
        requireProduct(productId);
        if (!"ACTIVE".equals(supplier(supplierId).status()))
            throw new BusinessConflictException("Nhà cung cấp không còn hoạt động");
        if (purchasePrice == null || purchasePrice.signum() <= 0 || leadTimeDays < 0)
            throw new IllegalArgumentException("Giá nhập hoặc thời gian giao không hợp lệ");
        store.saveSource(new CatalogStore.Source(productId, supplierId, supplierSku, purchasePrice, leadTimeDays, preferred));
    }
    public void removeSource(Long productId, Long supplierId) { requireProduct(productId); store.removeSource(productId, supplierId); }
    private void requireProduct(Long id) { products.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id)); }
}
