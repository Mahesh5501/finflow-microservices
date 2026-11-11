package com.finflow.commerce.catalogue.persistence;

import com.finflow.commerce.catalogue.domain.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * PRODUCT REPOSITORY - Catalogue's database access boundary.
 *
 * Spring Data JPA creates the implementation at runtime from this interface.
 * JpaRepository supplies CRUD, paging and sorting operations; the JpaSpecificationExecutor extension supports dynamic catalogue filters.
 * We declare only queries that are specific to Commerce use cases.
 *
 * Persistence layer interface called by application services. Spring Data discovers it below the application root and supplies a JPA proxy;
 * @Repository is not required on this interface (it is useful on a hand-written DAO for exception translation).
 * Removing JpaRepository inheritance would remove that generated CRUD contract.
 * Derived queries use Java property names, not SQL columns; custom JPQL must do the same.
 * Repositories do not authenticate callers: application services must supply owner/seller constraints and define transactions spanning multiple repositories.
 * Native SQL is an alternative for database-specific operations, at a portability cost.
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

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
    // PESSIMISTIC_WRITE asks the database to lock the selected row for the current transaction.
    // It is useful when a caller must read and then modify stock without another transaction changing it in between.
    // This is different from Product.@Version (optimistic locking): the pessimistic lock blocks competing writers; @Version detects a conflict.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // JPQL uses the Java entity name (Product) and Java property names, not the physical SQL table/column names. @Param binds method arguments to the named parameters in this query.
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
