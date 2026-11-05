package com.finflow.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReturnRequestBody(
        @NotBlank @Size(max = 500) String reason
) {
}
