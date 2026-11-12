package com.finflow.commerce.order.dto;

import com.finflow.commerce.payment.PaymentOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request used to create an order directly (without cart checkout).
 * {@code sellerId} is optional when called from HTTP — inferred from product lines when omitted.
 * Cart checkout passes an explicit seller id per bucket. A record is suitable because this DTO
 * carries request data and does not need mutable fields.
 */
public record CreateOrderRequest(
        Long sellerId, // Internal seller key; optional for customer direct orders when inferred from items.
        @NotEmpty @Valid List<OrderItemRequest> items,
        @Valid ShippingAddressRequest shippingAddress,
        String couponCode,
        PaymentOutcome paymentOutcome
) {
}
