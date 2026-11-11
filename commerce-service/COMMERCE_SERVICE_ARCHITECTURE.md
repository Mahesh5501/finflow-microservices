# Commerce service architecture and learning guide

Updated 2026-11-11 from current source. Java 21, Spring Boot 3.5.6, independent Maven application on port 18083. MySQL/JPA, Redis, JWT/JWKS, optional Kafka, Resilience4j, Actuator/Micrometer, JUnit/Mockito remain in the existing POM. Existing Lombok use is preserved; no dependency was added.

The implemented structure is a feature-first modular monolith inside the existing Commerce service. [PACKAGE-STRUCTURE.md](PACKAGE-STRUCTURE.md) lists every class. [FEATURE-FIRST-REFACTOR.md](docs/FEATURE-FIRST-REFACTOR.md) lists all 63 moves, created/modified classes and cleanup limitations.

## Boundaries and decisions

- Controllers handle routes, JSON binding, validation and trusted caller extraction. Application services enforce use cases and access checks. Domain/JPA objects own selected invariants; repositories remain Spring Data interfaces. DTOs isolate API shape from entity state.
- CatalogueService remains cohesive: category management is small and product creation/filtering needs category validation anyway. ProductMapper stays in application. No ProductService/CategoryService wrappers were added.
- CommerceService now coordinates orders only. Catalogue controllers call CatalogueService directly; MerchantCouponController calls the new CouponService. Placement, settlement, cancellation and fulfilment stay understandable without one class per tiny operation.
- CouponService owns merchant creation and seller-scoped checkout lookup. OrderPlacementService applies the returned Coupon domain behavior in its existing transaction, preserving the stored coupon code and money rounding.
- Existing PaymentPort/WalletPaymentAdapter/SimulatedPaymentAdapter were relocated and renamed PaymentGateway/WalletPaymentGateway/MockPaymentGateway. Provider settings remain `payment.provider=wallet|simulated`; no parallel payment implementation was created. The unused deprecated PaymentSimulator marker was removed in the subsequent cleanup.
- Event contracts/publishers remain in integration.event. CommerceEvents maps orders in order.application, so integration.event has no entity dependency. Feature dependencies are acyclic by import review; they are conventions rather than separately enforced modules.
- Table names, column names, constraints and versions are unchanged. The package refactor introduced no schema migration, endpoint, event JSON or permissions change. The subsequent local-testing change adds an explicit guarded security property/profile, documented below. Default security remains enabled. No wallet balance/ledger entity exists in Commerce.

## Catalogue flow

Customer HTTP -> CustomerCatalogueController -> CatalogueService -> ProductRepository/CategoryRepository -> MySQL -> ProductMapper -> ProductResponse -> JSON. Search uses Specifications for optional case-insensitive name/category filters and Pageable for bounded results. Marketplace products must be ACTIVE; the shared catalogue spans sellers. Categories are listed globally. These routes still require authenticated commerce:read under SecurityConfig.

Merchant product/category HTTP -> merchant controller -> validated DTO + principal orgIds -> CatalogueService ownership/category checks -> repository -> domain/DTO response. PATCH nulls preserve existing fields; they do not remove the category. Product @Version detects competing stock writes at commit. The explicit pessimistic repository query remains available; ordinary placement still uses findById plus optimistic versioning.

CacheConfig activates existing @Cacheable/@CacheEvict annotations with Boot's configured Redis cache. The category cache key remains organisationId; its condition checks membership **before lookup**, because a hit skips method authorization. Unauthorized callers execute the body and fail there. CategoryResponse is Serializable for the default Redis JDK serializer; no JSON field changes. Category creation evicts the seller's cached list. Tests use a simple cache and separately exercise the Redis serializer; they do not prove live Redis connectivity. Cached Java class names change on package migration; clear affected category cache entries when deploying over an installation with old serialized DTOs.

## Cart flow

CartController -> CartService -> active product lookup -> seller derived from product -> CartRepository -> customer/seller Cart and CartItem -> merged CartResponse. Queries always use the authenticated customer ID. Buyers need not belong to seller organisations. The API merges seller buckets, reports live prices/availability, and reserves no stock. Add increments a quantity; PUT replaces it and zero removes; DELETE removes a line or clears owner buckets.

