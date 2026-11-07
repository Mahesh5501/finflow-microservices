# Actual service dependencies

Baseline: existing 2026-11-07 implementation assessment. These are documented implementation/configuration facts, not freshly exercised integration guarantees. See [ARCHITECTURE.md](ARCHITECTURE.md) for sequences and [PROJECT_STATUS.md](../PROJECT_STATUS.md) for verification/risks.

| Caller/producer -> provider/consumer | Mechanism / contract | State and impact when changing |
|---|---|---|
| Gateway -> identity | HTTP auth/org/user-me routes; public JWKS | Configured; identity contract changes may affect edge routes/security and clients |
| Gateway -> wallet | HTTP wallets/transfers and users/{userId}/wallet | Configured; preserve route distinctions from identity /users/me |
| Gateway -> commerce | HTTP products/categories/carts/coupons/orders/returns | Configured; check edge routing when path families change |
| Gateway -> notification | HTTP notifications paths | Configured; inbox requires bearer token |
| Gateway, wallet, commerce, notification -> identity | HTTP GET /.well-known/jwks.json; RS256 JWT sub/email/permissions, orgIds where consumed | Gateway resource server; other services custom cached JWKS. Signing key/kid now persist via a configured identity key file; replicas must share the same provisioned key. Public JWKS/claims unchanged; no live cross-service HTTP restart test evidence |
| Commerce -> wallet | RestClient, caller JWT; GET /api/v1/users/{userId}/wallet, POST /api/v1/wallets/{walletId}/withdrawals or /deposits; amount and Idempotency-Key | Main payment.provider=wallet; tests simulated or mock HTTP. Charge key commerce-pay-{orderId}, refund commerce-refund-{orderId}; owner checks constrain staff refunds |
| Commerce -> Kafka -> notification | JSON commerce.events, orderId partition key, locally duplicated CommerceDomainEvent records/enums | Both flags default false. Producer ignores send completion; no durable domain-event outbox. Consumer dedupes eventType+orderId |

Commerce events: ORDER_PAID, ORDER_CANCELLED, ORDER_EXPIRED, ORDER_FULFILLED, RETURN_COMPLETED. Payload fields: type, orderId, orderNumber, organisationId, customerUserId, totalAmount, currencyCode, occurredAt. No recipient email or unique event ID. When changing this contract, inspect both event models/configs plus publisher and consumer tests; logging publication does not deliver to notification.

## Infrastructure and ownership

- Identity/wallet/commerce/notification own their separate MySQL schemas; shared local MySQL instance is not a shared data-access API. No cross-service DB joins/transactions.
- Gateway uses shared Redis token buckets; identity uses Redis fixed-window auth limits. Wallet has no Redis implementation.
- Commerce and notification have custom conditional Kafka configs despite default Kafka auto-configuration exclusion. No wallet/identity event pipeline is implemented.
- Mail calls in identity and notification are log-only, not SMTP/provider dependencies. Fulfilment is state management without a shipping integration.
- Static localhost service URLs are defaults; no service discovery. Only wallet is in Compose apps profile and needs host identity. Kafka advertises localhost:9092; other-container consumers need listener configuration.

## Contract-change handoff

Record changed services, endpoint/event/claim fields, compatibility and caller/consumer verification in the progress checkpoint. Update this file only when actual communication or dependencies change. Inspect directly affected callers/providers and expand only when an observed dependency requires it; do not infer that all services need rebuilding.
