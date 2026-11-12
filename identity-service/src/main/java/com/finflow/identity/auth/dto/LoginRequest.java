package com.finflow.identity.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for {@code POST /api/v1/auth/login}. */
public record LoginRequest(@NotBlank @Email String email, @NotBlank @Size(min = 8, max = 72) String password) {
}
