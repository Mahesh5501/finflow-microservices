
package com.finflow.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Complete response representing a customer's cart.
 * A record is suitable because this DTO carries response data and does not need mutable fields.
 */
public record CartResponse(
        Long cartId, // Unique database identifier of the cart.
        Long organisationId, // Organisation that owns this cart.
        Long customerUserId, // Customer who owns this cart.
        BigDecimal subtotal, // Sum of all cart item line totals.
        Instant updatedAt, // Time when the cart was last updated.
        List<CartItemResponse> items // Individual product lines contained in the cart.
) {
}
