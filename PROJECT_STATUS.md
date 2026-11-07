# FinFlow project status

Last assessed: **2026-11-07 (Asia/Calcutta)**. Source baseline: Git commit `5d79a90` (`commit`); initial tracked working tree clean. This assessment changes documentation only.

## Latest progress checkpoint - 2026-11-07 (multi-service workflow)

- **Scope/completed:** Shared repository setup only; no application service implementation changed. Extended root AGENTS.md for service selection/switching, affected-service progress and dependency maintenance. Reused/updated start-project and save-progress; created targeted-code-investigation, code-change-verification and database-migration-review in canonical .agents/skills/.
- **Important documentation:** Added docs/SERVICE_CATALOG.md (ownership/technology/entry points extracted from existing architecture), docs/SERVICE_DEPENDENCIES.md (documented communications/contracts), docs/DEVELOPMENT_WORKFLOW.md (daily use and agent compatibility). Replaced the duplicate architecture service table with a catalog link; application architecture is unchanged. Added separate service progress sections below. DATABASE_DESIGN.md and docs/DECISIONS.md remain unchanged.
- **Instruction review:** Checked the five service directories for existing nested AGENTS.md; none found. Existing root rules already describe relevant differences, including reactive gateway versus MVC/JPA and conditional integrations. No redundant nested instruction files, scripts, dependencies, CLAUDE.md or alternate skill copies created.
- **Verification:** Python standard-library inline checks passed for exactly five skill files, folder/name match, nonempty flat YAML name/description frontmatter, unique names, UTF-8 encoding, trailing whitespace and local documentation links. git diff --exit-code passed for tracked files; git status --short lists only the untracked documentation/skills (their content was checked separately). Reattempted bundled quick_validate.py for start-project; it stopped at import with missing PyYAML, as previously documented; no dependency added. No fresh-session Codex/Cursor/Claude discovery or behavioural invocation tested. No application tests rerun for setup; earlier test/build evidence remains historical.
- **Git baseline:** HEAD 5d79a90; initial documentation and two skills were already untracked. Current work updates AGENTS.md, PROJECT_STATUS.md, docs/ARCHITECTURE.md and the two skills, and adds the three skills/three docs above. No staging/commit or tracked application/configuration changes.
- **Pending/blockers/next:** R1-R18 and application status remain unchanged. Next implementation recommendation remains commerce + wallet idempotency R1/R2, after replay-contract agreement; restore failing packaging separately. Use $start-project to choose one or more services. Agent compatibility/discovery is documented from official sources but not exercised locally.

## Previous checkpoint - 2026-11-07 (two-skill workflow)

- **Completed:** Lightweight continuity workflow. AGENTS.md now requires reading it and this status before meaningful work, task-scoped inspection, preservation of existing conventions/unrelated work, approval for justified dependency/architecture changes, targeted testing and factual reporting. Progress updates happen after meaningful completed tasks without a separate permission interruption.
- **Changed components:** AGENTS.md (working/progress rules); .agents/skills/start-project/SKILL.md (read-only state handoff followed by user task selection); .agents/skills/save-progress/SKILL.md (evidence-based documentation checkpoint/end-of-day summary); this file (checkpoint). Both skills have name/description frontmatter and can be invoked as `$start-project` and `$save-progress`. No scripts or application/configuration changes were added.
- **Verification:** Ran the skill-creator bundled `quick_validate.py .agents/skills/start-project` and `quick_validate.py .agents/skills/save-progress` via Python; both stopped at import with `ModuleNotFoundError: No module named 'yaml'`. No dependency installed. A Python standard-library inline check passed for both folder/name matches, required nonempty name/description fields, flat YAML frontmatter delimiters, UTF-8 without BOM and trailing whitespace. `git diff --exit-code` passed for tracked files; Git status shows only documentation/skill paths as untracked. These files are untracked, so that diff command alone does not validate their content.
- **Application tests:** Not rerun for this documentation/skill-only task. The 47 passed/1 skipped results and packaging failures below belong to the earlier assessment, not this checkpoint.
- **Checkpoint baseline:** HEAD remains `5d79a90`; no commit/staging performed. The five initial documentation files were already untracked when this task started. AGENTS.md and PROJECT_STATUS.md were updated; two untracked skill files were added. DATABASE_DESIGN.md, docs/ARCHITECTURE.md and docs/DECISIONS.md were preserved. Compare against this checkpoint before attributing the dirty tree to future work.
- **Partial/pending and blockers:** Application status and R1-R18 remain unchanged. Bundled skill validation is unavailable until PyYAML is available; fresh-session skill discovery and behavioural execution have not been tested. Architecture/workflows and significant technical decisions did not change, so architecture/decision documents need no update.
- **Next task:** Review and implement idempotency scope/ownership fixes R1/R2 with targeted regressions, after agreeing on replay semantics. Restore the three outstanding packaging failures separately. Use `$start-project` to choose the next development task.

