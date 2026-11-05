package com.finflow.identity.dto.response;

/**
 * Result of {@code POST /auth/register}.
 * {@code token} = email-verification token in local/dev ({@code null} when real mail is enabled).
 */
public record RegisterResponse(
        UserProfileResponse profile,
        String token
) {
}
