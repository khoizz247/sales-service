package vn.edu.sales.api.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.sales.application.port.out.CatalogStore;
import vn.edu.sales.application.service.CatalogService;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class CatalogController {
    private final CatalogService service;
    public CatalogController(CatalogService service) { this.service = service; }

    @GetMapping("/api/categories")
    public List<CatalogStore.Category> categories() { return service.categories(); }
    @GetMapping("/api/categories/{id}")
    public CatalogStore.Category category(@PathVariable Long id) { return service.category(id); }
    @PostMapping("/api/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogStore.Category createCategory(@Valid @RequestBody CategoryCreate request) {
        return service.createCategory(request.code(), request.name(), request.description(), request.parentId());
    }
    @PutMapping("/api/admin/categories/{id}")
    public CatalogStore.Category updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryUpdate request) {
        return service.updateCategory(id, request.name(), request.description(), request.parentId(), request.status());
    }
    @GetMapping("/api/products/{productId}/categories")
    public List<Long> productCategories(@PathVariable Long productId) { return service.productCategoryIds(productId); }
    @PostMapping("/api/admin/products/{productId}/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignCategory(@PathVariable Long productId, @PathVariable Long categoryId) {
        service.assignCategory(productId, categoryId);
    }
    @DeleteMapping("/api/admin/products/{productId}/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCategory(@PathVariable Long productId, @PathVariable Long categoryId) {
        service.removeCategory(productId, categoryId);
    }

    @GetMapping("/api/suppliers")
    public List<CatalogStore.Supplier> suppliers() { return service.suppliers(); }
    @GetMapping("/api/suppliers/{id}")
    public CatalogStore.Supplier supplier(@PathVariable Long id) { return service.supplier(id); }
    @PostMapping("/api/admin/suppliers")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogStore.Supplier createSupplier(@Valid @RequestBody SupplierCreate request) {
        return service.createSupplier(request.code(), request.name(), request.contactName(), request.email(), request.phone());
    }
    @PutMapping("/api/admin/suppliers/{id}")
    public CatalogStore.Supplier updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierUpdate request) {
        return service.updateSupplier(id, request.name(), request.contactName(), request.email(), request.phone(), request.status());
    }
    @GetMapping("/api/admin/products/{productId}/suppliers")
    public List<CatalogStore.Source> productSuppliers(@PathVariable Long productId) { return service.productSources(productId); }
    @PutMapping("/api/admin/products/{productId}/suppliers/{supplierId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveSource(@PathVariable Long productId, @PathVariable Long supplierId,
                           @Valid @RequestBody SourceRequest request) {
        service.saveSource(productId, supplierId, request.supplierSku(), request.purchasePrice(),
                request.leadTimeDays(), request.preferred());
    }
    @DeleteMapping("/api/admin/products/{productId}/suppliers/{supplierId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSource(@PathVariable Long productId, @PathVariable Long supplierId) {
        service.removeSource(productId, supplierId);
    }

    public record CategoryCreate(@NotBlank @Size(max=40) String code, @NotBlank @Size(max=120) String name,
                                 @Size(max=1000) String description, Long parentId) {}
    public record CategoryUpdate(@NotBlank @Size(max=120) String name, @Size(max=1000) String description,
                                 Long parentId, @NotNull @Pattern(regexp="ACTIVE|INACTIVE") String status) {}
    public record SupplierCreate(@NotBlank @Size(max=40) String code, @NotBlank @Size(max=160) String name,
                                 @Size(max=120) String contactName, @Email String email, @Size(max=30) String phone) {}
    public record SupplierUpdate(@NotBlank @Size(max=160) String name, @Size(max=120) String contactName,
                                 @Email String email, @Size(max=30) String phone,
                                 @NotNull @Pattern(regexp="ACTIVE|INACTIVE") String status) {}
    public record SourceRequest(@Size(max=64) String supplierSku, @NotNull @DecimalMin("0.01") BigDecimal purchasePrice,
                                @Min(0) int leadTimeDays, boolean preferred) {}
}
