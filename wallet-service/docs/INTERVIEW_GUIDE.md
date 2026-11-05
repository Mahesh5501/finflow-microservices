# Wallet Service — Interview / Learning Guide

## 1. Tech stack (what we use and why)

| Layer | Choice | Why interview-worthy |
|---|---|---|
| Language | Java 21 | LTS, records, modern Spring Boot |
| Framework | Spring Boot 3.5 | Auto-config, DI, production default |
| API | Spring Web (REST) | Thin controllers, JSON DTOs |
| Validation | Bean Validation (`@Valid`, `@NotNull`) | Fail fast at the edge |
| Persistence | Spring Data JPA + Hibernate | Mapping + repositories |
| DB (local) | MySQL 8 | Real relational money store |
| DB (tests) | H2 in-memory | Fast CI without Docker |
| Money | `BigDecimal` scale 2 | Never `double` for currency |
| Concurrency | `@Version` optimistic lock | Safe concurrent updates |
| Ops | Actuator `/health` | Liveness for orchestrators |

Architecture style: **layered** — Controller → Service → Repository → Entity.  
Wallet is the **balance authority** (single source of truth for balances).

## 2. Patterns applied

| Pattern | Where | Interview line |
|---|---|---|
| Aggregate root | `Wallet` | Invariants (`credit`/`debit`) live on the entity |
| DTO vs Entity | `*Request` / `*Response` | Never expose JPA entities on the wire |
| Idempotency | `Idempotency-Key` header | Safe client retries after network timeout |
| Optimistic locking | `wallet.version` | Detect lost updates without DB row locks |
| Unique constraints | user_id, idempotency_key | DB is last line of defence under races |
| Central error mapping | `GlobalExceptionHandler` | Stable `{code, message}` for clients |
| Transaction boundary | `@Transactional` on service | Deposit/transfer = one atomic unit |
| Soft domain status | ACTIVE / BLOCKED / CLOSED | Risk hold without deleting money |

## 3. Failure cases we handle

| Case | HTTP | Code | How |
|---|---|---|---|
| Bad input (amount, currency) | 400 | `VALIDATION_FAILED` / `BAD_REQUEST` | Bean Validation + service checks |
| Missing Idempotency-Key | 400 | `BAD_REQUEST` | Required header on money APIs |
| Wallet not found | 404 | `WALLET_NOT_FOUND` | |
| Transfer not found | 404 | `TRANSFER_NOT_FOUND` | |
| Duplicate wallet (same user) | 409 | `WALLET_ALREADY_EXISTS` | Pre-check + unique index race catch |
| Blocked/closed money move | 409 | `WALLET_NOT_ACTIVE` | Domain guard on `credit`/`debit` |
| Concurrent balance update | 409 | `CONCURRENT_UPDATE` | `@Version` conflict |
| Idempotency race (two same keys) | 200 replay | — | Unique key + rollback + return winner |
| Insufficient funds | 422 | `INSUFFICIENT_BALANCE` | Domain debit check |
| Transfer different currency | 422 | `CURRENCY_MISMATCH` | |

## 4. Database design & optimisations

| Technique | Detail |
|---|---|
| 3 tables only | `wallet`, `wallet_history`, `wallet_transfer` |
| Unique `user_id` | One wallet per user (lookup index too) |
| Unique `idempotency_key` | Fast retry lookup; prevents double money |
| Materialised `balance` | O(1) read — no SUM over history each GET |
| `balance_after` on history | Statement without replaying all lines |
| Separate `wallet_transfer` | Clear from→to + one key for whole P2P move |
| `readOnly=true` on GETs | Hint for flush mode / replica routing later |
| `saveAndFlush` on idempotent writes | Force unique violation inside the TX |

Indexes that matter: PK, `uk_wallet_user_id`, `uk_wallet_history_idempotency_key`, `uk_wallet_transfer_idempotency_key`, history by `wallet_id` (add composite `(wallet_id, created_at)` when volume grows).

## 5. Will it survive heavy traffic?

**Honest answer for interviews:** this design is **correct for learning / moderate load**, not a billion-TPS wallet.

| Strength | Limit under heavy traffic |
|---|---|
| Optimistic lock avoids long row locks | Hot wallets → many 409s; need client retry or queue |
| Idempotency stops double-spend on retries | Key store grows; need TTL/partition later |
| Single MySQL node | Vertical limit; need read replicas, sharding later |
| In-process transactions | Fine for one DB; cross-service transfer needs saga |
| No auth/rate-limit | Must sit behind Gateway + Identity in prod |
| Full history list unbounded | Need pagination / cursor |

**Production next steps (say in interview):** pagination, connection pool tuning, Redis for idempotency cache, outbox+Kafka for events, row-level lock or ledger-first for ultra-hot accounts, authn/z.

## 6. End-to-end money invariants (say out loud)

1. Deposit increases one wallet; history type `DEPOSIT`.
2. Withdraw decreases only if balance enough; history `WITHDRAW`.
3. Transfer: debit A + credit B **in one DB transaction**; history `SENT` + `RECEIVED`; parent row in `wallet_transfer`.
4. Same Idempotency-Key → same result, **no second money move**.
5. Sum of wallets after withdraw/deposit changes; transfers **preserve** total.
