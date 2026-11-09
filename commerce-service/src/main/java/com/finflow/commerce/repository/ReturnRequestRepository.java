
package com.finflow.commerce.repository;

import com.finflow.commerce.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for ReturnRequest entities.
 *
 * It provides standard CRUD operations through JpaRepository
 * and one custom derived query for retrieving an order's returns.
 */
public interface ReturnRequestRepository
        extends JpaRepository<ReturnRequest, Long> {

    /**
     * Retrieves all return requests associated with an order.
     *
     * Results are sorted by creation time, newest first.
     *
     * Example:
     * Order ID = 1001
     *
     * Possible result:
     * Return request 3 - created today
     * Return request 2 - created yesterday
     * Return request 1 - created last week
     *
     * List is used because an order can have multiple return
     * requests over its lifecycle.
     */
    List<ReturnRequest> findByOrderIdOrderByCreatedAtDesc(
            Long orderId
    );
}
