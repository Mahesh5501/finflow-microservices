
package com.finflow.commerce.repository.catalogue;

import com.finflow.commerce.entity.catalogue.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Product database operations.
 * JpaRepository provides common CRUD and pagination-related database operations without requiring manual implementation.
 */
public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * Finds all products belonging to an organisation.
     * Results are ordered by product ID in ascending order.
     * @param organisationId the ID of the organisation.
     * @return the products belonging to the organisation.
     */
    List<Product> findByOrganisationIdOrderByProductIdAsc(Long organisationId);

    /**
     * Finds a product for update by its product ID and organisation ID.
     * @param productId the ID of the product.
     * @param organisationId the ID of the organisation.
     * @return the product with the given product ID and organisation ID.
     */
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
