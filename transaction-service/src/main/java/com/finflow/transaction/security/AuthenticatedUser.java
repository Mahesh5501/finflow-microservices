package com.finflow.transaction.security;

import java.util.List;

/** Caller from identity-service access JWT. */
public record AuthenticatedUser(Long userId, String email, List<Long> orgIds, List<String> permissions) {
}
