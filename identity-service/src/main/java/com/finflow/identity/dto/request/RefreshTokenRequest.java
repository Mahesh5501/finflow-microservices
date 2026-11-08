package com.finflow.identity.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Shared body for refresh + logout (both need the opaque refresh token).
 * {@code POST /auth/refresh}, {@code POST /auth/logout}.
 */
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}
