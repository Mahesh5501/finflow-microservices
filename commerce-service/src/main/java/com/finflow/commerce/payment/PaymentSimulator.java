package com.finflow.commerce.payment;

/**
 * Deprecated legacy class.
 * Payment simulation has moved to SimulatedPaymentAdapter. Real wallet communication is handled by WalletPaymentAdapter.
 * This class is intentionally kept as a no-op compatibility type so older source references or notes can still compile if they refer to PaymentSimulator.
 * New code must use PaymentPort and its active implementation instead.
 */
@Deprecated(forRemoval = true)
public final class PaymentSimulator {

    /**
     * Private constructor prevents creating instances of this class.
     * There are no instance methods or state to use. The class exists only as a deprecated compatibility marker.
     */
    private PaymentSimulator() {
    }
}