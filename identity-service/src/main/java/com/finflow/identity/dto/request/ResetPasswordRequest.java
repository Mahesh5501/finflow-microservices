package com.finflow.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for {@code POST /api/v1/auth/reset-password}. */
public record ResetPasswordRequest(@NotBlank String resetToken, @NotBlank @Size(min = 8, max = 72) String newPassword) {
}
