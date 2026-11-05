package com.finflow.transaction.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Body for orchestrated P2P transfer (LLD: sourceWalletId / destinationWalletId). */
public record TransferRequest(

        @NotNull @Positive Long sourceWalletId,

        @NotNull @Positive Long destinationWalletId,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @DecimalMax(value = "1000000.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount
) {
}
