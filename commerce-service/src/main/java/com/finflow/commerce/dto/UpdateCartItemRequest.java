package com.finflow.commerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @NotNull Long organisationId,
        @NotNull @Min(0) Integer quantity
) {
}
