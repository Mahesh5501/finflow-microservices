# Commerce Service — Architecture & Production Runbook

**Service:** `commerce-service` · **Java 21** · **Spring Boot 3.5.6** · **Port 18083**  
**Last updated:** 2026-11-10 (package-by-feature restructure + production hardening)

---

## 1. Directory tree (authoritative)

**Style:** classic Spring **layers** at the top, **feature subpackages** underneath (cart, order, catalogue, coupon, returns).

```
com.finflow.commerce
├── CommerceServiceApplication.java
│
├── controller/                       # REST — two audiences only
│   ├── customer/   browse + cart + orders + return request
│   └── merchant/   catalogue writes, coupons, fulfilment, return approval
│
├── service/
│   ├── catalogue/   CatalogueService, ProductMapper
│   ├── cart/        CartService, AbandonedCartCleanupService
│   ├── order/       CommerceService, OrderPlacementService, PaymentSettlementService, …
│   └── returns/     ReturnService
│
├── entity/                           # JPA + enums (per feature)
│   ├── catalogue/   Product, Category, ProductStatus
│   ├── cart/        Cart, CartItem
│   ├── coupon/      Coupon
│   ├── order/       CommerceOrder, PaymentOutbox, …
│   └── returns/     ReturnRequest, ReturnStatus
│
├── repository/
│   ├── catalogue/   ProductRepository, CategoryRepository
│   ├── cart/        CartRepository
│   ├── coupon/      CouponRepository
│   ├── order/       CommerceOrderRepository, …
│   └── returns/     ReturnRequestRepository
│
├── dto/
│   ├── catalogue/   ProductRequest, ProductResponse, …
│   ├── cart/        CartResponse, CartItemRequest, …
│   ├── coupon/
│   ├── order/
│   └── returns/
│
├── integration/        payment/, event/   (wallet & Kafka adapters)
├── config/             TransactionConfig, ProductionFeaturesConfig (ShedLock when jobs are enabled)
├── health/             dependency health HTTP API
├── exception/          ApiException, ErrorResponse, GlobalExceptionHandler
├── controller/         customer + merchant REST (`@AuthenticationPrincipal AuthenticatedUser`)
└── security/           JWT (`SecurityConfig`, `JwtAuthFilter`, `AuthenticatedUser`, …)
```

**Optional next step (merchant vs customer):** add a level under `controller` only, e.g. `controller/customer/cart/CartController` and `controller/merchant/catalogue/ProductController`, **same** `service` / `entity` / `repository` / `dto` packages. Split controllers when URLs or permissions differ.

| Layer | Package example | Role |
|-------|-----------------|------|
| Controller | `controller.OrderController` | HTTP only |
| Service | `service.order.CommerceService` | `@Transactional` use cases |
| Entity | `entity.order.CommerceOrder` | Persistence model |
| Repository | `repository.order.CommerceOrderRepository` | Spring Data |
| DTO | `dto.order.CreateOrderRequest` | API contracts |

Migration script: `scripts/migrate-to-layered-subpackages.py`.

---

## 2. Domain boundary (unchanged contract)

Commerce owns catalogue, carts, coupons, orders, stock reservation, payment **intent** (outbox), returns, and domain events. Wallet balances and JWT issuance remain external.

**Payment seam:** single `PaymentPort` → `WalletPaymentAdapter` (HTTP + caller JWT) or `SimulatedPaymentAdapter`.

### Marketplace model (Flipkart / Amazon style)

| Role | API contract |
|------|----------------|
| **Customer** | Global catalogue: `GET /api/v1/customer/products/search`, `GET .../products/{id}`. Cart: `{ "productId", "quantity" }` — seller id is read from the product row. Orders: list by **your user id**, not org membership. |
| **Merchant** | Org-scoped writes under `/api/v1/merchant/**` with `organisationId` (seller) on create/update. |

Checkout still creates **one order per seller** (same as one fulfilment batch). Multi-seller cart is merged in `GET /api/v1/customer/carts`; pass `sellerOrganisationId` on checkout when needed.

---

## 3. API map (by feature)

| Area | Base path | Controller |
|------|-----------|------------|
| Catalogue | `/api/v1/products`, `/api/v1/categories` | `catalogue.api.*` |
| Cart | `/api/v1/carts/**` | `cart.api.CartController` |
| Coupons | `/api/v1/coupons` | `coupon.api.CouponController` |
| Orders | `/api/v1/orders/**` | `order.api.OrderController` |
| Returns | `/api/v1/returns/**` | `returns.api.ReturnController` |
| **Dependency health** | `GET /api/v1/health/dependencies` | `health` (public) |

