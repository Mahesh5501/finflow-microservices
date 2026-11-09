
package com.finflow.commerce.repository;

import com.finflow.commerce.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findByOrganisationIdOrderByProductIdAsc(Long organisationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.productId = :productId
              AND p.organisationId = :organisationId
            """)
    Optional<Product> findForUpdateByProductIdAndOrganisationId(
            @Param("productId") Long productId,
            @Param("organisationId") Long organisationId
    );
}
