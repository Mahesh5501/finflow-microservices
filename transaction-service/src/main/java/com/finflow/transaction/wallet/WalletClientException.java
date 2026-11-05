package com.finflow.transaction.wallet;

/** Wallet rejected the money move (e.g. insufficient balance). */
public class WalletClientException extends RuntimeException {

    private final String code;

    public WalletClientException(String code, String message) {
        super(message);
        this.code = code == null ? "WALLET_DECLINED" : code;
    }

    public String getCode() {
        return code;
    }
}
