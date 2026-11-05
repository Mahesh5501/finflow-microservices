package com.finflow.commerce.dto;

import com.finflow.commerce.payment.PaymentOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotNull Long organisationId,
        @NotEmpty @Valid List<OrderItemRequest> items,
        @Valid ShippingAddressRequest shippingAddress,
        String couponCode,
        /** Used when payment.provider=simulated; ignored for wallet provider. */
        PaymentOutcome paymentOutcome
) {
}
