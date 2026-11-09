
package com.finflow.commerce.dto;

import java.math.BigDecimal;

/**
 * Response representation of one item in a customer's cart.
 *
 * Contains product details, requested quantity and calculated price.
 */
public record CartItemResponse(

        // Product identifier.
        Long productId,

        // Product name displayed to the customer.
        String productName,

        // Price of one unit of the product.
        BigDecimal unitPrice,

        // Number of units currently requested in the cart.
        int quantity,

        // Total price for this line: unitPrice multiplied by quantity.
        BigDecimal lineTotal,

        // Current available stock for this product.
        int availableQuantity

) {
}
