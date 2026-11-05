package com.finflow.commerce.dto;

import com.finflow.commerce.entity.ReturnRequest;
import com.finflow.commerce.entity.ReturnStatus;

import java.time.Instant;

public record ReturnResponse(
        Long returnId,
        Long orderId,
        Long customerUserId,
        String reason,
        ReturnStatus status,
        Instant createdAt
) {
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
