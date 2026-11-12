package com.finflow.identity.auth.domain;

/** Login is allowed only for ACTIVE users (interview: account lifecycle). */
public enum UserStatus {
    ACTIVE,
    DISABLED
}
