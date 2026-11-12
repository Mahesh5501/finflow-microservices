package com.finflow.wallet.payment;

import java.math.BigDecimal;

/**
 * Abstraction over card/UPI/net-banking capture. Production would call Razorpay/Stripe/etc.
 */
public interface TopUpPaymentProvider {

    /**
     * Authorize/capture customer funds. {@code clientPaymentReference} is the app's idempotency
     * handle for the PG session (distinct from wallet Idempotency-Key header when needed).
     */
    TopUpPaymentResult capture(
            Long userId,
            BigDecimal amount,
            String currency,
            TopUpPaymentMethod method,
            String clientPaymentReference
    );
}
