
package com.finflow.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO containing the shipping address supplied by the client.
 * A record is suitable because this DTO carries request data and does not need mutable fields.
 * Contains the shipping address supplied by the client. Used when creating an order or checking out a cart.
 */
public record ShippingAddressRequest(
        @NotBlank @Size(max = 200) String line1, // First address line is mandatory. Example: Flat 302, ABC Apartments.
        @Size(max = 200) String line2, // Second address line is optional. Example: Near City Mall.
        @NotBlank @Size(max = 100) String city, // City is mandatory.
        @Size(max = 100) String state, // State or province is optional.
        @NotBlank @Size(max = 20) String postalCode, // Postal or ZIP code is mandatory.
        @NotBlank @Size(min = 2, max = 2) String country // Country code is mandatory and must contain exactly 2 characters. Example: IN, US, GB.
) {
}
