package vn.edu.sales.infrastructure.persistence.product;

import org.springframework.stereotype.Repository;
import vn.edu.sales.application.port.out.ProductRepository;
import vn.edu.sales.domain.model.Product;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepositoryAdapter implements ProductRepository {
    private final SpringDataProductRepository repository;

    public ProductRepositoryAdapter(SpringDataProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = new ProductJpaEntity(
                product.id(), product.name(), product.description(), product.price(), product.stock(), product.active()
        );
        return toDomain(repository.save(entity));
    }

    @Override
    public List<Product> findAllActive() {
        return repository.findAllByActiveTrueOrderByIdDesc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Product> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private Product toDomain(ProductJpaEntity entity) {
        return new Product(
                entity.getId(), entity.getName(), entity.getDescription(), entity.getPrice(), entity.getStock(), entity.isActive()
        );
    }
}
