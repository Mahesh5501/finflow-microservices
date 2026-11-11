package com.finflow.commerce.returns.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO used when a customer asks to return an order.
 * A record is suitable because this DTO carries request data and does not need mutable fields. 
 * The request currently contains one field: reason. The reason is supplied by the customer when they ask to return an order.
 */
public record ReturnRequestBody(
        @NotBlank @Size(max = 500) String reason // Return reason supplied by the customer. Rejects null, empty and whitespace-only values. Restricts the reason to a maximum of 500 characters.
) {
}
