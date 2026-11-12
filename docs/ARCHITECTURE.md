# Actual FinFlow architecture

Initial source assessment: **2026-11-07**, commit `5d79a90`; Commerce package/workflow descriptions refreshed **2026-11-11**. This is an implementation map; [02-hld/ARCHITECTURE.md](02-hld/ARCHITECTURE.md), [03-lld](03-lld/README.md) and SystemDesign describe a wider target. Verification and risks are in [PROJECT_STATUS.md](../PROJECT_STATUS.md).

## Repository and technology

Five independent Maven applications, all `com.finflow:*-service:0.0.1-SNAPSHOT` (gateway artifact `api-gateway`), Java 21 and Spring Boot 3.5.6. No root POM, shared library, service-discovery server or frontend was found.

Service ownership, ports and entry points are maintained in [SERVICE_CATALOG.md](SERVICE_CATALOG.md). The actual communication/contract impact matrix is in [SERVICE_DEPENDENCIES.md](SERVICE_DEPENDENCIES.md); sequences and runtime behaviour remain here.

Gateway dependencies: Spring Cloud BOM **2025.0.3**, Gateway server WebFlux, Security/OAuth2 resource server, reactive Redis, Reactor Resilience4j, Actuator, Micrometer Prometheus and Brave. Other modules use Spring Web/MVC, Security, JPA/Hibernate, Jakarta Validation, MySQL Connector/J, JJWT **0.12.6**, Actuator, H2 test scope, Boot Test/Security Test. Identity adds blocking Redis; commerce and notification add Spring Kafka/Kafka Test. Identity, wallet and commerce include DevTools. Transitive versions follow BOMs; no transitive-version audit was performed.

## Component and runtime interactions

```mermaid
flowchart LR
    Client[API client] --> Gateway[Gateway 18080]
    Gateway --> Identity[Identity 18081]
    Gateway --> Wallet[Wallet 18082]
    Gateway --> Commerce[Commerce 18083]
    Gateway --> Notification[Notification 18084]
    Gateway --> Redis[(Shared Redis)]
    Identity --> Redis
    Identity --> IDB[(identity schema)]
    Wallet --> WDB[(wallet schema)]
    Commerce --> CDB[(commerce schema)]
    Notification --> NDB[(notification schema)]
    Commerce -->|WalletPaymentGateway HTTP + caller JWT| Wallet
    Wallet -->|Fetch public JWKS| Identity
    Commerce -->|Fetch public JWKS| Identity
    Notification -->|Fetch public JWKS| Identity
    Commerce -. optional producer .-> Kafka[commerce.events]
    Kafka -. optional consumer .-> Notification
```

Kafka arrows are disabled by default. Logging publication creates no notification record. Direct service HTTP ports exist; gateway is not the only security boundary.

Commerce uses feature-first api -> application -> persistence/domain packages; other MVC modules retain their existing layouts. DTO records and constructor injection remain. Identity DTOs separate request/response packages; others use dto. Services enforce user/organisation access; domain methods enforce selected invariants. PaymentGateway is a port/adapter seam, not evidence of a full hexagonal architecture throughout the repository.

## Workflows traced through source

### Identity and organisation onboarding

1. AuthController -> AuthService.register normalizes email, checks uniqueness, hashes password with BCrypt, creates unverified ACTIVE user and SHA-256 verification token, calls logging MailService, records audit. Default response exposes raw dev token.
2. verifyEmail loads hash, checks unused/unexpired, marks user verified/token used, records audit. Login rejects inactive/wrong credentials/unverified users.
3. Login loads ACTIVE memberships, unions their permissions, issues RS256 access JWT with sub/email/orgIds/permissions and a hashed opaque refresh token. JwtService loads a durable RSA private key through SigningKeyStore; local bootstrap creates a 2048-bit key only when absent. kid is derived from SHA-256 of the encoded public key and remains stable across restarts. A deployment can explicitly set IDENTITY_SIGNING_KEY_PATH and IDENTITY_GENERATE_KEY_IF_MISSING=false to require a provisioned key; there is no separate prod properties file. Base settings allow local bootstrap. Corrupt existing keys fail startup. One public key is published; overlapping rotation is not implemented.
4. Refresh revokes presented usable token and issues a new pair; logout revokes matching refresh hash. Reset/change password revoke all refresh tokens. Already-issued JWTs are not revoked.
5. Any authenticated user may create an organisation; creator gets ACTIVE ORG_ADMIN membership. Existing registered users can be invited by members:manage holders; invitee must log in and accept a usable matching-email token. Identity rechecks membership permissions in its own DB for protected organisation operations.
6. Last-admin checks protect sequential demotion/removal, not concurrent races. Re-login/refresh after org creation or invite acceptance is needed to get updated downstream claims.
7. TokenCleanupJob uses scheduled hourly JPQL deletes for expired/revoked/used tokens. AuditService writes in local transactions; it is not a platform-wide immutable audit system.
8. Direct identity security-filter failures use JSON code/message: missing or unusable bearer authentication on protected endpoints returns 401 with WWW-Authenticate: Bearer; authenticated Spring Security access denial returns 403. Controller/domain errors keep their own advice mapping. Public endpoints remain public even with an invalid optional token. MockMvc regressions verify these paths; live Redis and edge-to-identity behavior are separate checks.