## Overview and evidence

FinFlow is a Java 21 / Spring Boot 3.5.6 backend with five independently built services: gateway, identity, wallet, commerce and notification. It implements a local wallet and commerce demonstration with real JPA persistence and an HTTP wallet payment adapter. It is not yet a verified production platform.

The assessment used the existing CodeGraph index first, then source, POMs, configurations, entities, repositories, controllers, DTOs, tests, Dockerfiles, Compose and existing documentation. Flows below were traced through implementations. Existing high/low-level designs describe broader ambitions (ledger, recovery workers, integrations and operational tooling) that are not all present. See [actual architecture](docs/ARCHITECTURE.md), [database mappings](DATABASE_DESIGN.md) and [decisions](docs/DECISIONS.md).

“Completed and verified” below means the named behaviour passed existing automated tests, within their limits. It does not mean the whole feature is production complete. Risks remain applicable to tested features.

## Feature classification

| Area | Classification | Actual behaviour and evidence |
|---|---|---|
| Registration, verification, login | Completed and verified for tested service paths | Normalizes email, BCrypt hashes passwords, requires verification before login; duplicate/wrong-password paths tested in AuthServiceTest. Mail remains a stub. |
| Refresh rotation/logout/reset | Completed and verified for sequential service paths | SHA-256 opaque-token hashes, rotation/revocation and reset tested. Concurrent consumption and access-token revocation are not verified. |
| JWT issuance | Completed and verified for local issuance/parsing | RS256, JWKS and membership claims exercised by AuthServiceTest. Cross-service HTTP verification not tested. |
| Organisation onboarding | Completed and verified for tested paths | Creator admin; invite an existing registered user, accept matching email, deny member invitations, protect last admin; OrgServiceTest. |
| Membership role changes/removal, profile/audit, resend/change-password, hourly cleanup | Implemented but not fully verified | Concrete controller/service/repository paths exist; coverage is incomplete, especially concurrency and scheduling. Audit is identity-local. |
| Wallet balances/movement | Completed and verified for tested service paths | Zero balance, duplicate user, deposit/history, withdrawal/insufficient funds, sequential replay, transfer/ownership/currency checks, block/unblock/close: WalletServiceTest. |
| Wallet HTTP authorization and concurrency | Implemented but not fully verified | Permission filters, owner/party checks, unique keys and optimistic locking exist. No HTTP/security or concurrent MySQL tests. Replay defects below remain. |
| Catalogue/categories/search | Completed and verified for tested paths | Category creation and product search by name/category tested; product creation/update used by order tests. Negative/update boundary coverage limited. |
| Orders/coupons/fulfilment | Completed and verified with simulated payments | Placement, snapshots/totals, sequential replay, insufficient stock, approve/decline/uncertain outcomes, coupon, cancel/restock, fulfilment tested. Not live wallet integration. |
| Carts/maintenance | Completed and verified for selected service paths | Add/update/checkout/clear, reservation expiry and abandoned-cart deletion tested. Job scheduling and retry races unverified. |
| Wallet payment adapter | Implemented but not fully verified | Three mocked HTTP tests verify charge, insufficient-balance decline and refund. No live commerce -> wallet -> MySQL check. Default main configuration selects wallet; tests select simulated. |
| Returns | Partially implemented | Request/approve/reject/complete/list exist; completion refunds and restocks. No ReturnService regression tests; repeat-return/authorization/concurrency risks remain. |
| Payment recovery | Partially implemented | Placement writes payment_outbox, request thread settles, /pay retries. Repository pending-work query has no worker caller. No durable refund work or ambiguous-payment reconciliation. |
| Notification inbox/deduplication | Completed and verified for in-process tests | Persists messages, deduplicates sequential (event type, order), lists by user. “SENT” means a row plus log-stub call, not delivered email. |
| Kafka pipeline | Partially implemented | Conditional producer/listener/config beans exist; both flags default false, Kafka auto-config excluded with custom configs provided. No Kafka-enabled tests or durable event publisher. |
| Real email delivery | Not yet implemented | Both enabled and disabled mail paths only log. Identity hides dev response tokens when mail.enabled=true without actually sending. |
| Gateway | Implemented but not fully verified | Routes/security/Redis limits/CORS/retries/circuit breakers/fallbacks/correlation/access log configured; only contextLoads tested. |
| Database lifecycle | Partially implemented | Separate schemas and JPA mappings exist; no versioned migrations or verified live schema baseline. |
| Deployment/operations | Partially implemented | Five Dockerfiles, shared infra Compose, only wallet in apps profile. No discovered CI pipeline or full-stack automated tests. |
| Wider target design | Not yet implemented in this repository | No frontend, dedicated integration/webhook worker, full double-entry ledger, durable event outbox relay, persistent signing-key rotation, real shipping/PSP connector, or deployment manifests beyond Compose/Dockerfiles. These are design gaps, not confirmed new requirements. |

