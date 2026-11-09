
package com.finflow.commerce.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom runtime exception used for business/API errors.
 *
 * The HTTP status and application-specific error code are
 * interpreted by GlobalExceptionHandler.
 */
public class ApiException extends RuntimeException {

    // HTTP status that should be returned to the API caller.
    private final HttpStatus status;

    // Stable application error code.
    // Clients can use this code to identify the type of failure.
    private final String code;

    /**
     * Creates an API exception with an HTTP status, error code
     * and human-readable message.
     */
    public ApiException(HttpStatus status, String code, String message) {

        // Pass the message to RuntimeException.
        // This makes it available through getMessage().
        super(message);

        this.status = status;
        this.code = code;
    }

    // Returns the HTTP status.
    public HttpStatus getStatus() {
        return status;
    }

    // Returns the application error code.
    public String getCode() {
        return code;
    }

    /**
     * Creates a 409 Conflict exception.
     *
     * Example: duplicate coupon code or insufficient stock.
     */
    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    /**
     * Creates a 404 Not Found exception.
     *
     * Example: requested product or order does not exist.
     */
    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    /**
     * Creates a 403 Forbidden exception.
     *
     * Example: customer tries to access another organisation's data.
     */
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    /**
     * Creates a 422 Unprocessable Entity exception.
     *
     * Example: product exists but belongs to another organisation.
     */
    public static ApiException unprocessable(String code, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }

    /**
     * Creates a 400 Bad Request exception.
     *
     * Example: required Idempotency-Key header is missing.
     */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