### Wallet movement

Wallet controllers obtain the owner from JWT, not the request body. SecurityConfig checks configured `wallet:read` / `wallet:write` on GET/POST. WalletService checks ownership for reads and mutations.

Deposit/withdraw use a local transaction: owned wallet -> global history-key lookup -> credit/debit -> wallet save -> history saveAndFlush. Wallet enforces ACTIVE, positive amount and sufficient balance; current balance normalizes to 2 decimals. Transfer checks global transfer-key replay, sender ownership, different wallet and same currency, debits/credits both, saves transfer and SENT/RECEIVED history in one transaction. Wallet @Version detects conflicting updates; unique keys constrain duplicates. Sequential replay is tested; scope and concurrent replay limitations are R2/R11.

Wallet lifecycle statuses ACTIVE / BLOCKED / CLOSED exist in the domain (money moves only when ACTIVE). HTTP block/unblock/close and P2P transfer endpoints are not exposed in the e-commerce product; top-up is the shopper load path; deposit/withdraw serve commerce refunds and checkout debits.

### Commerce feature architecture (2026-11-11)

Commerce is a feature-first modular monolith: catalogue, cart, order, payment, returns and coupon own their implementation; customer/merchant HTTP adapters stay separate. See the current [package map](../commerce-service/PACKAGE-STRUCTURE.md), [source-traced feature flows](../commerce-service/COMMERCE_SERVICE_ARCHITECTURE.md) and [move inventory](../commerce-service/docs/FEATURE-FIRST-REFACTOR.md). Older status entries describe previous layouts and scheduler configurations.

Catalogue controllers call CatalogueService with ProductMapper/repositories. Marketplace product reads span sellers and require ACTIVE products; merchant writes require org access. CacheConfig activates Redis-backed category caching; membership is checked before cache lookup, and the DTO supports the existing serializer. No ProductService/CategoryService split was necessary. CartService keeps owner/seller buckets and exposes a merged view with live prices; one-seller checkout snapshots lines, places/pays an order, then clears that seller separately.

CommerceService now handles orders only. OrderPlacementService validates seller/products/stock, checks owner/seller/payload-key replay, applies CouponService lookup plus Coupon domain pricing, and atomically writes order/items, reservation, replay record and payment work. It commits before PaymentSettlementService calls PaymentGateway. A short TransactionTemplate applies APPROVED -> CONFIRMED/DONE, DECLINED -> PAYMENT_FAILED/FAILED + stock release, UNCERTAIN -> ACTION_REQUIRED/PENDING + stock retained. Repeated UNCERTAIN keeps ACTION_REQUIRED rather than requesting an invalid same-state transition.

PaymentGateway is the renamed existing port; wallet/simulated property values and HTTP contracts remain. WalletPaymentGateway forwards the caller token and stable pay/refund keys to Wallet, never accesses wallet tables, and retains 3s connect/10s read timeouts. Most caught failures bypass effective retry/circuit-breaker observation. MockPaymentGateway marks internal results as simulated; approved simulations confirm without fake wallet references, while real approvals retain required-reference validation. There is no recovery worker or durable event outbox.

Customer pay/cancel/return requires ownership; seller fulfilment/return decisions require org access. Confirmed cancellation refunds before a local stock/status transaction. Returns require FULFILLED -> REQUESTED -> seller decision -> refund -> restock/COMPLETED; original orders stay FULFILLED. Merchant-token refunds may fail the wallet's owner-only checks. Cart replay/edit races, return duplication and settlement/cancel/expiry/refund coordination remain open. Maintenance services are callable, but no scheduler/ShedLock configuration is currently wired; POM dependencies and TTL hints alone do not activate jobs.

