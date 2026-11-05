package com.finflow.identity.dto.response;

import java.util.List;

/**
 * Login / refresh response — access JWT (RS256, short-lived) + opaque refresh token.
 * Client sends access as {@code Authorization: Bearer ...}; uses refresh to rotate.
 */
public record AuthenticationResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        Long userId,
        String email,
        List<Long> orgIds,
        List<String> permissions
) {
}
