package com.finflow.commerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(
        @NotNull Long organisationId,
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity
) {
}
