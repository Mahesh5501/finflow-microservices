
package com.finflow.commerce.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/**
 * Central exception handler for REST controllers.
 *
 * Converts application exceptions into consistent HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles custom business/API exceptions.
     *
     * Uses the status and error code supplied by ApiException.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> api(ApiException ex) {

        return ResponseEntity.status(ex.getStatus())
                .body(Map.of(
                        "code", ex.getCode(),
                        "message", ex.getMessage()
                ));
    }

    /**
     * Handles validation failures for request bodies.
     *
     * Example:
     * ProductRequest has @NotBlank on its name field,
     * but the client sends an empty name.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(
            MethodArgumentNotValidException ex) {

        // Extract every field-level validation error.
        // Example: "name: must not be blank".
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage()
                )
                .toList();

        // Return HTTP 400 with a common error code,
        // a general message and individual validation details.
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "code", "VALIDATION_FAILED",
                        "message", "Validation failed",
                        "details", details
                ));
    }

    /**
     * Handles common client-side request errors.
     *
     * Includes:
     * - ConstraintViolationException
     * - IllegalArgumentException
     * - MissingRequestHeaderException
     */
    @ExceptionHandler({
            ConstraintViolationException.class,
            IllegalArgumentException.class,
            MissingRequestHeaderException.class
    })
    public ResponseEntity<Map<String, String>> badRequest(Exception ex) {

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "code", "BAD_REQUEST",
                        "message", ex.getMessage()
                ));
    }

    /**
     * Handles optimistic-locking conflicts.
     *
     * This can happen when another transaction updates a versioned
     * entity before the current transaction completes.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> optimisticLock(
            OptimisticLockingFailureException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "code", "CONCURRENT_UPDATE",
                        "message",
                        "Stock changed concurrently. Retry with the same Idempotency-Key."
                ));
    }

    /**
     * Handles database integrity conflicts.
     *
     * Example: attempting to insert a value that violates a
     * database uniqueness constraint.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> dataConflict(
            DataIntegrityViolationException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "code", "DATA_CONFLICT",
                        "message", "A uniqueness constraint was violated."
                ));
    }
}
