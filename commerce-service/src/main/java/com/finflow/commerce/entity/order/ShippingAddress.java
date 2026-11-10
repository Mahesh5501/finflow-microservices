
package com.finflow.commerce.entity.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class ShippingAddress {

    // First address line: house number, street, building, etc.
    @Column(name = "ship_line1", length = 200)
    private String line1;

    // Optional second address line: apartment, landmark, etc.
    @Column(name = "ship_line2", length = 200)
    private String line2;

    // City or town.
    @Column(name = "ship_city", length = 100)
    private String city;

    // State or region.
    @Column(name = "ship_state", length = 100)
    private String state;

    // Postal or PIN code.
    @Column(name = "ship_postal_code", length = 20)
    private String postalCode;

    // Two-character country code, such as IN or US.
    @Column(name = "ship_country", length = 2)
    private String country;

    // Required by JPA to create the embedded object when loading data.
    protected ShippingAddress() {
    }

    // Creates an address from the supplied address details.
    public ShippingAddress(
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country
    ) {
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country == null ? null : country.trim().toUpperCase();
    }
}