Checkout selects one seller from sellerOrganisationId, body organisationId or the sole nonempty bucket. Multiple sellers require the existing explicit selector; empty carts fail. A TransactionTemplate snapshots lines, then CommerceService places/pays the order, then another transaction clears that seller. The clear still happens for returned DECLINED/UNCERTAIN outcomes. Current replay/concurrent edit behavior can lose cart changes; the refactor does not claim atomic cart/payment coordination. AbandonedCartCleanupService deletes stale carts when called; no automatic scheduler is wired.

## Order and checkout flow

```mermaid
sequenceDiagram
    participant API as CustomerOrderController / CartService
    participant Facade as CommerceService
    participant Placement as OrderPlacementService
    participant DB as Commerce database
    participant Settlement as PaymentSettlementService
    participant Gateway as PaymentGateway
    participant Wallet as Wallet service
    API->>Facade: createOrder + owner + key + request
    Facade->>Placement: one seller and requested lines
    Placement->>DB: TX stock + order/items + replay key + payment work
    DB-->>Placement: commit
    Placement-->>Facade: orderId
    Facade->>Settlement: settleCharge
    Settlement->>Gateway: charge (no outer DB transaction)
    Gateway->>Wallet: wallet GET + withdrawal (wallet provider)
    Wallet-->>Gateway: approved / declined / ambiguous result
    Gateway-->>Settlement: PaymentResult
    Settlement->>DB: short TX apply result + work/stock updates
    Settlement-->>API: response; paid event on confirmation
```

Placement requires an idempotency key; a length-prefixed SHA-256 fingerprint includes customer, seller, ordered lines, address, coupon and sandbox outcome. A matching replay returns the previous order ID; another owner/seller/payload conflicts. Product status, seller and stock are validated. Names/prices are snapshotted, INR is used, address is optional for direct orders and mandatory for cart checkout. Coupon lookup is scoped to the seller; inactive/expired coupons cause COUPON_INVALID. All local placement changes roll back together on failure.

PaymentSettlementService reloads state after HTTP in TransactionTemplate. APPROVED confirms/DONE and records wallet/transaction references for real payments; DECLINED restores stock and marks PAYMENT_FAILED/FAILED; UNCERTAIN retains stock and marks ACTION_REQUIRED/PENDING. A repeat UNCERTAIN now keeps ACTION_REQUIRED instead of attempting an invalid same-state transition. Original order transition rules are unchanged. A completed order is returned without another charge through this settlement path.

CommerceService keeps create/pay/cancel free of an encompassing transaction. TransactionTemplate uses REQUIRED and can join an ambient transaction, so callers must preserve this contract. Read methods map lazy order items inside read-only transactions. Merchant fulfilment moves CONFIRMED -> FULFILMENT_PENDING -> FULFILLED (direct CONFIRMED -> FULFILLED remains allowed). The owner may cancel allowed statuses; a confirmed order refunds before OrderCancelService releases stock and commits CANCELLED. ReservationExpiryService is callable but no scheduler is wired; it expires old unpaid reservations and fails unfinished work.

## Payment flow and ownership

Order settlement -> PaymentGateway -> conditional WalletPaymentGateway -> wallet's own-user wallet lookup -> currency check -> withdrawal with caller bearer token and `commerce-pay-{orderId}` -> PaymentResult. Refund uses recorded wallet (or lookup), deposit and `commerce-refund-{orderId}`. Wallet alone owns balances, ledger and debit/credit transactions. No banking/card/UPI integration was added.

Wallet client retains 3-second connect/10-second read timeouts. Known 4xx/business rejection is DECLINED; 5xx/transport ambiguity is UNCERTAIN. @CircuitBreaker/@Retry remain, but the adapter catches most errors before proxy advice can observe them; retry/backoff/open-circuit recovery is not fully implemented. Do not infer resilience from annotation presence.

MockPaymentGateway preserves outcome hints, makes no external calls and invents no wallet IDs. PaymentResult now carries an internal simulated flag: simulated approval confirms with null references, while real-provider approval still calls CommerceOrder.recordPayment and requires both IDs. Clients do not supply PaymentResult or this flag; WalletPaymentGateway cannot be switched into simulation by checkout JSON. Local HTTP regressions exercise cart checkout and cancellation/refund with the simulator.

PaymentOutbox is payment-work intent written atomically with placement, not an event-delivery outbox. It has no drain worker/reconciler. No bearer token is stored for unattended wallet calls. Concurrent settlement/cancel/expiry and refund-finalization failures need separate design work.

## Returns flow

