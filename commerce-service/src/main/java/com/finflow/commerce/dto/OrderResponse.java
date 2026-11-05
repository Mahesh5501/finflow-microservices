package com.finflow.commerce.dto;

import com.finflow.commerce.entity.CommerceOrder;
import com.finflow.commerce.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String orderNumber,
        Long organisationId,
        Long customerUserId,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        String couponCode,
        String currencyCode,
        ShippingAddressResponse shippingAddress,
        Long paymentWalletId,
        Long paymentTransactionId,
        Instant createdAt,
        List<OrderItemResponse> items
) {
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
                order.getItems().stream().map(OrderItemResponse::from).toList()
        );
    }
}
