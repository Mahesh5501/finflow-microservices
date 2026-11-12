
package com.finflow.wallet.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositRequest(

        @NotNull(message = "Deposit amount is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "Deposit amount must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "Deposit amount must have at most 17 integer digits and 2 decimal places")
        BigDecimal amount

) {}
