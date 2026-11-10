
package com.finflow.commerce.dto.order;

import com.finflow.commerce.entity.order.CommerceOrder;
import com.finflow.commerce.entity.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO representing a complete commerce order.
 * A record is suitable because this DTO carries response data and does not need mutable fields.
 * Contains order information, pricing,  payment references, shipping address and all purchased order items.
 */
public record OrderResponse(
        Long orderId, // Database-generated order ID.
        String orderNumber, // Human-readable order reference.
        Long organisationId, // Organisation that owns the order.
        Long customerUserId, // Customer who placed the order.
        OrderStatus status, // Current order lifecycle status.
        BigDecimal totalAmount, // Final order amount.
        BigDecimal discountAmount, // Discount applied to the order.
        String couponCode, // Coupon code applied, if any.
        String currencyCode, // Currency used for this order, such as INR.
        ShippingAddressResponse shippingAddress, // Delivery address captured for the order. Converted into its response DTO.
        Long paymentWalletId, // Wallet used for payment, when available.
        Long paymentTransactionId, // Wallet transaction ID, when available.
        Instant createdAt, // Time at which the order was created.
        List<OrderItemResponse> items // All purchased items represented as response DTOs.
) {

    /**
     * Converts a CommerceOrder entity into an OrderResponse.
     * Keeping this mapping method here avoids repeating the same entity-to-response conversion in service methods.
     */
    public static OrderResponse from(CommerceOrder order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrganisationId(),
                order.getCustomerUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getCouponCode(),
                order.getCurrencyCode(),
                ShippingAddressResponse.from(order.getShippingAddress()),
                order.getPaymentWalletId(),
                order.getPaymentTransactionId(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList() // Convert every OrderItem entity into an OrderItemResponse. toList() creates the resulting list.
        );
    }
}
