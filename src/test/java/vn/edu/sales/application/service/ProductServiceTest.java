package vn.edu.sales.application.service;

import org.junit.jupiter.api.Test;
import vn.edu.sales.application.port.out.ProductRepository;
import vn.edu.sales.domain.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProductServiceTest {

    @Test
    void createsAValidProductWithoutDependingOnSpringOrJpa() {
        InMemoryProductRepository repository = new InMemoryProductRepository();
        ProductService service = new ProductService(repository);

        Product product = service.create("  Sản phẩm A  ", "Mô tả", new BigDecimal("120000"), 5);

        assertThat(product.id()).isEqualTo(1L);
        assertThat(product.name()).isEqualTo("Sản phẩm A");
        assertThat(product.active()).isTrue();
    }

    private static class InMemoryProductRepository implements ProductRepository {
        private final List<Product> products = new ArrayList<>();

        @Override
        public Product save(Product product) {
            Product saved = new Product(
                    (long) products.size() + 1,
                    product.name(),
                    product.description(),
                    product.price(),
                    product.stock(),
                    product.active()
            );
            products.add(saved);
            return saved;
        }

        @Override
        public List<Product> findAllActive() {
            return products.stream().filter(Product::active).toList();
        }

        @Override
        public Optional<Product> findById(Long id) {
            return products.stream().filter(product -> product.id().equals(id)).findFirst();
        }

        @Override
        public void deleteById(Long id) {
            products.removeIf(product -> product.id().equals(id));
        }
    }
}
