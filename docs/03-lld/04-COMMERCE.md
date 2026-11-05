# 4. Commerce — Complete Design

**Capability:** Products, stock, orders, simulated payment, fulfilment handoff  
**Process:** `commerce-service` · Port `8085`  
**Database:** `commerce_db`  
**Invariants:** Price snapshot · Conditional stock · Order state machine · Uncertain payment ≠ declined

---

## 1. Position

```mermaid
flowchart LR
    GW[API Gateway] --> COM[Commerce]
    COM --> CDB[(commerce_db)]
    COM -.-> PAY[Payment Sandbox]
    COM -.-> OUT[Outbox]
    OUT -.-> KAFKA[Kafka]
    KAFKA -.-> NOTIF[Notification]
    KAFKA -.-> SHIP[Shipping Connector]
    COM -.->|optional simulated payment| FF[FinFlow]
```

Commerce owns catalogue, inventory, and orders. It does **not** write FinFlow tables. Payment is a simulation or an explicit FinFlow API call.

---

## 2. Internal architecture

```mermaid
flowchart TB
    API[API · Orders · Products]
    APP[CreateOrder · CancelOrder · ApplyPaymentResult]
    DOMAIN[Order · OrderItem · Product · Inventory · OrderStatus]
    REPO[Repositories]
    DB[(commerce_db)]
    PORTS[PaymentPort · EventPublisher]

    API --> APP --> DOMAIN
    APP --> REPO --> DB
    APP --> PORTS
```

```text
com.finflow.commerce
  api
  application
  domain
  infrastructure.persistence
  infrastructure.payment
  infrastructure.messaging
```

---

## 3. Data model

```mermaid
erDiagram
    PRODUCTS ||--o{ ORDER_ITEMS : referenced
    ORDERS ||--|{ ORDER_ITEMS : contains
    ORDERS ||--o{ ORDER_STATUS_HISTORY : tracks
    ORDERS ||--o| IDEMPOTENCY_KEYS : protected_by
    ORDERS ||--o| OUTBOX_EVENTS : emits

    PRODUCTS {
        bigint product_id PK
        bigint organisation_id
        varchar name
        decimal unit_price
        int available_quantity
        bigint version
        varchar status
    }
    ORDERS {
        bigint order_id PK
        bigint organisation_id
        bigint customer_user_id
        varchar order_number UK
        varchar status
        decimal total_amount
        char currency_code
        datetime created_at
    }
    ORDER_ITEMS {
        bigint order_item_id PK
        bigint order_id FK
        bigint product_id
        varchar product_name_snapshot
        decimal unit_price_snapshot
        int quantity
    }
    ORDER_STATUS_HISTORY {
        bigint id PK
        bigint order_id FK
        varchar from_status
        varchar to_status
        datetime changed_at
    }
    IDEMPOTENCY_KEYS {
        varchar key PK
        bigint organisation_id
        bigint order_id
        varchar request_hash
    }
    OUTBOX_EVENTS {
        bigint id PK
        varchar event_id UK
        varchar event_type
        text payload
        varchar status
    }
```

Stock update must be conditional:

```sql
UPDATE products
SET available_quantity = available_quantity - :qty, version = version + 1
WHERE product_id = :id
  AND available_quantity >= :qty
  AND version = :expectedVersion;
```

---

## 4. Order state machine

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> PENDING_PAYMENT: Begin payment simulation
    PENDING_PAYMENT --> CONFIRMED: Approved
    PENDING_PAYMENT --> PAYMENT_FAILED: Declined
    PENDING_PAYMENT --> ACTION_REQUIRED: Timeout / uncertain
    ACTION_REQUIRED --> CONFIRMED: Safe recovery / provider confirmed
    ACTION_REQUIRED --> PAYMENT_FAILED: Confirmed decline
    ACTION_REQUIRED --> CANCELLED: Policy cancel
    CONFIRMED --> FULFILMENT_PENDING: Eligible
    FULFILMENT_PENDING --> FULFILLED: Shipping confirmed
    CREATED --> CANCELLED: Valid cancel
    PENDING_PAYMENT --> CANCELLED: Valid cancel
    CONFIRMED --> CANCELLED: Policy allows
    FULFILLED --> [*]
    PAYMENT_FAILED --> [*]
    CANCELLED --> [*]
```

**Never use a single `FAILED` for declined, timeout, and cancelled.**

---

## 5. APIs

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/products` | Create / manage product |
| GET | `/api/v1/products` | List products |
| POST | `/api/v1/orders` | Create order (`Idempotency-Key`) |
| GET | `/api/v1/orders/{orderId}` | Get order |
| POST | `/api/v1/orders/{orderId}/cancel` | Cancel if allowed |

**Create order**

```json
{
  "organisationId": 1,
  "items": [
    { "productId": 100, "quantity": 2 }
  ]
}
```

Totals and prices come from **authoritative product data**, not client-supplied prices.

---

## 6. Create order flow

```mermaid
sequenceDiagram
    actor Customer
    participant GW as Gateway
    participant COM as Commerce
    participant DB as commerce_db
    participant PAY as Payment Sandbox
    participant Kafka
    participant NOTIF as Notification
    participant SHIP as Shipping

    Customer->>GW: POST /orders + Idempotency-Key
    GW->>COM: Auth context
    COM->>COM: Validate items / org / permission
    COM->>DB: Check idempotency
    alt Duplicate
        COM-->>Customer: Existing order
    else New
        COM->>DB: TX: reserve stock + save order + outbox
        COM-->>Customer: Order accepted
        COM->>PAY: Simulate payment
        PAY-->>COM: Approved / Declined / Uncertain
        COM->>DB: Transition order status + follow-up event
        Kafka->>NOTIF: Notify
        Kafka->>SHIP: Fulfil when CONFIRMED
    end
```

---

## 7. Failure matrix

| Failure | Behaviour |
|---|---|
| Invalid items / qty | `400` / `422` — no stock change |
| Insufficient stock | Reject — no order |
| Duplicate idempotency key | Return original order |
| Payment declined | `PAYMENT_FAILED` — release reserved stock |
| Payment timeout | `ACTION_REQUIRED` — reconcile; do not invent decline |
| Notification fails | Order stays confirmed — retry notify |
| Shipping fails | Fulfilment retry / DLQ — order remains confirmed until policy says otherwise |

---

## 8. Use cases Commerce must satisfy

| ID | Use case |
|---|---|
| C1 | Create product with price and stock |
| C2 | Create order with price snapshots |
| C3 | Concurrent last-item stock — only one wins |
| C4 | Idempotent order create |
| C5 | Payment approved → CONFIRMED |
| C6 | Payment declined → PAYMENT_FAILED + stock release |
| C7 | Payment uncertain → ACTION_REQUIRED |
| C8 | Cancel only when policy allows |
| C9 | Cross-org access denied |
| C10 | Event published after commit only |

---

## 9. Introduction phase

Commerce is **Phase C** after Identity + FinFlow foundation. No commerce tables in FinFlow DB. No FinFlow tables in Commerce DB.
