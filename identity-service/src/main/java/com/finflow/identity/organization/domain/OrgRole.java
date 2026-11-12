package com.finflow.identity.organization.domain;

/**
 * Organisation membership role (stored on {@link Membership}). Permission strings come from
 * {@code identity.authorization.org-role.*} in configuration — not hard-coded here.
 */
public enum OrgRole {
    ORG_ADMIN,
    FINANCE_OPERATOR,
    MEMBER
}
