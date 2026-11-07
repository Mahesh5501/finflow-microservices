package com.finflow.identity.security.authentication;

import java.util.List;

/**
 * Who is calling this request — built from the access JWT by {@link JwtAuthFilter}.
 * Controllers read {@code userId} from here; never trust a userId from the request body.
 */
public record AuthenticatedUser(Long userId, String email, List<String> permissions) {
}
