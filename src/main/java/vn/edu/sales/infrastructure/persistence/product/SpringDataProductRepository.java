package vn.edu.sales.infrastructure.persistence.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, Long> {
    List<ProductJpaEntity> findAllByActiveTrueOrderByIdDesc();
}
