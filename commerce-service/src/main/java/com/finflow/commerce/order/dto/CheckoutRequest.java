package com.finflow.commerce.order.dto;

import com.finflow.commerce.payment.PaymentOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request received when a customer checks out their cart.
 * Sellers are taken from cart buckets — customers do not supply a seller id.
 * A record is suitable because this DTO carries request data and does not need mutable fields.
 */
public record CheckoutRequest(
        @NotNull @Valid ShippingAddressRequest shippingAddress, // Delivery address; @Valid validates nested fields.
        String couponCode, // Optional; applied per order when the coupon belongs to that seller.
        PaymentOutcome paymentOutcome // Simulated payment adapter only; ignored when payment.provider=wallet.
) {
}
