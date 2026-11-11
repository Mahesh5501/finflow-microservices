package com.finflow.commerce.cart.dto;

import java.math.BigDecimal;

/**
 * Response representation of one item in a customer's cart.
 * Contains product details, requested quantity and calculated price.
 */
public record CartItemResponse(
        Long productId, // Product identifier.
        String productName, // Product name displayed to the customer.
        BigDecimal unitPrice, // Price of one unit of the product.
        int quantity, // Number of units currently requested in the cart.
        BigDecimal lineTotal, // Total price for this line: unitPrice multiplied by quantity.
        int availableQuantity, // Current available stock for this product.
        Long sellerOrganisationId // Internal seller id (shown as "Sold by" in UIs if needed).
) {
}
