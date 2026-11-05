package com.finflow.commerce.payment;

/** Simulated PSP result — maps to distinct order statuses. */
public enum PaymentOutcome {
    APPROVED,
    DECLINED,
    UNCERTAIN
}
