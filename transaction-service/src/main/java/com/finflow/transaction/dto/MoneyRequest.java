package com.finflow.transaction.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Body for deposit / withdraw. */
public record MoneyRequest(

        @NotNull @Positive Long walletId,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @DecimalMax(value = "1000000.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount
) {
}
