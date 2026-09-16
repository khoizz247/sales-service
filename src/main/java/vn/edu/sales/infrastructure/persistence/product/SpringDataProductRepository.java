package vn.edu.sales.infrastructure.persistence.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import vn.edu.sales.domain.model.ProductStatus;

import java.util.List;

public interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, Long> {
    List<ProductJpaEntity> findAllByStatusOrderByIdDesc(ProductStatus status);
    boolean existsBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductJpaEntity p where p.id in :ids order by p.id")
    List<ProductJpaEntity> findAllByIdsForUpdate(@Param("ids") List<Long> ids);
}
