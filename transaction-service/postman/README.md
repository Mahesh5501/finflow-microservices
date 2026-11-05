# Postman — Transaction Service

Port: **18085** · env: `transactionBaseUrl`

## Role

Ledger + money orchestration over wallet balances (deposit / withdraw / transfer saga).
Wallet still owns materialised balance; this service posts balanced ledger entries + status history.

## Import

1. `postman/FinFlow-Local.postman_environment.json` → **FinFlow Local**
2. Identity + Wallet (create wallets first)
3. `transaction-service/postman/FinFlow-Transaction-Service.postman_collection.json`

## Order

1. Identity → org → login again (`wallets:*`, `transfers:create`)
2. Wallet: Create A (+ B for transfer)
3. Transaction: Deposit → history / ledger-balance → Transfer → Withdraw
