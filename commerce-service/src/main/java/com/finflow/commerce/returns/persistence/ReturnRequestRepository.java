package com.finflow.commerce.returns.persistence;

import com.finflow.commerce.returns.domain.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for ReturnRequest entities.
 * JpaRepository provides common CRUD and pagination-related database operations without requiring manual implementation.
 *
 * Persistence layer interface called by application services. Spring Data discovers it below the application root and supplies a JPA proxy; 
 * @Repository is not required on this interface (it is useful on a hand-written DAO for exception translation).
 * Removing JpaRepository inheritance would remove that generated CRUD contract.
 * Derived queries use Java property names, not SQL columns; custom JPQL must do the same. 
 * Repositories do not authenticate callers: application services must supply owner/seller constraints and define transactions spanning multiple repositories.
 * Native SQL is an alternative for database-specific operations, at a portability cost.
 */
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    // Retrieves all return requests associated with an order. Results are sorted by creation time, newest first. List is used because an order can have multiple return requests over its lifecycle.
    List<ReturnRequest> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
