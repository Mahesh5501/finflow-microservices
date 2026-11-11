package com.finflow.commerce.catalogue.domain;

/**
 * Lifecycle state of a catalogue product.
 *
 * ACTIVE products can be offered and have stock reserved. INACTIVE products
 * remain stored (important for historical orders) but cannot be newly sold.
 * EnumType.STRING is used by Product so database values remain readable and
 * are not coupled to enum ordinal positions.
 */
public enum ProductStatus {
    ACTIVE,
    INACTIVE
}
