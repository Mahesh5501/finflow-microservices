# 3. FinFlow — Complete Design

**Capability:** Simulated wallets, transfers, ledger, transaction history  
**Processes (current repo mapping):** `wallet-service` (:18082) — wallets, transfers, balance history  
**Logical ownership:** One FinFlow domain — one `finflow_db` (or schemas under FinFlow ownership)  
**Invariants:** Decimal money · Ledger nets to zero · Idempotent retries · No silent ledger edits

---

## 1. Position

```mermaid
flowchart LR
    GW[API Gateway] --> FF[FinFlow Domain]
    FF --> DB[(FinFlow Data)]
    FF -.-> OUT[Outbox]
    OUT -.-> KAFKA[Kafka]
    KAFKA -.-> NOTIF[Notification]
```

FinFlow owns financial state. Commerce never updates FinFlow tables. Payment simulation may call FinFlow or stay in Commerce — contract is explicit either way.

---

## 2. Internal architecture

```mermaid
flowchart TB
    API[API Layer\nControllers · DTOs · Validation]
    APP[Application Layer\nCreateWallet · Deposit · Withdraw · Transfer · GetHistory]
    DOMAIN[Domain\nWallet · Transfer · LedgerEntry · Status]
    REPO[Repositories]
    DB[(finflow_db)]
    OUTBOX[Outbox Publisher]
    CLIENT[Wallet internal ops\nif split across processes]

    API --> APP
    APP --> DOMAIN
    APP --> REPO --> DB
    APP --> OUTBOX
    APP -.-> CLIENT
```

```text
com.finflow.wallet / com.finflow.transaction
  api
  application
  domain
  infrastructure.persistence
  infrastructure.messaging
```

Preferred: **one FinFlow application** (`wallet-service`) owning wallet + transfer + ledger in one local transaction.  
A separate orchestrator process is optional interview depth — not required for the platform to work.

---

## 3. Data model

```mermaid
erDiagram
    WALLETS ||--o{ LEDGER_ENTRIES : has
    TRANSFERS ||--o{ LEDGER_ENTRIES : posts
    TRANSFERS ||--o{ TRANSFER_STATUS_HISTORY : tracks
    TRANSFERS ||--o| IDEMPOTENCY_KEYS : protected_by
    TRANSFERS ||--o| OUTBOX_EVENTS : emits

    WALLETS {
        bigint wallet_id PK
        bigint user_id
        bigint organisation_id
        char currency_code
        decimal balance
        varchar status
        bigint version
        datetime created_at
    }
    TRANSFERS {
        bigint transfer_id PK
        varchar idempotency_key
        bigint organisation_id
        bigint source_wallet_id
        bigint destination_wallet_id
        decimal amount
        char currency_code
        varchar type
        varchar status
        datetime created_at
    }
    LEDGER_ENTRIES {
        bigint entry_id PK
        bigint transfer_id FK
        bigint wallet_id FK
        decimal signed_amount
        varchar entry_type
        datetime posted_at
    }
    TRANSFER_STATUS_HISTORY {
        bigint id PK
        bigint transfer_id FK
        varchar from_status
        varchar to_status
        datetime changed_at
    }
    IDEMPOTENCY_KEYS {
        varchar key PK
        bigint organisation_id
        bigint transfer_id
        varchar request_hash
        datetime created_at
    }
    OUTBOX_EVENTS {
        bigint id PK
        varchar event_id UK
        varchar event_type
        text payload
        varchar status
        datetime created_at
    }
```

**Unique:** `(user_id, currency_code)` on wallets · `(organisation_id, idempotency_key)` on transfers  
**Money:** `DECIMAL(19,2)` — never float  
**Ledger rule:** For a transfer, debit + credit signed amounts = `0`  
**Balance:** Materialised on wallet; must reconcile to sum of ledger entries

`type`: `DEPOSIT` | `WITHDRAWAL` | `TRANSFER`  
`status`: `PENDING` | `SUCCESS` | `FAILED` | `REVERSED`

---

## 4. APIs

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/wallets` | Create wallet |
| GET | `/api/v1/wallets/{walletId}` | Get wallet |
| GET | `/api/v1/users/{userId}/wallet` | Get by user |
| POST | `/api/v1/transfers` | Transfer (header: `Idempotency-Key`) |
| POST | `/api/v1/transactions/deposits` | Deposit |
| POST | `/api/v1/transactions/withdrawals` | Withdraw |
| GET | `/api/v1/transactions/{id}` | Get transaction |
| GET | `/api/v1/transactions?walletId=` | History |

**Create wallet**

```json
{ "currency": "INR" }
```

**Transfer**

```json
{
  "sourceWalletId": 10,
  "destinationWalletId": 22,
  "amount": "150.00"
}
```

**Response**

```json
{
  "transferId": 9,
  "status": "SUCCESS",
  "amount": "150.00",
  "currency": "INR"
}
```

---

## 5. Transfer flow (happy path)

```mermaid
sequenceDiagram
    actor Client
    participant GW as Gateway
    participant FF as FinFlow
    participant DB as finflow_db
    participant Pub as Outbox Publisher
    participant Kafka

    Client->>GW: POST /transfers + Idempotency-Key
    GW->>FF: Authenticated request
    FF->>DB: Find idempotency key
    alt Duplicate
        FF-->>Client: Existing transfer result
    else New
        FF->>DB: Begin TX
        FF->>DB: Check balance + version
        FF->>DB: Ledger debit + credit
        FF->>DB: Update balances
        FF->>DB: Transfer SUCCESS + outbox
        FF->>DB: Commit
        FF-->>Client: SUCCESS
        Pub->>DB: Read outbox
        Pub->>Kafka: TransferCompleted
    end
```

---

## 6. State machine

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> SUCCESS: Debit+credit posted
    PENDING --> FAILED: Insufficient balance / validation
    PENDING --> REVERSED: Compensating credit after partial failure
    SUCCESS --> [*]
    FAILED --> [*]
    REVERSED --> [*]
```

---

## 7. Failure and recovery

| Failure | Handling |
|---|---|
| Duplicate request | Return stored result via idempotency key |
| Concurrent update | Optimistic `@Version` → `409` → client retries same key |
| Insufficient balance | No ledger post · status `FAILED` |
| Timeout after commit | Client retries same key · no second money move |
| Split-process credit fails after debit | Compensating credit to sender · status `REVERSED` |
| Duplicate Kafka event | Consumer dedupe by `event_id` |
| Wrong ledger entry | New reversing entry — never edit posted row |

---

## 8. Concurrency

```sql
UPDATE wallets
SET balance = balance - :amount, version = version + 1
WHERE wallet_id = :id
  AND balance >= :amount
  AND version = :expectedVersion;
```

Zero rows → reject (insufficient funds or stale version).

---

## 9. Use cases FinFlow must satisfy

| ID | Use case |
|---|---|
| F1 | Create wallet balance 0 |
| F2 | Deposit increases balance + ledger credit |
| F3 | Withdraw decreases only if enough |
| F4 | Transfer moves money with balanced ledger |
| F5 | Same idempotency key → one effect |
| F6 | Owner-only read |
| F7 | History readable |
| F8 | Reconciliation: balance = sum(ledger) |
| F9 | Event published only after commit |

---

## 10. Repo today vs target

| Today (`wallet-service`) | Target |
|---|---|
| Create / get / deposit | Keep and extend |
| Balance column only | Add ledger + transfer |
| No idempotency | Add keys |
| No org | Add organisation scope |
| Direct public deposit | Prefer deposit via transaction/transfer API |

Do not rewrite the existing wallet; evolve it into this model.
