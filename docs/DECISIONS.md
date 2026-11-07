# Evident decisions and unresolved questions

Assessment date: 2026-11-07. These are observations of code/configuration, **not newly approved architecture decisions**. Where intent cannot be established, it is explicitly unknown. Broader target-design proposals remain in 02-hld, 03-lld and SystemDesign.

## Decisions evident in the implementation

| Decision | Evidence | Established reason / consequence |
|---|---|---|
| Five independently packaged services | Five POMs and main classes; no root reactor | Separate executable ownership is implemented. Why this size of service split was originally chosen is unknown. |
| Gateway WebFlux; business services MVC/JPA | Gateway Cloud WebFlux dependencies; servlet controllers/security in other modules | Reactive edge and blocking persistence are separate. No evidence that all services should become reactive. |
| Database per owning service on one local MySQL instance | Four JDBC schema URLs and separate repository/entity packages | Ownership isolation at application level; local deployment shares root credentials and infrastructure. No cross-schema transactional guarantee. |
| User-owned single wallet | wallet.user_id unique; owner checks in WalletService | Actual model is one wallet per user, irrespective of organisation/currency. Org-scoped wallets in target notes are not implemented. |
| Materialized balance + movement history | Wallet.balance, WalletHistory, WalletTransfer | Source comments explicitly cite constant-time balance reads, statements and atomic transfers. This is not a complete double-entry ledger. |
| Domain methods and optimistic locking for wallets/stock | Wallet.credit/debit; Product.reserveStock; @Version; HTTP conflict advice | Source explicitly describes moderate contention and retryable conflicts. Orders/returns/payment work do not have the same protection. |
| Stateless access JWT, opaque persisted session/action tokens | SecurityConfig, JwtService, TokenCrypto, token entities | Code/comments establish asymmetric private-key ownership and hash-only database token storage. Access token defaults 15 min; refresh 7 days; no immediate access-token revocation. |
| Membership roles map to permission strings | OrgRole and AuthService.loadOrgAccess | APIs check permissions rather than role labels. A flat union across memberships is current behaviour; cross-org permission policy is unresolved. |
| Redis shared as infrastructure with owner keyspaces | Root README, identity limiter, gateway limiter | README explicitly rejects a separate Redis service/shared starter until justified by duplicated MVC patterns. Wallet currently has no Redis dependency/use. |
| Payment port with conditional adapters | PaymentPort, WalletPaymentAdapter, SimulatedPaymentAdapter | Allows real wallet HTTP versus in-process outcomes. Main configuration uses wallet; tests simulated. Some comments describing a simulated local default are stale. |
| Place locally, then call wallet outside placement transaction | OrderPlacementService; CommerceService; PaymentSettlementService; TransactionTemplate | Source comments explicitly avoid wallet HTTP inside order DB transaction. PaymentOutbox records intent; an automatic drain/recovery worker is absent. |
| Immutable order item price/name snapshots | OrderItem; OrderPlacementService | Existing orders preserve placement values when catalogue changes; separate embedded shipping address belongs to order. |
| Separate payment outcomes | PaymentOutcome and OrderStatus | Explicit source rationale keeps decline and uncertain/manual-action outcomes distinct; uncertain calls retain stock. Expiry semantics still need reconciliation. |
| Optional Kafka, log publisher fallback | Conditional event configs/listener; main properties | Supports local execution with Kafka disabled. Logging publisher does not invoke notification in process. No durable event-delivery guarantee. |
| Consumer dedupe by event type + order | NotificationRecord unique constraint and dispatcher | Source explicitly aims to suppress Kafka redelivery. Sequential duplicate suppression tested, concurrency and real delivery not proven. |
| Log-only mail and development token exposure | Both mail classes; AuthService/OrgService | Source explicitly calls this a learning/local hook. Enabling the property does not add SMTP; SENT is not provider confirmation. |
| Hibernate-managed schema and H2 tests | Main ddl-auto=update; test create-drop | Existing implementation choice. Original reason and production suitability not established; no migration tool/baseline. |
| Non-root Java 21 containers, skip tests at image build | All Dockerfiles | Runtime UID/GID 10001; Maven build stage skips tests. Reason for skip policy is not documented. |
| Local Maven Central mirror | Gateway/identity/wallet .mvn/maven.config and settings; commerce settings without selector | Identity settings comment explicitly avoids company Nexus 401 for public dependencies. Notification has no corresponding settings; Docker build excludes these files. |
| Keep legacy types for compatibility with notes | PaymentSimulator; notificationservice legacy class | Deprecation comments explicitly preserve old references. Do not treat them as second implementations. |

