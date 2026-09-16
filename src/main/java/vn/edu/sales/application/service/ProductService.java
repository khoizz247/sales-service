package vn.edu.sales.application.service;

import vn.edu.sales.application.port.out.ProductRepository;
import vn.edu.sales.domain.exception.ResourceNotFoundException;
import vn.edu.sales.domain.exception.BusinessConflictException;
import vn.edu.sales.domain.model.Product;
import vn.edu.sales.domain.model.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllActive() {
        return productRepository.findAllActive();
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
                .filter(product -> product.status() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));
    }

    public Product create(String sku, String name, String description, BigDecimal price, int stockQuantity) {
        String normalizedSku = normalizeSku(sku);
        if (productRepository.existsBySku(normalizedSku)) {
            throw new BusinessConflictException("SKU đã tồn tại: " + normalizedSku);
        }
        Product product = new Product(null, normalizedSku, name.trim(), normalizeDescription(description),
                price, stockQuantity, ProductStatus.ACTIVE, null);
        return productRepository.save(product);
    }

    public Product update(Long id, String name, String description, BigDecimal price, int stockQuantity) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));
        return productRepository.save(new Product(existing.id(), existing.sku(), name.trim(),
                normalizeDescription(description), price, stockQuantity, existing.status(), existing.version()));
    }

    public Product updateStock(Long id, int stockQuantity) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));
        return productRepository.save(existing.withStock(stockQuantity));
    }

    public void delete(Long id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));
        productRepository.save(existing.inactive());
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