Standalone Commerce testing now has an explicit local profile: loopback bind, JWT validation disabled, configured user/org principal and simulated payments. Default/integrated startup still validates JWTs; re-enable by removing local or overriding commerce.security.jwt-validation-enabled=true. Existing org/owner checks remain. See [Commerce README](../commerce-service/README.md). Health skips disabled Identity/Wallet integrations in local mode; MySQL/Redis settings are unchanged. Automatic cart seller inference now reads lazy items inside a short transaction. Obsolete migration scripts/backups/scaffolding and PaymentSimulator were removed after the completed refactor.

### Events and notifications

Commerce publishes ORDER_PAID, ORDER_CANCELLED, ORDER_EXPIRED, ORDER_FULFILLED, RETURN_COMPLETED through CommerceEventPublisher. Default is logging; enabled Kafka publisher sends JSON keyed by orderId, topic commerce.events, three partitions/one replica. Producer send completion is ignored. Publication is not backed by a durable domain-event outbox; payment_outbox serves a different purpose.

Notification conditional @EnableKafka/config/listener accepts JSON into a locally duplicated CommerceDomainEvent record (type headers disabled, trusted packages '*'). Dispatcher checks (eventType,orderId), inserts/flushed NotificationRecord, then calls MailChannel in the DB transaction. Initial status is SENT. Both mail flag branches only log userId/subject/body; no recipient email lookup/provider exists. Inbox filters by JWT subject. Sequential duplicate tests pass; concurrent unique-violation rollback and delivery atomicity are unresolved.

## API inventory and current behaviour

All paths below include the /api/v1 prefix unless explicitly absolute. Behaviour is source-traced; full HTTP contracts were **not exercised**. Success defaults to 200 unless noted. Request validation may return 400; domain ApiException maps 401/403/404/409/422 (plus auth rate-limit 429). See DTOs for exact wire fields.

### Identity

Public POST endpoints (no access JWT required):

| Path | Body / success behaviour |
|---|---|
| /auth/register | email/password (8â€“72 chars) -> 201 profile + dev token; 409 duplicate |
| /auth/verify-email | token -> 204; invalid/used/expired -> 401 |
| /auth/resend-verification | email -> generic message + dev token when applicable |
| /auth/login | email/password -> accessToken/refreshToken/tokenType/expiresInSeconds/userId/email/orgIds/permissions; 401 rejection |
| /auth/refresh | refreshToken -> rotated pair; unusable -> 401 |
| /auth/logout | refreshToken -> 204; unknown token ignored |
| /auth/forgot-password | email -> generic message + optional dev token |
| /auth/reset-password | resetToken/newPassword -> 204 |

Public GET **/.well-known/jwks.json** returns current RSA key. Authenticated POST /auth/change-password accepts currentPassword/newPassword -> 204; GET /users/me returns current profile/DB memberships.

| Organisation path | Behaviour |
|---|---|
| POST /orgs; GET /orgs | 201 create with creator admin; list caller ACTIVE memberships |
| GET /orgs/{orgId} | Active member only |
| POST /orgs/{orgId}/invites | email/role; members:manage in that org -> 201 invite, existing user required |
| POST /orgs/invites/accept | token; logged-in matching-email user -> membership |
| GET /orgs/{orgId}/members | Active member; returns ACTIVE/INVITED members |
| PUT /orgs/{orgId}/members/{userId}/role | role; members:manage; guard last admin |
| DELETE /orgs/{orgId}/members/{userId} | members:manage; soft-remove -> 204; guard last admin |
| GET /orgs/{orgId}/audit | org:manage; newest first |

### Wallet

GETs require `wallet:read`; POSTs require `wallet:write`. Owner checks are additional on wallet-id paths; `/customer/wallet` resolves the caller from JWT.

| Method/path | Behaviour |
|---|---|
| GET/POST /api/v1/customer/wallet/** | Customer-facing balance, open, top-up, history (any app) |
| GET /wallets/{walletId} | Owner-only balance/status (integration/tools) |
| GET /users/{userId}/wallet | Path user must equal caller (commerce lookup) |
| POST /wallets/{walletId}/deposits; /withdrawals | amount, required Idempotency-Key (commerce) |
| GET /wallets/{walletId}/transactions | Full newest-first statement (optional tools) |

### Commerce

The current routes use `/api/v1/customer/**` and `/api/v1/merchant/**`, preserved by this package refactor. The complete [HTTP surface and flow guide](../commerce-service/COMMERCE_SERVICE_ARCHITECTURE.md#preserved-http-surface) lists methods, paths and security boundaries. Catalogue, cart, customer orders/returns and merchant products/categories/coupons/orders/returns retain the original JSON and Idempotency-Key contracts. GET /api/v1/** requires commerce:read and mutations commerce:write; ownership/org checks also apply. The dependency-health API is currently protected by commerce:read. Audience package names alone do not establish a stronger permission model.

### Notification and gateway

Authenticated GET **/api/v1/notifications/me** returns caller's full newest-first inbox. No REST notification-create/mark-read/retry endpoints exist.

