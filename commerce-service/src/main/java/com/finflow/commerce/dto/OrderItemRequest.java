
package com.finflow.commerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * One product line supplied when creating an order.
 *
 * The order request can contain multiple OrderItemRequest objects.
 */
public record OrderItemRequest(

        // Identifies the product the customer wants to purchase.
        @NotNull Long productId,

        // Number of units requested for this product.
        // A quantity below 1 is invalid.
        @Min(1) int quantity

) {
}
