
package com.finflow.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO used when a customer asks to return an order.
 *
 * This is a Java record, so Java automatically generates:
 * - Constructor
 * - Accessor method
 * - equals()
 * - hashCode()
 * - toString()
 *
 * The request currently contains one field: reason.
 */
public record ReturnRequestBody(

        /**
         * Return reason supplied by the customer.
         *
         * @NotBlank:
         * Rejects null, empty and whitespace-only values.
         *
         * @Size(max = 500):
         * Restricts the reason to a maximum of 500 characters.
         */
        @NotBlank
        @Size(max = 500)
        String reason

) {
}