**Note:** The IntegratedSlots monitor path `/IntegratedSlots/healthcheck/monitor` belongs to the **gaming servlet** stack (`integratedslotsv2`), not FinFlow commerce. Commerce exposes **dependency-only** health at `/api/v1/health/dependencies`. JVM/process metrics stay on Actuator (`/actuator/metrics`, `/actuator/prometheus`).

---

## 4. Critical lifecycle (create order)

Unchanged semantics; orchestration now lives under `order.application`:

1. `OrderController` → `CommerceService.createOrder`
2. `OrderPlacementService.placePendingOrder` — single TX: stock (`@Version`), idempotency, outbox `PENDING`
3. `PaymentSettlementService.settleCharge` — wallet HTTP **outside** placement TX
4. `PaymentPort.charge` — Resilience4j **circuit breaker + retry**, RestClient **3s connect / 10s read**
5. Short TX apply outcome → event publish on success

---

## 5. Production hardening checklist

### Resilience

- [x] Resilience4j CB on `WalletPaymentAdapter` (`walletService` instance)
- [x] Resilience4j retry with exponential backoff (`application.properties`)
- [x] RestClient timeouts in `integration.payment.PaymentConfig`
- [ ] Outbox drain worker (rows written; no reconciler yet — see prior audit)

### Consistency

- [x] Idempotency keys on order create (header) and wallet charge/refund keys
- [x] Optimistic locking on product stock (`CONCURRENT_UPDATE` via `GlobalExceptionHandler`)
- [x] Payment settlement outside placement transaction

### Observability

- [ ] `TraceIdFilter` (optional) — `X-Trace-Id` + MDC `traceId`; removed for now
- [x] `ErrorResponse` — `code`, `message`, `timestamp`, `traceId`, optional `details`
- [x] Micrometer OTel bridge on classpath (export when collector configured)
- [x] Log pattern includes `%X{traceId}`

### Health

- [x] `GET /api/v1/health/dependencies` — database, Redis, identity JWKS, wallet (or `SKIPPED` if simulated payment)
- [x] Actuator liveness/readiness probes enabled
- [x] Actuator remains separate from dependency monitor (no heap/GC in `/api/v1/health/dependencies`)

### Security

- [x] Stateless JWT via identity JWKS
- [x] `commerce:read` / `commerce:write` on `/api/v1/**` (except public health)

---

## 6. Local runbook

1. **Java 21:** `source ../finflow-env.sh` then `mvn -version`
2. **Infra:** MySQL `commerce` on `3307`, Redis `6379`, identity `18081`, wallet `18082` when `payment.provider=wallet`
3. **Start:** `mvn spring-boot:run` or IDE `CommerceServiceApplication`
4. **Smoke**
   - `GET http://localhost:18083/api/v1/health/dependencies` → `UP` when deps healthy
   - `GET http://localhost:18083/actuator/health` → includes probe groups
5. **Scheduled jobs (optional):** reservation expiry / cart purge — not wired for now; `ReservationExpiryService` and `AbandonedCartCleanupService` remain callable when you add a scheduler back.

---

## 7. Verification (this change set)

| Check | Result |
|-------|--------|
| `mvn -DskipTests compile` (Java 21) | **Passed** (2026-11-10) |
| Unit/integration tests | Not run — execute after confirmation per project policy |

**Suggested test scope after approval:**  
`mvn -B -Dtest=WalletPaymentAdapterTest,CatalogueServiceTest,ReservationExpiryServiceTest,CommerceServiceTest,CartServiceTest test`

---

## 8. Package naming (`entity` subpackages)

JPA types live under **`entity.{feature}`** (e.g. `entity.order.CommerceOrder`), not a single flat `entity/` folder. That matches your preferred **layer + subpackage** style and keeps cart/order/coupon types easy to find.

---

## 9. Splitting APIs: customer vs merchant (suggestion)

Today FinFlow uses **one** JWT surface (`commerce:read` / `commerce:write`) and **org scoping** in services (`organisationId`, `mineOnly`, membership checks). That is valid for an MVP.

For clearer boundaries (gateway routes, OpenAPI, audits, rate limits), split **adapters** without duplicating business logic:

**Recommended audience subpackages (only when you split APIs)** — use industry-neutral names:

