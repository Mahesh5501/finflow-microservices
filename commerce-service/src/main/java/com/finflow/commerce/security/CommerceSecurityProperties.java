package com.finflow.commerce.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Security settings owned by Commerce, bound once at startup rather than from HTTP headers. 
 * JWT verification defaults to enabled even if the property is omitted.
 * The local identity exists only for the explicitly guarded local testing mode; normal callers still come from verified JWT claims.
 * @ConfigurationProperties gives typed configuration. 
 * SecurityConfig registers this record with @EnableConfigurationProperties; without registration Spring cannot inject it. 
 * @DefaultValue supplies safe defaults when a property is absent. 
 * A fixed configured identity keeps existing @AuthenticationPrincipal arguments and owner/seller checks useful without trusting caller-supplied user or organisation IDs.
 */
@ConfigurationProperties("commerce.security")
public record CommerceSecurityProperties(
        @DefaultValue("true") boolean jwtValidationEnabled,
        @DefaultValue("1") long localUserId,
        @DefaultValue("10") List<Long> localOrganisationIds
) {
    public CommerceSecurityProperties {
        if (localUserId <= 0) {
            throw new IllegalArgumentException("Local test user ID must be positive");
        }
        localOrganisationIds = List.copyOf(localOrganisationIds);
        if (localOrganisationIds.stream().anyMatch(id -> id <= 0)) {
            throw new IllegalArgumentException("Local test organisation IDs must be positive");
        }
    }

    /** Fixed local principal; request bodies and unverified token claims cannot override it. */
    public AuthenticatedUser localUser() {
        return new AuthenticatedUser(localUserId, "local-test@finflow.invalid", localOrganisationIds, List.of("commerce:read", "commerce:write"));
    }
}
