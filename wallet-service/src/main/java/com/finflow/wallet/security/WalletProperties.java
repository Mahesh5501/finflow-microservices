package com.finflow.wallet.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds {@code wallet.*} service settings from configuration only (no defaults in code).
 */
@Validated
@ConfigurationProperties(prefix = "wallet")
public class WalletProperties {

    @NotBlank
    private String defaultCurrency;

    @NotBlank
    private String topupProvider;

    private final Security security = new Security();

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public String getTopupProvider() {
        return topupProvider;
    }

    public void setTopupProvider(String topupProvider) {
        this.topupProvider = topupProvider;
    }

    public Security getSecurity() {
        return security;
    }

    @Validated
    public static class Security {

        @NotBlank
        private String readPermission;

        @NotBlank
        private String writePermission;

        public String getReadPermission() {
            return readPermission;
        }

        public void setReadPermission(String readPermission) {
            this.readPermission = readPermission;
        }

        public String getWritePermission() {
            return writePermission;
        }

        public void setWritePermission(String writePermission) {
            this.writePermission = writePermission;
        }
    }
}
