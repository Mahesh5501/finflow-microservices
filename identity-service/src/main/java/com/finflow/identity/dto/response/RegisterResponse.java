package com.finflow.identity.dto.response;

/**
 * Result of {@code POST /auth/register}.
 * {@code token} = email-verification token when {@code mail.enabled=false}; {@code null} when SMTP sends it.
 */
public record RegisterResponse(UserProfileResponse profile, String token) {
}
