
package com.finflow.commerce.dto;

import com.finflow.commerce.entity.ReturnRequest;
import com.finflow.commerce.entity.ReturnStatus;

import java.time.Instant;

/**
 * Response DTO representing a return request.
 *
 * This defines which return-related information is exposed
 * through the Commerce Service API.
 *
 * It does not expose the JPA entity directly.
 */
public record ReturnResponse(

        // Unique ID generated for the return request.
        Long returnId,

        // Order against which the customer requested the return.
        Long orderId,

        // Customer who created the return request.
        Long customerUserId,

        // Reason supplied by the customer.
        String reason,

        // Current return lifecycle status.
        ReturnStatus status,

        // Time at which the return request was created.
        Instant createdAt

) {

    /**
     * Converts a ReturnRequest entity into a ReturnResponse DTO.
     *
     * This is a static factory method.
     *
     * It copies the required values from the entity
     * into the response record.
     *
     * Example:
     * ReturnResponse.from(returnRequest)
     */
    public static ReturnResponse from(ReturnRequest request) {

        return new ReturnResponse(
                request.getReturnId(),
                request.getOrderId(),
                request.getCustomerUserId(),
                request.getReason(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }
}
