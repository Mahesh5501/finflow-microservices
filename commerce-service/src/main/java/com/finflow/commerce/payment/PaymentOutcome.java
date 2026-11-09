package com.finflow.commerce.payment;

/**
 * Represents the outcome returned by a payment provider.
 *
 * This enum is shared by the real wallet adapter and the simulated adapter.
 * Commerce Service uses it to decide what should happen to the order.
 */
public enum PaymentOutcome {

    /**
     * The payment provider confirmed that the payment succeeded.
     *
     * Commerce can proceed with confirming the order,
     * subject to its own transaction and consistency checks.
     */
    APPROVED,

    /**
     * The payment provider explicitly rejected the payment.
     *
     * Example:
     * - Insufficient wallet balance.
     * - Invalid payment request.
     *
     * This is a known failure, not an unknown result.
     */
    DECLINED,

    /**
     * Commerce Service cannot determine whether the payment succeeded.
     *
     * Example:
     * 1. Commerce sends a payment request.
     * 2. Wallet Service processes the payment.
     * 3. The network connection times out before Commerce receives the response.
     *
     * The customer might already have been charged.
     * Therefore, Commerce must not automatically treat this as a decline.
     */
    UNCERTAIN
}