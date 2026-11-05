package com.finflow.identity.entity;

/** Login is allowed only for ACTIVE users (interview: account lifecycle). */
public enum UserStatus {
    ACTIVE,
    DISABLED
}
