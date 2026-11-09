
package com.finflow.commerce.dto;

import com.finflow.commerce.entity.OrderItem;

import java.math.BigDecimal;

/**
 * Response DTO representing one item purchased in an order.
 *
 * Product details are read from the order-item snapshot,
 * not from the current Product entity.
 */
public record OrderItemResponse(

        // ID of the purchased product.
        Long productId,

        // Product name saved when the order was created.
        String productName,

        // Unit price saved when the order was created.
        BigDecimal unitPrice,

        // Quantity purchased.
        int quantity,

        // Total amount for this order line.
        BigDecimal lineTotal

) {

    /**
     * Converts an OrderItem entity into an OrderItemResponse.
     */
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductNameSnapshot(),
                item.getUnitPriceSnapshot(),
                item.getQuantity(),
                item.lineTotal()
        );
    }
}
