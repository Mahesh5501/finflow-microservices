package com.finflow.commerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotNull Long organisationId,
        @NotBlank String name,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
        @Min(0) int availableQuantity,
        Long categoryId
) {
}