CustomerReturnController -> ReturnService -> order owner + FULFILLED validation -> REQUESTED ReturnRequest saved. MerchantReturnController -> seller membership check -> approve/reject domain transition. Complete requires APPROVED -> PaymentSettlementService refund outside local transaction -> TransactionTemplate reloads, restores stock, marks COMPLETED -> RETURN_COMPLETED event. The order stays FULFILLED.

Only the order's customer requests a return; merchant operations pass null callerUserId to force seller membership checks. Domain state errors are RETURN_STATE conflicts. Completion forwards the merchant's bearer token; the wallet's owner-only API may reject a refund to a different user's wallet. No service refund authority, single-return constraint, partial return, return window, external logistics or durable refund work exists. Tests prove selected ownership/state/restock behavior with the simulated refund, not live merchant-to-wallet authorization.

## Coupon flow

MerchantCouponController -> CouponService -> org membership -> duplicate code check -> CouponRepository -> Coupon -> CouponResponse. Existing org/code unique constraint remains final concurrent protection. No customer management API was added.

During checkout, OrderPlacementService -> CouponService.findForCheckout(seller, trimmed code) -> seller-scoped case-insensitive repository lookup -> Coupon.apply validates activity/expiry and calculates a four-decimal HALF_UP discounted total -> CommerceOrder.applyDiscount captures code/discount/total. REQUIRED read-only lookup joins the existing write transaction; it does not start an independent pricing commit or make the outer transaction read-only.

## Security, discovery and infrastructure

CommerceServiceApplication stays at com.finflow.commerce; component/JPA/repository auto-discovery includes every feature below it. No explicit scan whitelist was needed. SecurityConfig, JWT filter/JWKS provider, exception advice and health controllers retain their packages. Normal startup retains JWT validation and the existing permission rules. /api/v1 GET requires commerce:read; mutations require commerce:write; application owner/org checks remain. JWT org/permission claims may be stale and permissions are unioned across memberships; package names do not add stronger tenant isolation.

Standalone testing uses `--spring.profiles.active=local` (IntelliJ Active profiles: local). application-local.properties binds 127.0.0.1, sets commerce.security.jwt-validation-enabled=false, supplies configured user 1/org 10 and commerce read/write permissions, and selects simulated payments. JwtAuthFilter refuses bypass outside the local profile alone or on a non-loopback bind. It never decodes unverified claims or accepts a request-supplied owner. The same permission/ownership checks still run with that fixture principal. Its servlet registration is disabled so it executes only inside Spring Security's chain.

Normal startup defaults to commerce.security.jwt-validation-enabled=true. Remove the local profile to restore JWT + wallet defaults, or override the property to true while retaining local simulation. Dependency health skips Identity JWKS when validation is disabled and Wallet when simulation is selected. MySQL/Redis configuration is unchanged. See [README](README.md) for direct-service startup and Postman settings. The automatic single-seller cart lookup now has a short transaction to keep lazy items attached, without enclosing wallet HTTP.

Kafka remains optional (`commerce.events.kafka-enabled=false` by default). Logging is default; enabled Kafka sends the same JSON keyed by orderId, without waiting for acknowledgement. ORDER_PAID/RETURN_COMPLETED publish after the transaction callback; cancellation/expiry/fulfilment currently publish inside a transaction. No durable domain-event outbox exists.

Actuator and /api/v1/health/dependencies remain distinct. Current SecurityConfig protects dependency health with commerce:read (older runbooks called it public). Health probes themselves do not prove successful payment/security integration. Scheduled jobs and ShedLock configuration are absent from current source even though the dependencies/TTL hints remain in the POM/properties; older checkpoints describe a previous implementation.

## Preserved HTTP surface

| Audience/feature | Existing routes |
|---|---|
| Customer catalogue | GET /api/v1/customer/categories; /products/search; /products/{productId} |
| Merchant catalogue | POST /api/v1/merchant/categories; POST /api/v1/merchant/products; PATCH /api/v1/merchant/products/{productId} |
| Cart | GET/DELETE /api/v1/customer/carts; POST /items; PUT/DELETE /items/{productId}; POST /checkout |
| Customer orders | POST/GET /api/v1/customer/orders; GET /{orderId}; POST /{orderId}/pay or /cancel |
| Merchant orders | GET /api/v1/merchant/orders?organisationId=&mineOnly=; GET /{orderId}; POST /{orderId}/fulfilment/start or /complete |
| Customer returns | POST/GET /api/v1/customer/orders/{orderId}/returns |
| Merchant returns | POST /api/v1/merchant/returns/{returnId}/approve, /reject, /complete |
| Coupons | POST /api/v1/merchant/coupons |

