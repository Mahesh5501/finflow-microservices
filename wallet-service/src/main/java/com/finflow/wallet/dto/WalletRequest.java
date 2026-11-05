package com.finflow.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Body for create-wallet.
 * {@code userId} comes from the JWT subject — never trust a client-supplied owner id.
 */
public record WalletRequest(

        @NotBlank(message = "Currency is required")
        @Pattern(
                regexp = "(?i)INR|USD|EUR|GBP",
                message = "Currency must be one of: INR, USD, EUR, GBP"
        )
        String currency
) {
}
