package com.finflow.wallet.wallet.dto;

import com.finflow.wallet.payment.TopUpPaymentMethod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Customer "add money" — amount plus PG session reference (from client/SDK after user pays).
 */
public record TopUpRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be greater than zero")
        @DecimalMax(value = "1000000.00", message = "Amount must be at most 1,000,000.00")
        @Digits(integer = 17, fraction = 2, message = "Amount must have at most 17 integer digits and 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "paymentMethod is required")
        TopUpPaymentMethod paymentMethod,

        /**
         * Client-generated idempotency for the payment session (e.g. Razorpay order_id).
         * Paired with header {@code Idempotency-Key} for wallet-side replay safety.
         */
        @NotBlank(message = "paymentReference is required")
        String paymentReference
) {
}