| Subpackage | Who | Examples in commerce |
|------------|-----|----------------------|
| `controller.customer` | Logged-in shopper (B2C) | Cart, checkout, own orders, return request |
| `controller.merchant` | Org staff / seller (B2B ops) | Product CRUD, coupons, fulfilment, approve return |
| (removed) | Catalogue browse lives under `controller.customer` | `GET /api/v1/customer/products` |

Avoid vague names like `user` vs `admin` unless identity service already uses them. FinFlow JWT today uses `commerce:read` / `commerce:write`; merchant/customer folders prepare for finer permissions later.

Flat layout today (no audience split):

```
com.finflow.commerce.controller.CartController
com.finflow.commerce.controller.OrderController
…
```

After split (same URLs possible; class names must differ):

```
com.finflow.commerce.controller.customer.CartController
com.finflow.commerce.controller.merchant.ProductController
```

`service`, `entity`, `repository`, and `dto` stay in **feature** subpackages only (`service.cart`, …)—never duplicate per audience.

**Suggested mapping from current endpoints**

| Audience | Current endpoints (examples) | Future prefix (optional) |
|----------|------------------------------|---------------------------|
| **Customer** | Cart CRUD, checkout, create/cancel/pay **own** order, request return | `/api/v1/customer/...` |
| **Merchant** | Product/category PATCH/POST, coupons, `mineOnly=false` orders, fulfilment, approve/reject/complete return | `/api/v1/merchant/...` |
| **Customer read** | Product list/search, category list | `/api/v1/customer/products`, `/api/v1/customer/categories` |

**Implementation rules**

1. **Controllers stay thin** — both call the same `application` services (`CartService`, `CatalogueService`, etc.).
2. **Authorize in one place** — add permissions later, e.g. `commerce:merchant:write` vs `commerce:shopper:write`, or keep org checks in services until identity grows.
3. **Do not** split by controller alone without URL or permission differences — that is folder noise.

Gateway (api-gateway) can route `/api/v1/merchant/**` to stricter policies and `/api/v1/customer/**` to public/mobile clients.

---

## 10. Design patterns & SOLID (honest scorecard)

**Applied (substantially)**

| Pattern / principle | Where |
|---------------------|--------|
| **Hexagonal ports** | `PaymentPort` + wallet/simulated adapters |
| **Facade** | `CommerceService` orchestrates placement/settlement/cancel |
| **Repository** | Spring Data in `*.persistence` |
| **Outbox (partial)** | `PaymentOutbox` rows; drain worker still TODO |
| **Idempotency** | Orders + wallet keys |
| **Optimistic locking** | Product stock `@Version` |
| **Circuit breaker / retry** | `WalletPaymentAdapter` + Resilience4j |
| **S — Single responsibility** | `OrderPlacementService`, `PaymentSettlementService`, etc. |
| **D — Dependency inversion** | Services depend on `PaymentPort`, not wallet HTTP |
| **Constructor injection** | Controllers and services |

**Partial or gaps (not “fully enterprise” yet)**

| Item | Gap |
|------|-----|
| **O — Open/closed** | `CommerceService` still exposes catalogue/coupon helpers (delegates, but wide facade) |
| **I — Interface segregation** | One fat `CommerceService` for many callers; could split facades per audience |
| **Liskov** | No deep inheritance hierarchies — low risk |
| **Domain events** | Publisher abstraction exists; Kafka path still fire-and-forget / no outbox for events |
| **CQRS** | Not used (read/write share same model — OK at this scale) |
| **Saga** | Payment compensations on cancel/return incomplete vs ideal |
| **ACL** | No anti-corruption layer toward wallet DTOs (minimal records in adapter — acceptable) |

**Verdict:** Structure matches **modular monolith + ports/adapters** with solid transaction boundaries. It is **not** a full DDD tactical pattern textbook implementation; next improvements are **audience-split controllers**, **narrower facades**, and **outbox/reconciliation**—not renaming packages again.

---

## 11. Cleanup & maintenance

- Run `python scripts/cleanup-legacy-packages.py` after merges if old `controller/`, `service/`, `entity/` folders reappear (OneDrive/git conflicts).
- Legacy `entity/` enums were removed; canonical types live under each feature’s `domain` package.

---

## 12. Related documentation

- [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)
- [docs/SERVICE_DEPENDENCIES.md](../docs/SERVICE_DEPENDENCIES.md)
- [docs/commerce-service-13-class-retrospective.md](docs/commerce-service-13-class-retrospective.md)
