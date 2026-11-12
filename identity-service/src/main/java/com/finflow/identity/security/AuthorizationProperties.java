package com.finflow.identity.security;

import com.finflow.identity.organization.domain.OrgRole;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Single place for JWT permission labels: loads {@code identity.authorization.*} from config,
 * validates at startup, and resolves labels for verified shoppers and org roles.
 */
@Validated
@Component
@ConfigurationProperties(prefix = "identity.authorization")
public class AuthorizationProperties {

    @NotBlank
    private String verifiedConsumer;

    @NotEmpty
    private Map<String, String> orgRole = new HashMap<>();

    @PostConstruct
    void requireEveryOrgRole() {
        for (OrgRole role : OrgRole.values()) {
            if (rolePermissions(role).isEmpty()) {
                throw new IllegalStateException(
                        "Missing or empty identity.authorization.org-role." + role.name());
            }
        }
    }

    public List<String> verifiedConsumerPermissions() {
        return List.copyOf(splitCsv(verifiedConsumer));
    }

    public List<String> rolePermissions(OrgRole role) {
        if (role == null) {
            return List.of();
        }
        String csv = orgRole.get(role.name());
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return List.copyOf(splitCsv(csv));
    }

    public boolean roleGrants(OrgRole role, String permission) {
        if (permission == null || permission.isBlank()) {
            return false;
        }
        String normalized = permission.trim().toLowerCase(Locale.ROOT);
        return rolePermissions(role).stream()
                .anyMatch(p -> p.equalsIgnoreCase(normalized));
    }

    public String getVerifiedConsumer() {
        return verifiedConsumer;
    }

    public void setVerifiedConsumer(String verifiedConsumer) {
        this.verifiedConsumer = verifiedConsumer;
    }

    public Map<String, String> getOrgRole() {
        return orgRole;
    }

    public void setOrgRole(Map<String, String> orgRole) {
        this.orgRole = orgRole;
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
