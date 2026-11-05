package com.finflow.identity.entity;

import java.util.List;

/**
 * Role assigned on a Membership. Permissions go into the access JWT.
 * Interview: RBAC = assign a role; APIs check permission strings, not role names.
 *
 * <pre>
 * ORG_ADMIN         → org + members + wallets + transfers + commerce
 * FINANCE_OPERATOR  → wallets + transfers + commerce
 * MEMBER            → own wallet read/write + commerce (checkout debits wallet; no P2P)
 * </pre>
 */
public enum OrgRole {
    ORG_ADMIN(List.of(
            "org:manage", "members:manage",
            "wallets:read", "wallets:write", "transfers:create",
            "commerce:read", "commerce:write"
    )),
    FINANCE_OPERATOR(List.of(
            "wallets:read", "wallets:write", "transfers:create",
            "commerce:read", "commerce:write"
    )),
    MEMBER(List.of(
            "wallets:read", "wallets:write",
            "commerce:read", "commerce:write"
    ));

    private final List<String> permissions;

    OrgRole(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<String> permissions() {
        return permissions;
    }
}
