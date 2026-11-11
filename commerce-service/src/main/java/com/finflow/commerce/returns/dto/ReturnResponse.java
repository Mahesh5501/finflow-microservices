package com.finflow.commerce.returns.dto;

import com.finflow.commerce.returns.domain.ReturnRequest;
import com.finflow.commerce.returns.domain.ReturnStatus;

import java.time.Instant;

/**
 * Response DTO representing a return request.
 * A record is suitable because this DTO carries response data and does not need mutable fields.    
 * Contains the return request ID, order ID, customer user ID, reason, status and creation time.
 */
public record ReturnResponse(
        Long returnId, // Unique ID generated for the return request.
        Long orderId, // Order against which the customer requested the return.
        Long customerUserId, // Customer who created the return request.
        String reason, // Reason supplied by the customer.
        ReturnStatus status, // Current return lifecycle status.
        Instant createdAt // Time at which the return request was created.
) {

    /**
     * Converts a ReturnRequest entity into a ReturnResponse DTO.
     * Keeping this mapping method here avoids repeating the same entity-to-response conversion in service methods.
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
