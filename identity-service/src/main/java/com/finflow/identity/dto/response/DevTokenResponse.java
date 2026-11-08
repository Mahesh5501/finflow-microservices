package com.finflow.identity.dto.response;

/**
 * Standard “message + optional token” response for email-driven flows.
 * <ul>
 *   <li>{@code message} — always returned (safe for prod, anti-enumeration)</li>
 *   <li>{@code token} — filled only when {@code mail.enabled=false}; when mail is on, read the token from email</li>
 * </ul>
 * Used by: forgot-password, resend-verification.
 */
public record DevTokenResponse(
        String message,
        String token
) {
}
