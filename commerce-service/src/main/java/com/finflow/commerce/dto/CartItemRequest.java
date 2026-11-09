
package com.finflow.commerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request received when a customer adds a product to their cart.
 *
 * A record is suitable because this DTO carries request data
 * and does not need mutable fields.
 */
public record CartItemRequest(

        // Organisation in which the cart and product belong.
        // Required because this is a multi-tenant application.
        @NotNull Long organisationId,

        // Product that the customer wants to add.
        @NotNull Long productId,

        // Number of units requested.
        // Must be at least 1 when adding a new cart item.
        @NotNull @Min(1) Integer quantity

) {
}
