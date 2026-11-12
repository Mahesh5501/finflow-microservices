package com.finflow.commerce.order.dto;

import java.util.List;

/**
 * Result of marketplace cart checkout — one {@link OrderResponse} per seller that had lines
 * in the customer's cart when checkout started.
 */
public record CheckoutResponse(List<OrderResponse> orders) {
}
