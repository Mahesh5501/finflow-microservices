package com.finflow.commerce.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Simulated payment implementation.
 * This adapter does not make any HTTP calls. It simply returns the outcome requested through the sandbox hint.
 * It is useful for: Local development. Automated tests. Demonstrating checkout without running Wallet Service.
 * Spring activates this implementation when: payment.provider=simulated
 * It is also activated when payment.provider is not configured, because matchIfMissing=true.
 */
@Component
@ConditionalOnProperty(
        name = "payment.provider",
        havingValue = "simulated",
        matchIfMissing = true
)
public class SimulatedPaymentAdapter implements PaymentPort {

    /**
     * Simulates a wallet charge.
     * @param request the PaymentChargeRequest containing the customer, amount, currency, idempotency key, access token and optional sandbox outcome.
     * @return a PaymentResult containing the payment result.
     */ 
    @Override
    public PaymentResult charge(PaymentChargeRequest request) {
        // The sandboxHint controls whether the simulated payment is: APPROVED, DECLINED or UNCERTAIN. No actual money is deducted.
        return PaymentResult.simulated(request.sandboxHint());
    }

    /**
     * Simulates a successful refund. No actual wallet deposit happens. We return the wallet ID from the request when available. The transaction ID is null because no real wallet transaction exists.
     * @param request the PaymentRefundRequest containing the customer, amount, currency, idempotency key, access token and optional sandbox outcome.
     * @return a PaymentResult containing the payment result.
     */
    @Override
    public PaymentResult refund(PaymentRefundRequest request) { return PaymentResult.approved(request.walletId(), null); }
}