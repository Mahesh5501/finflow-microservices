# Wallet E2E — status

Interview deep-dive: see [`INTERVIEW_GUIDE.md`](./INTERVIEW_GUIDE.md).

## Done (solid wallet core)

| API | Purpose |
|---|---|
| `/api/v1/customer/wallet/**` | Shopper API (any app) |
| GET `/api/v1/users/{userId}/wallet` | Integration lookup |
| GET `/api/v1/wallets/{walletId}` | Integration read |
| POST `.../deposits` / `.../withdrawals` | Commerce refund / checkout debit |
| GET `.../transactions` | History by wallet id |

Tables: `wallet`, `wallet_history`, `wallet_transfer`

## Later (not needed for wallet E2E now)

| Item | Why later |
|---|---|
| Auth / JWT | Identity service |
| History pagination | When volume grows |
| Refunds / holds | Product feature |
| FX transfer | Different currencies |
| Kafka events | Integration layer |

## DTOs in use

`WalletRequest`, `WalletResponse`, `MoneyRequest`, `TransferRequest`, `TransferResponse`, `TransactionResponse`