Paths after a semicolon in a row share that row's prefix. Idempotency-Key, seller selector, optional fields, response JSON and 201 creation statuses are unchanged. All nine controller route/status annotation sets were compared to the task-start sources.

## Verification and remaining work

Current local-testing results are recorded below; the earlier refactor verification is explicitly historical. Tests run under Java 21 with the existing H2 test datasource and a simple test cache; no production/test database configuration was changed. Gateway HTTP tests use MockRestServiceServer. MVC tests exercise both fixed local and injected principals; unit filter tests verify real RSA signatures and reject forged/expired tokens using an in-memory key provider. Live JWKS HTTP/key refresh is not exercised. No live MySQL/Redis/Kafka/wallet test, containers, SQL or production startup was run.

Remaining work: outbox recovery and durable events, order/payment/refund concurrency coordination, wallet retry/breaker behavior, merchant refund authorization, cart replay/concurrent edits, return uniqueness/window, versioned database migrations, deployment secret configuration and load/concurrency verification. These are explicit limits on production readiness, not additional services or CQRS requirements.

### Historical refactor verification - 2026-11-11 (before local-testing changes)

| Check under per-process Temurin Java 21 | Actual result |
|---|---|
| `mvn -B -o compile` | BUILD SUCCESS; 90 main-source files (after correcting moved event-factory imports) |
| `mvn -B -o clean test` | BUILD SUCCESS; 19 tests, 0 failures, 0 errors, 0 skipped |
| `mvn -B -o -DskipTests package` | BUILD SUCCESS; executable Spring Boot JAR created; tests skipped only in this packaging invocation |
| Source/package/import/dependency review | All 95 main/test Java files have matching packages; no legacy code imports; feature import graph is acyclic |
| Domain/API baseline comparison | 15 domain types have identical executable/mapping source; route/status annotations match across all 9 controllers |
| JAR inspection | Correct main class and relocated classes; no legacy layer-first packages or unresolved-compilation error artifacts |

Tests: CatalogueServiceTest 3; ReservationExpiryServiceTest 2; CommerceServiceApplicationTests 1; FeatureWorkflowIntegrationTest 7; PaymentGatewayTest 6. SpringBootTest cases are the available H2 integration tests and run through Surefire; no separate Failsafe profile is configured. Eight JPA repositories were discovered in the test context.

Earlier attempts are not hidden: the first test run hit Windows temporary-file/Mockito restrictions, resolved by an authorized escalated run. That run exposed a test expectation about uppercase coupon codes and a real repeated-UNCERTAIN state-transition failure; both were corrected. A subsequent incremental run loaded a class containing unresolved Resilience4j compilation errors despite correct source; the final clean build regenerated it successfully. The cause of the intervening artifact replacement is unconfirmed. The final packaged JAR was checked for such error placeholders.

Logs: `target/feature-refactor-final.log` (clean compile/test), `target/feature-refactor-package.log` (packaging); detailed counts in `target/surefire-reports`. An initial failed-compile log is retained separately. The generated target files can be removed by a future clean build.

### Local JWT switch and cleanup verification - 2026-11-11

- Java 21 `mvn -B -o clean test`: BUILD SUCCESS, **29 tests, zero failures/errors/skips**. Includes normal JWT permissions, local token-free merchant/cart/checkout/refund, seller isolation, re-enabled validation, RSA signature/expiry checks and startup guards. No real Identity/Wallet/Redis/MySQL/Kafka environment was started or exercised.
- The first new checkout regression exposed a detached lazy cart collection during automatic seller inference; a short TransactionTemplate lookup fixed it. The final clean suite includes that regression.
- Removed 84 obsolete files: 33 migration backups, eight one-shot scripts, one structure generator, 41 scaffold markers and unused PaymentSimulator. Removed all empty source/script/backup directories using verified paths; no permission workaround or ACL change was needed. Kept application code, tests, Postman, IDE settings, Maven configuration and architecture/learning documentation.
- Latest logs: target/commerce-local-tests.log and target/surefire-reports. Historical target logs can be removed by clean builds.
- Java 21 `mvn -B -o -DskipTests package`: BUILD SUCCESS. Final JAR has secure default/local profile resources and no legacy PaymentSimulator or unresolved-compiler placeholders; all 98 main/test Java paths/imports checked.