Gateway preserves downstream paths. Routes: /api/v1/users/me -> identity; /api/v1/users/*/wallet and /api/v1/customer/wallet/** -> wallet; /api/v1/auth/**, /api/v1/orgs/** and JWKS -> identity; /api/v1/wallets/** -> wallet; commerce path families above -> commerce; /api/v1/notifications/** -> notification.

Gateway default route filters configure Redis token bucket 50/s with burst 100, 1 MB body limit and deduped CORS headers. Each route has circuit breaker fallback **/fallback/{service}** (503 UPSTREAM_UNAVAILABLE), GET-only retry twice on 502/503/504, 2 s connect and 5 s response/time-limiter defaults. Non-GET money/auth operations are not retried by this configuration. The unused app.max-body-bytes binding was removed during cleanup; active RequestSize remains the literal 1MB in YAML. HTTP timeout properties are consumed directly by YAML, without a duplicate typed Http binding.

JWT protects /api/v1/** except listed public POST auth routes; public JWKS/OPTIONS/health/info/prometheus/fallback. Other gateway exchanges permit access by default, so exposed actuator metrics/gateway endpoints require deployment review. Wildcard allowed origin patterns with credentials false. CorrelationIdFilter is a route GlobalFilter, not a pre-security WebFilter; uniform IDs/logging on security rejections are not proven.

## Security, validation, errors and logging

- Identity issues and verifies locally; gateway uses resource-server JWKS; wallet/commerce/notification use custom JJWT filters and cached RSA JWKS keys (300 s, refresh on unknown kid). Invalid token exceptions are swallowed to clear authentication.
- No issuer/audience claims/checks in custom issuance/validation; keys now persist across restarts; file replacement still changes trust, and overlapping rotation remains unimplemented. Identity-org operations use DB permissions; downstream claims may be stale.
- DTO records have Jakarta constraints and @Valid; coverage/consistency varies (for example price precision/create versus update). Controller method constraints and malformed JSON paths lack HTTP regressions.
- Identity/wallet/commerce advice emits code/message and validation details; wallet/commerce handle optimistic conflicts. Wallet/commerce security send explicit JSON 401/403. Identity has no equivalent custom entry-point/error body; framework behaviour should be verified rather than copied from comments. Notification has auth JSON entry point but no domain controller advice.
- Gateway has security JSON errors, reactive global exception mapping and fallback JSON. Filter-produced errors are not guaranteed to pass its exception handler.
- SLF4J logs gateway method/path/status/duration/correlation and service events. No complete downstream MDC propagation, central collector or distributed tracing backend is configured. Identity audit records selected successes, not failed login attempts; identity mail-stub logs omit token-bearing bodies after cleanup; local API responses still expose development tokens when mail.enabled=false.

## Persistence, build and deployment

See [DATABASE_DESIGN.md](../DATABASE_DESIGN.md) for all 22 mapped tables. Main schemas use localhost:3307 and Hibernate update, open-in-view false. H2 test schemas use MySQL mode/create-drop. No migrations were found.

Compose: MySQL 8.4 host 3307, Redis 7.4-alpine host 6379, Kafka 3.8.1 single-node KRaft host 9092; finflow-net. mysql-data/redis-data are external named volumes. Only wallet-service is in apps profile and points JWKS to host.docker.internal:18081. Kafka advertises localhost; full container-to-container Kafka use needs listener configuration.

Each Dockerfile builds with Maven/JDK21, skips tests, and runs Temurin21 JRE as non-root uid10001. Four modules include Maven wrappers configured for Maven 3.10.0; commerce has no wrapper. Gateway/identity/wallet .mvn/maven.config automatically select local maven-settings.xml, mirroring all repositories to Maven Central; commerce has the settings file but no automatic selector, and notification has neither settings file nor selector. The identity settings comment identifies avoiding company Nexus 401 errors as the reason. Docker contexts exclude these settings/wrappers and test sources. No CI/CD manifests were found. Postman environment/collections and Python generators support manual flows; not executed during this assessment.

Testing combines context-load tests and H2 service tests, with mock HTTP wallet-adapter checks. No HTTP security/controller, live infrastructure, return workflow, concurrency, or full-stack automated verification was found. See status for actual executed outcomes and packaging failures.
