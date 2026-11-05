package com.finflow.commerce.dto;

import com.finflow.commerce.payment.PaymentOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull Long organisationId,
        @NotNull @Valid ShippingAddressRequest shippingAddress,
        String couponCode,
        /** Used when payment.provider=simulated. */
        PaymentOutcome paymentOutcome
) {
}
