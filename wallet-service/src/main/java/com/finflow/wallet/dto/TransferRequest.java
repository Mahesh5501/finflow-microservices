package com.finflow.wallet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Body for wallet-to-wallet transfer. */
public record TransferRequest(

        @NotNull(message = "From wallet ID is required")
        @Positive(message = "From wallet ID must be positive")
        Long fromWalletId,

        @NotNull(message = "To wallet ID is required")
        @Positive(message = "To wallet ID must be positive")
        Long toWalletId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be greater than zero")
        @DecimalMax(value = "1000000.00", message = "Amount must be at most 1,000,000.00")
        @Digits(integer = 17, fraction = 2, message = "Amount must have at most 17 integer digits and 2 decimal places")
        BigDecimal amount
) {
}
