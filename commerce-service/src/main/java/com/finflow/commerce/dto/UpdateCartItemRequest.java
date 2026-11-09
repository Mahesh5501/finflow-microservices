
package com.finflow.commerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request used to update the quantity of a product in the cart.
 *
 * A quantity of zero is allowed and is handled by CartService
 * as a request to remove the item.
 */
public record UpdateCartItemRequest(

        // Organisation to which the cart belongs.
        @NotNull Long organisationId,

        // New quantity for the cart item.
        // Zero is allowed; negative quantities are rejected.
        @NotNull @Min(0) Integer quantity

) {
}
