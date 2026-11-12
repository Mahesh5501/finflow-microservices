package com.finflow.identity.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for {@code POST /api/v1/orgs}. */
public record CreateOrgRequest(@NotBlank @Size(min = 2, max = 120) String name) {
}
