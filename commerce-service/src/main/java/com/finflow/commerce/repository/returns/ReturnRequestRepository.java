
package com.finflow.commerce.repository.returns;

import com.finflow.commerce.entity.returns.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for ReturnRequest entities.
 * JpaRepository provides common CRUD and pagination-related database operations without requiring manual implementation.
 */
public interface ReturnRequestRepository
        extends JpaRepository<ReturnRequest, Long> {

    /**
     * Retrieves all return requests associated with an order.
     * Results are sorted by creation time, newest first. List is used because an order can have multiple return requests over its lifecycle.
     * @param orderId the ID of the order.
     * @return the return requests associated with the order.
     */
    List<ReturnRequest> findByOrderIdOrderByCreatedAtDesc(
            Long orderId
    );
}
