package com.finflow.commerce.payment;

/**
 * Normalized PSP/wallet outcome for order state machine.
 * {@code walletId}/{@code walletTransactionId} are set only when money actually moved.
 */
public record PaymentResult(
        PaymentOutcome outcome,
        Long walletId,
        Long walletTransactionId,
        String failureCode
) {
    public static PaymentResult approved(Long walletId, Long walletTransactionId) {
        return new PaymentResult(PaymentOutcome.APPROVED, walletId, walletTransactionId, null);
    }

    public static PaymentResult declined(String failureCode) {
        return new PaymentResult(PaymentOutcome.DECLINED, null, null, failureCode);
    }

    public static PaymentResult uncertain(String failureCode) {
        return new PaymentResult(PaymentOutcome.UNCERTAIN, null, null, failureCode);
    }

    public static PaymentResult simulated(PaymentOutcome outcome) {
        PaymentOutcome resolved = outcome == null ? PaymentOutcome.APPROVED : outcome;
        return new PaymentResult(resolved, null, null, null);
    }
}
