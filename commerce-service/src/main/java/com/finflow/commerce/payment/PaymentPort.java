package com.finflow.commerce.payment;

/**
 * Money side of checkout. Commerce never writes wallet tables —
 * implementations call wallet-service (or a sandbox simulator).
 */
public interface PaymentPort {

    PaymentResult charge(PaymentChargeRequest request);

    PaymentResult refund(PaymentRefundRequest request);
}
