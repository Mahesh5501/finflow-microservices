
package com.finflow.commerce.security;

import java.util.List;

/**
 * Represents the authenticated caller extracted from an identity-service access JWT.
 * This object becomes the principal stored in Spring Security's SecurityContext after successful JWT verification.
 */
public record AuthenticatedUser(
        Long userId, // Unique user ID extracted from the JWT subject (sub).
        String email, // Email address extracted from the JWT email claim.
        List<Long> orgIds, // Organisation IDs that the authenticated user belongs to. Controllers/services can use these to enforce organisation scope.
        List<String> permissions // Permissions granted to this user. These are also converted into Spring Security authorities.
) { 
}
