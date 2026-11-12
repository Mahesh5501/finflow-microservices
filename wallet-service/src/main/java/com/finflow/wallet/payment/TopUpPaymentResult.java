package com.finflow.wallet.payment;

/**
 * Result from an external (or stub) payment capture before wallet credit.
 */
public record TopUpPaymentResult(
        boolean approved,
        String providerReference,
        String failureCode
) {
    public static TopUpPaymentResult approved(String providerReference) {
        return new TopUpPaymentResult(true, providerReference, null);
    }

    public static TopUpPaymentResult declined(String failureCode) {
        return new TopUpPaymentResult(false, null, failureCode);
    }
}