## Progress by service

These summaries reuse the initial assessment; no service was re-analysed or changed by workflow setup. Update only affected sections after meaningful implementation, preserving other services and dated evidence. Detailed test results below are from the initial 2026-11-07 assessment.

### api-gateway

- Implemented: routing/security, Redis rate limits, resilience and correlation/access logging; context-load test passed and verify succeeded historically.
- Partial/pending: HTTP security/routing/filter/resilience integration verification; local deployment exposure review (R17/R18).
- Next: targeted edge contract/security checks when selected. No application files changed in setup.

### identity-service

- Verified service paths: registration/verification/login, sequential token rotation/logout/reset and organisation onboarding/guards. Other lifecycle/audit/cleanup paths need fuller verification.
- Partial/blockers: log-only mail, key/session/membership concurrency and claims limitations (R7-R9/R15-R16); 13 tests passed historically, packaging failed with OneDrive provider error.
- Next: restore packaging baseline; address identity security policy/guards when selected. No application files changed in setup.

### wallet-service

- Verified service paths: balance/history, sequential movement replay, transfer ownership/currency and lifecycle; 14 tests and verify passed historically.
- Partial/pending: replay scope/transaction-race handling, HTTP authorization and MySQL concurrency (R2/R11/R18); commerce money contract (R12).
- Next: scoped idempotency/request matching regressions with commerce. No application files changed in setup.

### commerce-service

- Verified selected H2/simulator paths: catalogue/search, cart checkout, order/payment outcomes, coupon/cancel/fulfilment and maintenance; wallet adapter has three mocked HTTP checks.
- Partial/blockers: payment recovery/expiry races, replay scope, returns/staff refund authorization, monetary precision and cart retries (R1/R3-R6/R10/R12-R13). Historically 16 tests passed; packaging failed with missing Plexus FileSet.
- Next: order replay scope/ownership fixes with wallet; packaging restoration separately. No application files changed in setup.

### notification-service

- Verified in-process persistence/inbox and sequential dedupe; historically 3 tests passed and 1 legacy test skipped.
- Partial/blockers: Kafka disabled by default, no reliable event/delivery recovery or real mail; duplicate race risk (R7/R10-R11). Packaging failed with missing Plexus FileSet.
- Next: producer/consumer contract and delivery verification when selected, after defining delivery guarantees. No application files changed in setup.

## Verification performed

Command in **each** service: `mvn -B -o verify`, Maven **3.9.16**, installed Temurin **21.0.12.1+1-LTS**, Windows 11; completed approximately 10:15–10:16 IST on 2026-11-07. Java was selected per process; default shell Java 8 was unchanged. These were incremental builds, not clean rebuilds; wrappers were not used.

| Service | Tests discovered / passed / skipped | Failures / errors | Verify/package result |
|---|---|---|---|
| api-gateway | 1 / 1 / 0 | 0 / 0 | SUCCESS; executable JAR repackaged |
| identity-service | 13 / 13 / 0 | 0 / 0 | FAILED at jar:3.4.2: “The cloud file provider is not running” |
| wallet-service | 14 / 14 / 0 | 0 / 0 | SUCCESS; executable JAR repackaged |
| commerce-service | 16 / 16 / 0 | 0 / 0 | FAILED at jar:3.4.2: missing org/codehaus/plexus/archiver/FileSet |
| notification-service | 4 / 3 / 1 | 0 / 0 | FAILED at jar:3.4.2: missing org/codehaus/plexus/archiver/FileSet |

