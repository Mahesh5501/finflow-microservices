package com.finflow.commerce.exception;

import java.time.Instant;
import java.util.List;

/**
 * Standard API error envelope returned by {@link GlobalExceptionHandler}.
 */
public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String traceId,
        List<String> details
) {
    public static ErrorResponse of(String code, String message, String traceId) {
        return new ErrorResponse(code, message, Instant.now(), traceId, List.of());
    }

    public static ErrorResponse of(String code, String message, String traceId, List<String> details) {
        return new ErrorResponse(code, message, Instant.now(), traceId, details);
    }
}
