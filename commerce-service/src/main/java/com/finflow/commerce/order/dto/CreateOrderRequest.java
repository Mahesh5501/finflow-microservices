package com.finflow.commerce.order.dto;

import com.finflow.commerce.payment.PaymentOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request used to create an order directly.
 * A record is suitable because this DTO carries request data and does not need mutable fields.
 * Unlike CheckoutRequest, this request contains the product IDs and quantities that should be included in the order.
 */
public record CreateOrderRequest(
        Long organisationId, // Optional — inferred from product lines when omitted.
        @NotEmpty @Valid List<OrderItemRequest> items, // Requested order lines. @NotEmpty requires at least one item. @Valid validates each OrderItemRequest inside the list.
        @Valid ShippingAddressRequest shippingAddress, // Optional shipping address. The current DTO permits null. If supplied, its nested fields are validated.
        String couponCode, // Optional coupon code. Null means that the customer is not applying a coupon.
        PaymentOutcome paymentOutcome // Used when payment.provider=simulated. Ignored by the wallet payment provider.
) {
}
