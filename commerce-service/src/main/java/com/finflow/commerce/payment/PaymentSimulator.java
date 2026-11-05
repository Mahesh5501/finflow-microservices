package com.finflow.commerce.payment;

/**
 * @deprecated Replaced by {@link SimulatedPaymentAdapter} / {@link WalletPaymentAdapter}.
 * Kept as a no-op type so older notes still compile if referenced.
 */
@Deprecated(forRemoval = true)
public final class PaymentSimulator {

    private PaymentSimulator() {
    }
}
