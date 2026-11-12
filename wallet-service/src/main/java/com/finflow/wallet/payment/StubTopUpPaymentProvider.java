package com.finflow.wallet.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Local/test payment gateway: approves all top-ups and returns a synthetic PG reference.
 * Replace with a real adapter when integrating Razorpay/Stripe (property {@code wallet.topup.provider=stub|razorpay}).
 */
@Component
@ConditionalOnProperty(name = "wallet.topup.provider", havingValue = "stub")
public class StubTopUpPaymentProvider implements TopUpPaymentProvider {

    @Override
    public TopUpPaymentResult capture(
            Long userId,
            BigDecimal amount,
            String currency,
            TopUpPaymentMethod method,
            String clientPaymentReference
    ) {
        if (clientPaymentReference == null || clientPaymentReference.isBlank()) {
            return TopUpPaymentResult.declined("PAYMENT_REFERENCE_REQUIRED");
        }
        String pgRef = "stub-" + method.name().toLowerCase() + "-" + clientPaymentReference.trim();
        return TopUpPaymentResult.approved(pgRef);
    }
}
