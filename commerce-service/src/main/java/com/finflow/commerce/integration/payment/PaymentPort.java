package com.finflow.commerce.integration.payment;

/**
 * Defines the payment operations required by Commerce Service.
 * This is an abstraction (contract), not the actual payment implementation.
 * Commerce Service does not directly access wallet-service database tables. Instead, it calls this interface, and Spring injects the configured implementation.
 * Possible implementations: 1. WalletPaymentAdapter -> calls the actual wallet-service over HTTP. 2. SimulatedPaymentAdapter -> returns simulated payment outcomes.
 */
public interface PaymentPort {

    /**
     * Charges the customer's wallet for an order.
     * The request contains the customer, amount, currency, idempotency key, access token and optional sandbox outcome.
     * Returns a normalized PaymentResult so the calling service does not need to understand the wallet-service's HTTP response format.
     * @param request the PaymentChargeRequest containing the customer, amount, currency, idempotency key, access token and optional sandbox outcome.
     * @return the PaymentResult containing the payment result.
     */
    PaymentResult charge(PaymentChargeRequest request);

    /**
     * Refunds money to the customer's wallet.
     * This is typically called when a previously paid order is cancelled or another business operation requires a refund.
     * It returns PaymentResult because a refund can also be approved, declined or have an uncertain outcome.
     * @param request the PaymentRefundRequest containing the customer, amount, currency, idempotency key, access token and optional sandbox outcome.
     * @return the PaymentResult containing the payment result.
     */
    PaymentResult refund(PaymentRefundRequest request);
}