package com.finflow.commerce.dto;

import com.finflow.commerce.entity.ShippingAddress;

public record ShippingAddressResponse(
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country
) {
    public static ShippingAddressResponse from(ShippingAddress address) {
        if (address == null || address.getLine1() == null) {
            return null;
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
