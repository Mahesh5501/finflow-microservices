# Wallet service — package layout

Feature-first layout aligned with **commerce-service** and **identity-service**.

## Modules

| Module | Package | Responsibility |
|--------|---------|----------------|
| **wallet** | `com.finflow.wallet.wallet` | Balances, top-up, withdrawals, deposits, history |
| **payment** | `com.finflow.wallet.payment` | Top-up provider port + stub (like commerce `payment`) |
| **security** | `com.finflow.wallet.security` | JWT/JWKS, `WalletProperties` |
| **exception** | `com.finflow.wallet.exception` | API errors |

## Wallet feature layers

```
wallet/
  api/
    customer/     → CustomerWalletController  (/api/v1/customer/wallet/**)
    integration/  → IntegrationWalletController (commerce /api/v1/users|wallets/**)
  application/    → WalletService
  domain/         → Wallet, WalletHistory, WalletTransfer, enums
  persistence/    → repositories
  dto/            → API records
```

## Conventions

- **customer** vs **integration** API split mirrors commerce **customer** vs **merchant** adapters.
- **`WalletProperties`** lives under **security** (same idea as `CommerceSecurityProperties`).
- HTTP paths unchanged; only packages and controller split changed.
