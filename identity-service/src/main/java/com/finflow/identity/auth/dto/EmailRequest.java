package com.finflow.identity.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Shared body for endpoints that only need an email:
 * {@code POST /auth/forgot-password}, {@code POST /auth/resend-verification}.
 */
public record EmailRequest(@NotBlank @Email String email) {
}
