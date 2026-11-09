
package com.finflow.commerce.security;

import java.util.List;

/**
 * Represents the authenticated caller extracted from an identity-service access JWT.
 * This object becomes the principal stored in Spring Security's SecurityContext after successful JWT verification.
 */
public record AuthenticatedUser(

        /**
         * Unique user ID extracted from the JWT subject (sub).
         * @return the user ID.
         */
        Long userId,

        /**
         * Email address extracted from the JWT email claim.
         * @return the email address.
         */
        String email,

        /**
         * Organisation IDs that the authenticated user belongs to.
         * Controllers/services can use these to enforce organisation scope.
         * @return the organisation IDs.
         */
        List<Long> orgIds,

        /**
         * Permissions granted to this user.
         * These are also converted into Spring Security authorities.
         * @return the permissions.
         */
        List<String> permissions
) {
}
