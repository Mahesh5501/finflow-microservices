package com.finflow.commerce.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom runtime exception used for business/API errors.
 * The HTTP status and application-specific error code are interpreted by GlobalExceptionHandler.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status; // HTTP status that should be returned to the API caller.

    private final String code; // Stable application error code. Clients can use this code to identify the type of failure.

    /**
     * Creates an API exception with an HTTP status, error code and human-readable message.
     * @param status HTTP status that should be returned to the API caller.
     * @param code Stable application error code. Clients can use this code to identify the type of failure.
     * @param message Human-readable message.
     */
    public ApiException(HttpStatus status, String code, String message) {
        super(message); // Pass the message to RuntimeException. This makes it available through getMessage().
        this.status = status; // HTTP status that should be returned to the API caller.
        this.code = code; // Stable application error code. Clients can use this code to identify the type of failure.
    }

    /**
     * Returns the HTTP status.
     * @return HTTP status that should be returned to the API caller.
     */
    public HttpStatus getStatus() {
        return status; // HTTP status that should be returned to the API caller.
    }

    /**
     * Returns the application error code.
     * @return Stable application error code. Clients can use this code to identify the type of failure.
     */
    public String getCode() {
        return code; // Stable application error code. Clients can use this code to identify the type of failure.
    }

    /**
     * Creates a 409 Conflict exception.
     * @param code Stable application error code. Clients can use this code to identify the type of failure.
     * @param message Human-readable message.
     * Example: duplicate coupon code or insufficient stock.
     */
    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message); // Create a new ApiException with the HTTP status, error code and human-readable message. Returns the new ApiException.
    }

    /**
     * Creates a 404 Not Found exception.
     * @param code Stable application error code. Clients can use this code to identify the type of failure.
     * @param message Human-readable message.
     * Example: requested product or order does not exist.
     */
    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message); // Create a new ApiException with the HTTP status, error code and human-readable message. Returns the new ApiException.
    }

    /**
     * Creates a 403 Forbidden exception.
     * @param message Human-readable message.
     * Example: customer tries to access another organisation's data.
     */
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message); // Create a new ApiException with the HTTP status, error code and human-readable message. Returns the new ApiException.
    }

    /**
     * Creates a 422 Unprocessable Entity exception.
     * @param code Stable application error code. Clients can use this code to identify the type of failure.
     * @param message Human-readable message.
     * Example: product exists but belongs to another organisation.
     */
    public static ApiException unprocessable(String code, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message); // Create a new ApiException with the HTTP status, error code and human-readable message. Returns the new ApiException.
    }

    /**
     * Creates a 400 Bad Request exception.
     * @param message Human-readable message.
     * Example: required Idempotency-Key header is missing.
     */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message); // Create a new ApiException with the HTTP status, error code and human-readable message. Returns the new ApiException.
    }
}
