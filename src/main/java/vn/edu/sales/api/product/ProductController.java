package vn.edu.sales.api.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.sales.application.service.ProductService;
import vn.edu.sales.domain.model.Product;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> getAll() {
        return productService.getAllActive().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return ProductResponse.from(productService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return ProductResponse.from(productService.create(
                request.name(), request.description(), request.price(), request.stock()
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

    public record CreateProductRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 1000) String description,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @Min(0) int stock
    ) {
    }

    public record ProductResponse(
            Long id,
            String name,
            String description,
            BigDecimal price,
            int stock,
            boolean active
    ) {
        static ProductResponse from(Product product) {
            return new ProductResponse(
                    product.id(), product.name(), product.description(), product.price(), product.stock(), product.active()
            );
        }
    }
}
