
package com.finflow.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Complete response representing a customer's cart.
 *
 * Contains cart identity, ownership, calculated subtotal,
 * last-update time and individual cart lines.
 */
public record CartResponse(

        // Unique database identifier of the cart.
        Long cartId,

        // Organisation that owns this cart.
        Long organisationId,

        // Customer who owns this cart.
        Long customerUserId,

        // Sum of all cart item line totals.
        BigDecimal subtotal,

        // Time when the cart was last updated.
        Instant updatedAt,

        // Individual product lines contained in the cart.
        List<CartItemResponse> items

) {
}
