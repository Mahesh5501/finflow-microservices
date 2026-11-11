package com.finflow.commerce.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * One product line supplied when creating an order.
 * The order request can contain multiple OrderItemRequest objects.
 * A record is suitable because this DTO carries request data and does not need mutable fields.
 */
public record OrderItemRequest(
        @NotNull Long productId, // Identifies the product the customer wants to purchase.
        @Min(1) int quantity // Number of units requested for this product. A quantity below 1 is invalid.
) {
}
