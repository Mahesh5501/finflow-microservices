package com.finflow.commerce.order.dto;

import com.finflow.commerce.order.domain.OrderItem;

import java.math.BigDecimal;

/**
 * Response DTO representing one item purchased in an order.
 * A record is suitable because this DTO carries response data and does not need mutable fields.
 * Product details are read from the order-item snapshot, not from the current Product entity.
 */
public record OrderItemResponse(
        Long productId, // ID of the purchased product.
        String productName, // Product name saved when the order was created.
        BigDecimal unitPrice, // Unit price saved when the order was created.
        int quantity, // Quantity purchased.
        BigDecimal lineTotal // Total amount for this order line.
) {

    /**
     * Converts an OrderItem entity into an OrderItemResponse.
     * Keeping this mapping method here avoids repeating the same entity-to-response conversion in service methods.
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
