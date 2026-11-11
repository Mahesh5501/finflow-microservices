package com.finflow.commerce.order.dto;

import com.finflow.commerce.order.domain.ShippingAddress;

/**
 * Response DTO representing a shipping address.
 * A record is suitable because this DTO carries response data and does not need mutable fields.
 * Contains the shipping address in a format suitable for the API response.
 */
public record ShippingAddressResponse(
        String line1, // First address line.
        String line2, // Optional second address line.
        String city, // City.
        String state, // State or province.
        String postalCode, // Postal or ZIP code.
        String country // Two-character country code.
) {

    /**
     * Converts a ShippingAddress entity into a response DTO.
     * Keeping this mapping method here avoids repeating the same entity-to-response conversion in service methods.
     * Returns null if the address itself is null or if its mandatory first address line is missing.
     */
    public static ShippingAddressResponse from(ShippingAddress address) {
        // Defensive check: Avoid dereferencing a null address and also treat an address without line1 as absent.
        if (address == null || address.getLine1() == null) {
            return null; // Return null if the address itself is null or if its mandatory first address line is missing.
        }
        return new ShippingAddressResponse(
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()
        );
    }
}
