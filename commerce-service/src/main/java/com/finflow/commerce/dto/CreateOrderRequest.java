
package com.finflow.commerce.dto;

import com.finflow.commerce.payment.PaymentOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request used to create an order directly.
 *
 * Unlike CheckoutRequest, this request contains the product IDs
 * and quantities that should be included in the order.
 */
public record CreateOrderRequest(

        // Organisation in which the order is being created.
        @NotNull Long organisationId,

        // Requested order lines.
        // @NotEmpty requires at least one item.
        // @Valid validates each OrderItemRequest inside the list.
        @NotEmpty @Valid List<OrderItemRequest> items,

        // Optional shipping address.
        // The current DTO permits null.
        // If supplied, its nested fields are validated.
        @Valid ShippingAddressRequest shippingAddress,

        // Optional coupon code.
        String couponCode,

        /**
         * Used when payment.provider=simulated.
         * Ignored by the wallet payment provider.
         */
        PaymentOutcome paymentOutcome

) {
}
