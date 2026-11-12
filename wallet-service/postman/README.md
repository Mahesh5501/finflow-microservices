# Wallet Service — Postman

Import `FinFlow-Wallet-Service.postman_collection.json` and environment **`FinFlow Local`** from `postman/FinFlow-Local.postman_environment.json`.

## Collection layout (matches code)

| Folder | Controller | Base path |
|--------|------------|-----------|
| **Customer** | `CustomerWalletController` | `/api/v1/customer/wallet/**` |
| **Integration** | `IntegrationWalletController` | `/api/v1/users/{id}/wallet`, `/api/v1/wallets/{id}/**` |

Shopper flows (open wallet, top-up, my transactions) live under **Customer**. Commerce checkout (withdraw) and refunds (deposit) live under **Integration**.

## Prerequisites

1. Identity: Register → Verify email → Login  
2. JWT should include `wallet:read` and `wallet:write` (verified consumer — see `docs/PERMISSIONS.md`)  
3. Run **Customer → Open wallet** so `{{walletId}}` is set for integration calls  

## Idempotency

Top-up, deposit, and withdraw require `Idempotency-Key` (collection vars: `topUpKey`, `depositKey`, `withdrawKey`).

## Regenerate

```bash
cd postman && python _gen_collections.py
```
