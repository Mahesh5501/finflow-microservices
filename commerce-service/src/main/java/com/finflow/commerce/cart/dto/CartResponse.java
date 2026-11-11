package com.finflow.commerce.cart.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Complete response representing a customer's cart.
 * A record is suitable because this DTO carries response data and does not need mutable fields.
 */
public record CartResponse(
        Long cartId, // Null for merged marketplace cart spanning multiple sellers.
        Long organisationId, // Null for merged marketplace cart.
        Long customerUserId, // Customer who owns this cart.
        BigDecimal subtotal, // Sum of all cart item line totals.
        Instant updatedAt, // Latest cart update across all seller buckets.
        List<CartItemResponse> items // Individual product lines contained in the cart.
) {
}
