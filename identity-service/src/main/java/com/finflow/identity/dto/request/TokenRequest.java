package com.finflow.identity.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Shared body for one-time opaque tokens:
 * {@code POST /auth/verify-email}, {@code POST /orgs/invites/accept}.
 * JSON: {@code { "token": "..." }}.
 */
public record TokenRequest(
        @NotBlank String token
) {
}