## Decisions requiring discussion

1. **Replay contract:** global versus scoped key namespace, retention, payload fingerprint and collision response; how internal commerce payment keys are protected from user-submitted wallet keys.
2. **Authorization model:** should members manage catalogue/fulfilment/return approvals? What permission belongs to each organisation? Are wallets personal or organisation-owned? Is self-service deposit/block/unblock intended only for the demo?
3. **Refund authority:** staff workflow cannot rely on staff JWT ownership of customer wallets. Define a constrained internal refund contract/service identity and refund audit before adding credentials.
4. **Payment consistency:** how to reconcile uncertain wallet outcomes before expiry/cancel; durable charge/refund intent, retries, worker authentication, state transitions and recovery ownership. Do not persist raw end-user access tokens as an incidental worker workaround.
5. **Returns:** full versus partial returns, eligibility/window, one or multiple returns, stock/refund idempotency and resulting order status.
6. **Money:** commerce scale 4 versus wallet scale 2, permitted currencies, order caps, discount rounding and deterministic charge/refund amounts.
7. **Keys and sessions:** file-backed RSA persistence is implemented (see dated decision below); managed storage/overlapping rotation, issuer/audience, multi-replica operational provisioning, refresh replay/reuse detection, invitation supersession and access revocation expectations remain unresolved.
8. **Delivery guarantees:** required event durability, unique event IDs/schema evolution, publication ordering, notification retry/DLT, real provider and recipient lookup. Current payload has customerUserId, not an email address.
9. **Schema lifecycle:** initial migration baseline for existing personal databases, local/test/production profiles, dedicated DB users and explicit FK/index choices.
10. **Runtime scope:** learning project versus deployable product; full Compose stack/listeners, secrets/TLS, trusted proxies, public metrics/CORS and CI acceptance checks.
11. **Build reproducibility:** why wrapper distribution is 3.10.0 in four services while installed Maven 3.9.16 was used, and how to address OneDrive/plugin packaging failures without changing business code.

No outcomes for these questions were inferred or implemented during this documentation assessment.

## 2026-11-07: Persist identity signing keys across restarts

Approved task: stop identity restarts invalidating issued JWTs. Use one binary PKCS#8 RSA private-key file, derive its matching public key and deterministic kid, and preserve existing RS256 claims/JWKS. This avoids another metadata/public-key file getting out of sync and uses existing Java/JJWT dependencies. SigningKeyStore handles loading/local bootstrap; JwtService keeps token/JWKS responsibilities.

Local mode may bootstrap once, with a lock and atomic publication. Invalid existing files fail rather than implicitly rotating. The originally added prod properties file was removed at the user's request (see update below); provisioned secret mounts now require explicit key-path and generation-disabled settings. Key bytes are ignored by Git; filesystem ACLs, durable storage and secure provisioning remain deployment responsibilities. Relative local paths depend on working directory; deployment should use an absolute path. Previous in-memory keys cannot be migrated, so the first upgrade requires reauthentication.

Scope deliberately excludes automated rotation/KMS integration and issuer/audience changes. One-key JWKS replacement cannot guarantee overlap for existing tokens; plan staged multi-key publication/retirement before implementing rotation. Existing consumers need no code changes for stable key/kid reuse; live multi-service restart and multi-replica guarantees remain unverified. Tests exercise independent-JVM persistence and production configuration, not a full deployment.

## 2026-11-07: Use one identity runtime properties file

At the user's request, removed identity-service/src/main/resources/application-prod.properties to simplify configuration. application.properties retains persistent-key loading/local bootstrap and environment overrides. Deployment requiring a pre-provisioned key must explicitly set IDENTITY_SIGNING_KEY_PATH and IDENTITY_GENERATE_KEY_IF_MISSING=false; activating prod alone no longer supplies those safeguards. Required-key configuration tests retain missing-key rejection and provisioned-key loading coverage without depending on a prod resource. No JWT algorithms, claims, routes, database schema or organisation behavior changed.

## 2026-11-07: Lombok only in api-gateway where accessors/constructors/loggers are pure boilerplate

At the user's request, added optional Lombok to api-gateway and applied it only where it removed clear boilerplate: `@Getter`/`@Setter` on `GatewayProperties` (and nested bindable types), `@RequiredArgsConstructor` on `SecurityConfig`/`GlobalErrorConfig`, and `@Slf4j` on logging classes. Filters/config beans without fields were left explicit. `@Data` is avoided. Other FinFlow services remain Lombok-free; DTOs continue to prefer records.
