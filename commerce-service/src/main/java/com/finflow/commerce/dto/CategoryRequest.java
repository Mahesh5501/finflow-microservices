package com.finflow.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotNull Long organisationId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 140) String slug
) {
}
