
package com.finflow.commerce.dto;

import com.finflow.commerce.entity.ShippingAddress;

/**
 * Response DTO representing a shipping address.
 *
 * Keeps the API response separate from the persistence entity.
 */
public record ShippingAddressResponse(

        // First address line.
        String line1,

        // Optional second address line.
        String line2,

        // City.
        String city,

        // State or province.
        String state,

        // Postal or ZIP code.
        String postalCode,

        // Two-character country code.
        String country

) {

    /**
     * Converts a ShippingAddress entity into a response DTO.
     *
     * Returns null if the address itself is null or if its
     * mandatory first address line is missing.
     */
    public static ShippingAddressResponse from(ShippingAddress address) {

        // Defensive check:
        // Avoid dereferencing a null address.
        //
        // Also treat an address without line1 as absent.
        if (address == null || address.getLine1() == null) {
            return null;
        }

        // Copy the entity values into the response record.
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