**47 passed, 1 skipped, 0 test failures/errors in the final runs.** The skipped notificationservice legacy test is intentionally disabled. Evidence: each service's ignored `target/surefire-reports/*.txt` and `TEST-*.xml`; these reports are generated and not a permanent tracked artifact.

Initial sandboxed wallet run had 14 test initialization errors: Mockito could not create a temporary Java agent file (“Access is denied”). Approved outside-sandbox reruns resolved that test-environment failure. Packaging failures above remain unresolved; the missing plugin class is a local build/plugin-loading symptom, and its underlying cause has not been established.

`docker compose config --quiet` succeeded. No images built, containers started, live APIs called, personal MySQL data inspected, Redis rate-limit scenarios exercised, or Kafka/SMTP delivery tested. H2 is not MySQL concurrency/schema evidence. Identity service tests call services directly and do not exercise the Redis auth filter. Gateway test only boots context.

## Issue and risk register

Source findings below are **not reproduced exploits or concurrency tests**.

| ID | Priority | Evidence and consequence |
|---|---|---|
| R1 | High | OrderPlacementService looks up idempotency key globally and returns stored orderId after checking only the newly requested organisation. It does not bind replay to original customer, organisation or payload. A key collision can return another order and invoke its settlement. |
| R2 | High | WalletService deposit/withdraw replay checks ownership of requested wallet, then returns any history row with that global key without checking row wallet/type/amount. Transfer replay checks stored sender ownership but not requested recipient/amount. Wrong-operation/data replay is possible. Stable commerce-pay/refund keys also share this client-accessible namespace. |
| R3 | High | Order, PaymentOutbox and ReturnRequest have no @Version/row locks. Settlement/cancel/expiry/return finalization can race; CommerceOrder.transitionTo simply assigns. An ambiguous debit may be expired/cancelled with stock release and no reconciliation/refund. Successful wallet HTTP followed by DB failure is not recovered automatically. |
| R4 | High | Repeated returns can be created for a still-FULFILLED order; completing a return does not change order status or enforce one return per order. Multiple completions can restock repeatedly while refund key is shared per order. Concurrent completion is also unguarded. |
| R5 | High / policy | OrgRole.MEMBER has commerce:write. Fulfilment and return approve/reject/complete check organisation membership, not a separate staff role/customer restriction. Any member can perform these operations. Intended staff/customer policy is unknown. |
| R6 | High / policy | Refund calls wallet with the completing caller's JWT; wallet enforces ownership. A staff member completing another customer's return cannot deposit into that customer's wallet. No service credential/refund authorization model exists. |
| R7 | High | Both mail classes are logging stubs, even when enabled. Identity logs raw one-time tokens; default APIs return dev tokens. NotificationRecord starts SENT before real delivery exists. |
| R8 | Medium | AuthService unions permissions across all orgs. Downstream checks separate flat permissions from orgIds, not permission-per-org. Claims remain stale until refresh/re-login/expiry; logout/password change revoke refresh tokens, not issued access JWTs. |
| R9 | Medium | RSA signing key/kid are generated per identity process. Restart changes keys; multiple replicas would issue mutually different keys. JWTs have no issuer/audience enforcement in these custom flows; JWKS HTTP clients have no explicit timeouts. |
| R10 | Medium | Kafka publisher calls send without handling completion, outside a durable event record. Some publish calls occur inside local DB transactions, ORDER_PAID after settlement commit. Crash/rollback/broker errors can lose or misorder business notifications. No configured DLT/recovery pipeline. |
| R11 | Medium | Wallet/notification unique-race catches query again inside an already failed/rollback-only transaction. They do not establish safe concurrent replay; no concurrency tests prove the advertised comments. |
| R12 | Medium | Commerce amounts are scale 4 (coupon calculation); wallet HTTP only accepts scale 2 and <=1,000,000.00. Fractional-minor-unit totals or large orders can be declined. Main order currency is hard-coded INR. |
| R13 | Medium | Cart checkout snapshots cart before idempotency replay, then clears separately regardless of payment outcome. Retry after successful clear can fail CART_EMPTY; concurrent edits may be removed. addItem checks increment against stock, not cumulative cart quantity. |
| R14 | Medium | Hibernate update has no migration history; same root DB credentials serve all schemas. Most references are scalar IDs without FKs. Most lists and maintenance batches are unbounded; no explicit query indexes beyond unique/PK mappings. |
| R15 | Medium | Identity Redis limiter increments and expires in separate commands and trusts first X-Forwarded-For. Redis errors lack an explicit local fallback/error policy. Proxy trust and a possible no-expiry failure window need testing. |
| R16 | Medium | Sequential refresh/reset/invite checks have no locking/version protection; old outstanding invite tokens are not invalidated on reinvite and acceptance allows an already active membership. Last-admin guard is also a count-then-write race. |
| R17 | Operational | Only wallet runs through Compose apps profile; it needs host identity. External mysql-data/redis-data volumes must already exist. Kafka advertises localhost:9092, unsuitable for other-container clients without listener changes. Gateway allows wildcard CORS and public Prometheus; local credentials/ports need deployment-specific policy. |
| R18 | Verification | Identity/commerce/notification packaging not verified successfully. No controller/security/concurrency/real-infra regressions; existing target docs and identity role table contain claims beyond current code. |

