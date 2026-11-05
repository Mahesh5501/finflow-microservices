package com.finflow.wallet.security;

import java.util.List;

/** Caller identity from the identity-service access JWT. */
public record AuthenticatedUser(Long userId, String email, List<String> permissions) {
}
