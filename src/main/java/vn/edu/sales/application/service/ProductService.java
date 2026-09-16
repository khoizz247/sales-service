package vn.edu.sales.application.service;

import vn.edu.sales.application.port.out.ProductRepository;
import vn.edu.sales.domain.exception.ResourceNotFoundException;
import vn.edu.sales.domain.model.Product;

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
                .filter(Product::active)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));
    }

    public Product create(String name, String description, BigDecimal price, int stock) {
        Product product = new Product(null, name.trim(), normalizeDescription(description), price, stock, true);
        return productRepository.save(product);
    }

    public void delete(Long id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));
        productRepository.deleteById(existing.id());
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }
}
