
package com.finflow.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO containing the shipping address supplied by the client.
 *
 * Used when creating an order or checking out a cart.
 */
public record ShippingAddressRequest(

        // First address line is mandatory.
        // Example: Flat 302, ABC Apartments.
        @NotBlank
        @Size(max = 200)
        String line1,

        // Second address line is optional.
        // Example: Near City Mall.
        @Size(max = 200)
        String line2,

        // City is mandatory.
        @NotBlank
        @Size(max = 100)
        String city,

        // State or province is optional.
        @Size(max = 100)
        String state,

        // Postal or ZIP code is mandatory.
        @NotBlank
        @Size(max = 20)
        String postalCode,

        // Country code is mandatory and must contain exactly 2 characters.
        // Example: IN, US, GB.
        @NotBlank
        @Size(min = 2, max = 2)
        String country

) {
}
