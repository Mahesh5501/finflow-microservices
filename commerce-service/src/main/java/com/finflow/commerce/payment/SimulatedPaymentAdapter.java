package com.finflow.commerce.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-process sandbox — no HTTP. Default for unit tests and local demo without wallet running.
 * Interview: swap {@code payment.provider=wallet} to hit the real money service.
 */
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "simulated", matchIfMissing = true)
public class SimulatedPaymentAdapter implements PaymentPort {

    @Override
    public PaymentResult charge(PaymentChargeRequest request) {
        return PaymentResult.simulated(request.sandboxHint());
    }

    @Override
    public PaymentResult refund(PaymentRefundRequest request) {
        return PaymentResult.approved(request.walletId(), null);
    }
}
