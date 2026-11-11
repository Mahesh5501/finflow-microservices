package com.finflow.commerce.order.dto;

import com.finflow.commerce.payment.PaymentOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request received when a customer checks out their cart.
 * A record is suitable because this DTO carries request data and does not need mutable fields.
 */
public record CheckoutRequest(
        Long organisationId, // Optional seller override; otherwise inferred from cart (one seller).
        @NotNull @Valid ShippingAddressRequest shippingAddress, // Delivery address for the order. @Valid tells Bean Validation to validate the fields inside ShippingAddressRequest as well.
        String couponCode, // Optional coupon code entered by the customer. Null means that the customer is not applying a coupon.
        PaymentOutcome paymentOutcome // Allows the simulated adapter to produce outcomes such as APPROVED, DECLINED or UNCERTAIN.
) {
}
