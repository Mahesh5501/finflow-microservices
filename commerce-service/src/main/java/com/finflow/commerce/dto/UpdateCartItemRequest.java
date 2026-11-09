
package com.finflow.commerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request used to update the quantity of a product in the cart.
 * A record is suitable because this DTO carries request data and does not need mutable fields.
 * A quantity of zero is allowed and is handled by CartService as a request to remove the item.
 */
public record UpdateCartItemRequest(
        @NotNull Long organisationId, // Organisation to which the cart belongs.
        @NotNull @Min(0) Integer quantity // New quantity for the cart item. Zero is allowed; negative quantities are rejected.
) {
}
