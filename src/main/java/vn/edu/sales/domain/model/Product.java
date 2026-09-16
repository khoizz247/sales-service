package vn.edu.sales.domain.model;

import java.math.BigDecimal;

public record Product(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stock,
        boolean active
) {
    public Product {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Giá sản phẩm không được âm");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Tồn kho không được âm");
        }
    }
}
