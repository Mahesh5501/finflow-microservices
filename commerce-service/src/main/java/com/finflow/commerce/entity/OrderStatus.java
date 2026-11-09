
package com.finflow.commerce.entity;

import java.util.EnumSet;

/**
 * Represents the lifecycle of a commerce order.
 *
 * Each status defines which next statuses are valid.
 *
 * This prevents application services from moving an order
 * into an invalid state.
 */
public enum OrderStatus {

    CREATED,
    PENDING_PAYMENT,
    CONFIRMED,
    PAYMENT_FAILED,
    ACTION_REQUIRED,
    CANCELLED,
    EXPIRED,
    FULFILMENT_PENDING,
    FULFILLED;

    /**
     * Checks whether this order status can transition to the
     * supplied next status.
     *
     * @param next the proposed next status
     * @return true if the transition is allowed; otherwise false
     */
    public boolean canTransitionTo(OrderStatus next) {

        if (next == null) {
            return false;
        }

        return switch (this) {

            case CREATED ->
                    EnumSet.of(
                            PENDING_PAYMENT,
                            CANCELLED
                    ).contains(next);

            case PENDING_PAYMENT ->
                    EnumSet.of(
                            CONFIRMED,
                            PAYMENT_FAILED,
                            ACTION_REQUIRED,
                            CANCELLED,
                            EXPIRED
                    ).contains(next);

            case ACTION_REQUIRED ->
                    EnumSet.of(
                            CONFIRMED,
                            PAYMENT_FAILED,
                            CANCELLED,
                            EXPIRED
                    ).contains(next);

            case PAYMENT_FAILED ->
                    next == CANCELLED;

            case CONFIRMED ->
                    EnumSet.of(
                            FULFILMENT_PENDING,
                            FULFILLED,
                            CANCELLED
                    ).contains(next);

            case FULFILMENT_PENDING ->
                    next == FULFILLED;

            case FULFILLED, CANCELLED, EXPIRED ->
                    false;
        };
    }
}
