package com.finflow.wallet.payment;

/**
 * How the customer funded the wallet top-up (maps to PG rails in production).
 */
public enum TopUpPaymentMethod {
    CARD,
    UPI,
    NET_BANKING
}
