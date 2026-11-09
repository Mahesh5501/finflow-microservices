
package com.finflow.commerce.dto;

import com.finflow.commerce.payment.PaymentOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request received when a customer checks out their cart.
 *
 * This DTO contains checkout-level information.
 * Individual product quantities are taken from the customer's cart,
 * rather than supplied again in this request.
 */
public record CheckoutRequest(

        // Organisation in which the cart and checkout belong.
        // Required because this application supports multiple organisations.
        @NotNull Long organisationId,

        // Delivery address for the order.
        // @Valid tells Bean Validation to validate the fields
        // inside ShippingAddressRequest as well.
        @NotNull @Valid ShippingAddressRequest shippingAddress,

        // Optional coupon code entered by the customer.
        // Null means that the customer is not applying a coupon.
        String couponCode,

        /**
         * Used when payment.provider=simulated.
         *
         * Allows the simulated adapter to produce outcomes such as
         * APPROVED, DECLINED or UNCERTAIN.
         */
        PaymentOutcome paymentOutcome

) {
}
