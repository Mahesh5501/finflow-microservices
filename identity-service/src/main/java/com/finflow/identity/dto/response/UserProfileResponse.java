package com.finflow.identity.dto.response;

import com.finflow.identity.entity.UserStatus;

import java.time.Instant;
import java.util.List;

/** User profile — nested under register response, and returned by {@code GET /users/me}. */
public record UserProfileResponse(
        Long userId,
        String email,
        UserStatus status,
        boolean emailVerified,
        Instant createdAt,
        List<Long> orgIds,
        List<String> permissions
) {
}
