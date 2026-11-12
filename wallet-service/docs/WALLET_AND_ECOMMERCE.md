# Wallet + e-commerce (production-oriented design)

## Role in FinFlow

| Capability | Owner | Notes |
|------------|--------|--------|
| Customer stored balance | **wallet-service** | One wallet per user, INR (etc.) |
| Pay at checkout | **commerce-service** | Debits wallet via withdrawal API + customer JWT |
| Refund on cancel/return | **commerce-service** | Credits wallet via deposit API + customer JWT |
| Add money (card/UPI) | **wallet-service** | `POST .../top-ups` after PG capture |
| Seller settlement / escrow | **Not implemented** | Future: ledger + payout service |

This matches **Amazon Pay / Flipkart wallet** at a high level: load balance → shop → auto-debit; refunds return to the same wallet.

## Customer journey (integrated)

1. **Identity**: register → verify email → login (no org required for shopping).
2. JWT includes **platform consumer** permissions from identity config (default `wallet:*`, `commerce:*`).
3. **Wallet**: create INR wallet → **top-up** (PG stub or real adapter).
4. **Commerce**: browse → cart → checkout (`payment.provider=wallet`) → withdrawal.
5. Optional: cancel (refund deposit) or fulfil → return (refund deposit).

## API layers

### Layer A — `CustomerWalletController` (`/api/v1/customer/wallet`)

**Use in any app** (e-commerce, future billing, etc.). JWT identifies the user; no `walletId` in URLs.

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/v1/customer/wallet` | Balance & status |
| POST | `/api/v1/customer/wallet` | Open wallet (201 first time, 200 if exists) |
| POST | `/api/v1/customer/wallet/top-ups` | Add money (auto-creates INR wallet if needed) |
| GET | `/api/v1/customer/wallet/transactions` | History (TOP_UP, WITHDRAW from checkout, DEPOSIT refunds, …) |

**Debit at checkout** does not appear here — Commerce calls integration APIs below with the same user JWT.

### Layer B — `IntegrationWalletController` (commerce / ops)

| API | Audience | Use |
|-----|----------|-----|
| `GET /users/{userId}/wallet` | Commerce | Resolve wallet before charge |
| `POST /wallets/{id}/withdrawals` | Commerce | Checkout debit |
| `POST /wallets/{id}/deposits` | Commerce | Refunds |
| `GET /wallets/{id}`, `.../transactions` | Tools | Optional diagnostics |

P2P **wallet-to-wallet transfers** and **block / unblock / close** are **not HTTP-exposed** (e-commerce wallet product). Wallet status (`ACTIVE` / `BLOCKED` / `CLOSED`) remains in the domain for future admin tooling and unit tests.

### Not needed for a minimal e-commerce wallet UI

- Manual `withdrawals` (pay-at-checkout is server-driven)
- P2P transfers (removed from HTTP API)
- Raw `deposits` from the app (use top-ups; deposits stay for refunds)

## Top-up provider

- Property: `wallet.topup.provider=stub` (default).
- `StubTopUpPaymentProvider` approves when `paymentReference` is present.
- Production: implement `TopUpPaymentProvider` (Razorpay verify signature, etc.) and set `wallet.topup.provider=razorpay`.

## Identity permissions (2026-11)

- **Verified users** receive labels from `identity.authorization.verified-consumer` on the JWT (see `docs/PERMISSIONS.md`).
- Org roles **add** merchant/finance capabilities (`org:manage`, extra `commerce:*`, …).
- Shoppers do **not** need to create a seller org to shop.

## Commerce ↔ wallet money

- Orders may use scale **4**; wallet uses scale **2**.
- `WalletPaymentGateway` rounds **HALF_UP** to 2 decimals on charge/refund.

## Still open for true production

- Service identity for refunds when the caller is not the customer (merchant ops).
- Persist PG reference on `wallet_history` and reconciliation jobs.
- Seller payout / escrow and payment-outbox worker in commerce.
- Replace `ddl-auto=update` with migrations.
- Rate limits and fraud rules on top-up velocity.
