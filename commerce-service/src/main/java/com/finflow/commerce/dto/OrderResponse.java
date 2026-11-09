
package com.finflow.commerce.dto;

import com.finflow.commerce.entity.CommerceOrder;
import com.finflow.commerce.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO representing a complete commerce order.
 *
 * Contains order information, pricing, payment references,
 * shipping address and all purchased order items.
 */
public record OrderResponse(

        // Database-generated order ID.
        Long orderId,

        // Human-readable order reference.
        String orderNumber,

        // Organisation that owns the order.
        Long organisationId,

        // Customer who placed the order.
        Long customerUserId,

        // Current order lifecycle status.
        OrderStatus status,

        // Final order amount.
        BigDecimal totalAmount,

        // Discount applied to the order.
        BigDecimal discountAmount,

        // Coupon code applied, if any.
        String couponCode,

        // Currency used for this order, such as INR.
        String currencyCode,

        // Delivery address captured for the order.
        // Converted into its response DTO.
        ShippingAddressResponse shippingAddress,

        // Wallet used for payment, when available.
        Long paymentWalletId,

        // Wallet transaction ID, when available.
        Long paymentTransactionId,

        // Time at which the order was created.
        Instant createdAt,

        // All purchased items represented as response DTOs.
        List<OrderItemResponse> items

) {

    /**
     * Converts a CommerceOrder entity into an OrderResponse.
     *
     * Nested objects are converted into their corresponding DTOs
     * rather than exposing JPA entities to the API.
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

                // Convert every OrderItem entity into an OrderItemResponse.
                // toList() creates the resulting list.
                order.getItems().stream()
                        .map(OrderItemResponse::from)
                        .toList()
        );
    }
}