## Important files

| Location (relative to repository) | Responsibility |
|---|---|
| */pom.xml; */src/main/resources/application.* | Independent dependencies/builds and default service configuration |
| */src/test/resources/application.properties | H2, simulated payment and Kafka-disabled test configuration; gateway has wiring properties |
| api-gateway/.../config, security, ratelimit, filter, error; application.yml | Edge route/filter/security/resilience pipeline |
| identity-service/.../service/AuthService.java; OrgService.java | Account/token lifecycle, memberships and scoped identity authorization |
| identity-service/.../security/JwtService.java; TokenCrypto.java; RateLimitService.java | RSA/JWKS, opaque-token hashing, Redis rate limit |
| identity-service/.../service/AuditService.java; TokenCleanupJob.java | Identity-local audit and hourly token purge |
| wallet-service/.../service/WalletService.java; entity/Wallet.java | Balance authority, local movement transactions, ownership, optimistic lock |
| commerce-service/.../service/OrderPlacementService.java | Atomic stock reservation/order/idempotency/payment-work creation |
| commerce-service/.../service/CommerceService.java; PaymentSettlementService.java; OrderCancelService.java | Order orchestration, external payment boundary and follow-up DB updates |
| commerce-service/.../service/CatalogueService.java; CartService.java; ReturnService.java | Catalogue/search, carts/checkout, returns/refunds |
| commerce-service/.../payment; event; job/CommerceMaintenanceJobs.java | Payment adapters, direct event publication and maintenance scheduling |
| notification-service/.../kafka; service/NotificationDispatcher.java; mail/MailChannel.java | Conditional consumption, persisted inbox/dedupe and log delivery |
| DATABASE_DESIGN.md | All mapped tables, constraints, ownership and known persistence gaps |
| docker-compose.yml; */Dockerfile; finflow-env.sh | Local infrastructure, image definitions and Java 21 shell setup |
| */.mvn/maven.config; */maven-settings.xml; wallet-service/scripts/reset-local-db.sql | Local Maven repository overrides (not uniform across services); destructive local wallet data reset, not executed |
| postman; */postman | Collections, shared local environment, Python generators; not executed checks |
| docs/02-hld; docs/03-lld; docs/SystemDesign; wallet-service/docs | Existing target architecture and learning/reference material; preserved |

In this table `.../` expands to `src/main/java/com/finflow/<service>/` (gateway uses `apigateway`).

## Recommended next development task

**Fix and prove idempotency scope/ownership first (R1/R2)** before adding features. Define replay identity as caller + operation + resource/organisation + canonical request payload; reject mismatches; authorize the stored result before returning it or invoking settlement. Add regressions for two users/two organisations, same key with changed amount/type/recipient, reserved commerce payment-key collisions, and concurrent identical requests against MySQL. Exact schema/key strategy needs review before implementation.

Separately restore a reproducible packaging baseline for the three failed modules without changing application design. Then address payment/expiry/cancel reconciliation and returns (R3–R6), followed by HTTP authorization tests, durable event/delivery recovery and migration baselining. These are recommendations awaiting review, not accepted requirements.

## Resume checklist

- Re-read this file and inspect git diff; no application fixes were made during the assessment.
- Confirm intended customer/staff permissions, user-owned versus tenant-owned wallet policy, currency/rounding policy and real versus learning deployment scope.
- Resolve local packaging symptoms and rerun affected checks; preserve the dated evidence above.
- Choose one issue, trace its implementation and add narrowly targeted behaviour tests before implementation.
- Update this file with changed status, actual commands/results and the next task; keep unknowns explicit.
