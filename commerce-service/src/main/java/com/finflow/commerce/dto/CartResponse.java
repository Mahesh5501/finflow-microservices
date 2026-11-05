package com.finflow.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(
        Long cartId,
        Long organisationId,
        Long customerUserId,
        BigDecimal subtotal,
        Instant updatedAt,
        List<CartItemResponse> items
) {
}
