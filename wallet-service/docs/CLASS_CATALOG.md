# Wallet E2E — status

Interview deep-dive: see [`INTERVIEW_GUIDE.md`](./INTERVIEW_GUIDE.md).

## Done (solid wallet core)

| API | Purpose |
|---|---|
| POST `/api/v1/wallets` | Create |
| GET `/api/v1/wallets/{walletId}` | Get by id |
| GET `/api/v1/users/{userId}/wallet` | Get by user |
| POST `.../deposits` | Deposit + idempotency |
| POST `.../withdrawals` | Withdraw + idempotency |
| GET `.../transactions` | History |
| POST `/api/v1/transfers` | Transfer A→B |
| GET `/api/v1/transfers/{transferId}` | Get transfer |
| POST `.../block` `/unblock` `/close` | Status |

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
