# API gateway implementation and production-readiness review

Reviewed **2026-11-07 (Asia/Calcutta)** at HEAD `09f5206` (agents added); gateway source is unchanged from `5d79a90`. Scope: api-gateway only, with existing dependency/architecture documentation. CodeGraph was used before tracing code; no repeat full-repository assessment. Application source, POMs and configurations were not modified.

## Verdict

**A functional learning/local-development gateway with production-oriented building blocks; not ready for public production deployment in its current configuration.**

The implementation correctly uses reactive Spring Cloud Gateway, resource-server JWT verification, static service routes, GET-only retries, Redis token buckets, circuit breakers, graceful shutdown, Actuator and a non-root image. However, runtime checks exposed public operational metrics, a streaming body-limit bypass, missing correlation/error coverage, and ineffective nested configuration validation. Its only committed automated test loads the Spring context.

Production readiness is demonstrated security, bounded resource use, predictable failure behaviour and operations under the intended deployment topology. Feature names and successful startup do not establish those properties.

## Verification and limits

Command in api-gateway: `mvn -B -o verify`, installed Maven 3.9.16 with per-process Temurin Java 21. **BUILD SUCCESS; 1 test run, 0 failures/errors/skips** (contextLoads), at approximately 10:51 IST. Incremental build; Maven wrapper and Docker image were not executed.

Inspected the rebuilt executable JAR: Boot 3.5.6, Gateway 4.3.5, Cloud Commons 4.3.3, Security 6.5.5, Framework/WebFlux 6.2.11, Reactor Netty HTTP 1.2.10 and Netty HTTP codec 4.1.127.Final. These are actual packaged versions, not inferred from starter names.

Two temporary gateway processes were run on unused loopback ports and stopped. The first used packaged defaults except bind address/port. The second used an in-process local mock identity backend, deliberately unavailable Redis port and negative nested CORS max-age override. No real business services, databases or containers were started or mutated.

**14 HTTP probes plus a negative nested-property startup check** were performed. These are ad hoc observations, not committed regression tests or successful business flows. Generated evidence (ignored, not permanent): api-gateway/target/gateway-review-runtime.log, gateway-review-probes.json, gateway-review-mock.log, gateway-review-mock-probes.json and surefire-reports.

| Probe | Observed result |
|---|---|
| Anonymous GET /actuator/metrics | 200; metric names available |
| Anonymous GET /actuator/prometheus | 200; scrape body available |
| GET /actuator/gateway/routes | 404; management route endpoint not enabled by default |
| GET protected wallet path without JWT | 401 JSON; no X-Correlation-Id |
| GET protected wallet path with malformed JWT | 401 JSON; no X-Correlation-Id |
| GET /fallback/wallet directly | 503 JSON; no X-Correlation-Id |
| GET unmatched path | 404 JSON; no X-Correlation-Id |
| OPTIONS order route from https://untrusted.example | 200; Access-Control-Allow-Origin reflects that origin |
| Liveness and readiness endpoints | Both 200 UP; not proof of usable downstream/JWKS/Redis |
| Mock GET JWKS returning 500, Redis unavailable | Backend called 3 times; gateway returned 500, empty body; traffic still forwarded |
| 2 MiB fixed-length POST to mock registration | 413, empty body, no backend call |
| 2 MiB chunked POST to mock registration | 200 from mock; backend received all 2,097,152 bytes despite 1 MB configured limit |
| Browser-origin request rejected for missing JWT | 401 without Allow-Origin, correlation ID or WWW-Authenticate |
| Startup with app.cors.max-age-seconds=-1 | Started despite nested @Min(0) |

Not verified: valid/expired/wrong-issuer/audience JWTs with a real identity, live Redis quotas/distributed load, full circuit-open/half-open cycles, downstream timeout/client-disconnect races, TLS/load-balancer deployment, container image build, vulnerability scan, sustained performance, or real wallet/commerce integration. No certification or exploit claim beyond the stated local probes.

## Request path and why it matters

1. WebFlux receives request; Spring Security authenticates/authorizes before gateway route GlobalFilters run.
2. CORS preflight can terminate without routing. Local Actuator/fallback handlers also do not traverse gateway route GlobalFilters.
3. A matched route combines default/route filters and GlobalFilters by order.
4. CorrelationIdFilter adds an ID at route entry. DedupeResponseHeader, RequestSize and RequestRateLimiter apply.
5. Route CircuitBreaker wraps Retry and downstream routing. Retry reruns later filters, including the low-priority AccessLogFilter.
6. Response/error flows back; framework-written 413/429 or downstream error responses are not necessarily exceptions and therefore need not pass GlobalErrorConfig.

This explains why auth/management/404 responses lack the route correlation ID, and why the mock failing GET produced **three access-log entries for one client request**. Fixed-length 413 and missing-JWT requests produced no custom access-log line.

## Prioritized findings and improvements

### GW1 - High: public operational endpoints and permissive catch-all

Evidence: security/SecurityConfig.java:44-65; application.properties:40-44. Anonymous metrics/Prometheus 200 reproduced. anyExchange().permitAll() allows non-API mappings by default; explicit permitAll for Prometheus also exists.

Improve: define explicit public health/auth endpoints and private operational access; deny unmatched endpoints; restrict operational network/port or credentials as deployment requires. Test access rules before enabling more endpoints. **Do not claim public route mutation:** /actuator/gateway/routes currently returns 404, and the resolved Gateway version defaults its actuator endpoint to disabled. If enabled later, use read-only access and authorization. [Gateway actuator guidance](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/actuator-api.html).

### GW2 - High: configured body limit is incomplete

Evidence: application.yml:9-11; GatewayProperties.java:18-19; application.properties:31-32. YAML hard-codes 1MB, so changing app.max-body-bytes does not change RequestSize. The filter checks Content-Length, not streamed byte totals; the 2 MiB chunked probe reached the backend.

Improve: one authoritative limit; enforce bounded body handling for unknown/chunked lengths at the intended ingress/gateway layer, including cancellation and buffer release. Add fixed/chunked/absent-length boundary tests. Do not simply buffer arbitrary bodies into memory. Request limits and connection/header/timeout limits protect different resources. [Version-matched filter source](https://raw.githubusercontent.com/spring-cloud/spring-cloud-gateway/v4.3.5/spring-cloud-gateway-server/src/main/java/org/springframework/cloud/gateway/filter/factory/RequestSizeGatewayFilterFactory.java).

### GW3 - High: production version/support policy is missing

POM pins Boot 3.5.6 with Cloud 2025.0.3. Their release families are aligned, but the Cloud release was based on Boot 3.5.15; a bootable combination is not a current patch/support guarantee. Official Spring announcement states Cloud 2025.0.x open-source support ended 2026-06-30, before this review date. Determine supported OSS/commercial policy, align tested dependencies and run dependency/image scanning through an approved upgrade task. No particular exploitable CVE was established here. Do not blindly upgrade majors or edit dependencies during review. [Official release/support statement](https://spring.io/blog/2026/06/11/spring-cloud-2025-0-3-aka-northfields-has-been-released/).

### GW4 - High before deployment: JWT trust contract is incomplete

Evidence: application.properties:9 and SecurityConfig.java:67-69. A trusted JWKS endpoint verifies signatures, but only jwk-set-uri is configured; issuer/audience requirements are not established. Gateway authenticates API callers; downstream services currently own permission/tenant/resource checks.

Improve: agree issuer/audience/token lifetime/key-rotation contract with identity, then validate it with correctly signed but wrong-issuer/audience tokens as well as missing/expired tokens. Current identity documentation records process-local keys and no issuer/audience claims, so this requires a coordinated change; adding gateway issuer validation alone would break current tokens. Use HTTPS/controlled internal trust in deployment. Optional coarse route permissions require a converter for FinFlow permissions claims; default scope authorities are not those custom permissions. Do not move business ownership checks into the gateway. [Spring Security JWT validation](https://docs.spring.io/spring-security/reference/6.5/reactive/oauth2/resource-server/jwt.html).

### GW5 - Medium/high: rate-limit failure and quota semantics are implicit

RateLimitConfig.java:18-33 uses subject or socket IP, all routes share the same numerical settings. Framework Redis error handling deliberately allows requests; mock traffic with unavailable Redis reproduced forwarding. Redis keys include routeId as well as resolved identity, so the configured rate is **per route and caller**, not a universal whole-gateway user limit. Public auth paths share their route bucket; invalid JWTs fail before rate limiting.

Improve: explicitly choose fail-open versus fail-closed/degraded policy by route risk, emit alerts, apply tighter auth limits and appropriate client/user/tenant quotas only when business policy requires them. Define trusted-proxy/client-IP handling; behind a proxy many callers may share the peer IP. Test spoofed forwarding headers, shared NAT and unavailable Redis. Do not trust arbitrary X-Forwarded-For or count Redis as guaranteed abuse protection. [Redis error behaviour and key source](https://raw.githubusercontent.com/spring-cloud/spring-cloud-gateway/v4.3.5/spring-cloud-gateway-server/src/main/java/org/springframework/cloud/gateway/filter/ratelimit/RedisRateLimiter.java), [proxy trust configuration](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/httpheadersfilters.html).

### GW6 - Medium/high: retry/circuit-breaker policy does not match a narrow reading of YAML

YAML lists retry statuses 502/503/504 but leaves the default 5xx series intact. Mock HTTP 500 caused 3 backend calls. Retry is limited to GET, which is appropriate; preserve the absence of automatic POST money/auth retry.

CircuitBreaker has no statusCodes configured. Source establishes that returned HTTP error statuses are not automatically classified as exceptions; after retries a 500 can be treated as a completed call rather than a breaker failure. Only a single failing logical request was runtime-probed; repeated circuit opening was not tested.

Improve: explicitly set retry status series/exceptions and breaker status policy, distinguish business 4xx from transient failures, align total deadline with per-attempt timeouts/backoff, and test open/half-open/recovery. Do not present every 503 for a timed-out money operation as proof it did not execute downstream. Consider jitter/load isolation after measuring need. [Retry defaults](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/gatewayfilter-factories/retry-factory.html), [breaker HTTP status policy](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/gatewayfilter-factories/circuitbreaker-filter-factory.html).

### GW7 - Medium: correlation and access logs cover only part of traffic

CorrelationIdFilter.java:23-34 is route-only; AccessLogFilter.java:23-49 is late inside the retry chain. Missing IDs and 3 per-attempt logs reproduced. System.currentTimeMillis measures wall-clock time; cancellation is not handled with doFinally. Caller-supplied correlation ID is accepted without format/length validation.

Improve: establish bounded/sanitized ID and final request log at an early WebFilter layer, before security, with deliberate exception/final-status timing. Use a monotonic duration source, distinguish client request versus downstream attempt, handle cancellation and include routeId and trace/span context. Keep bodies/tokens out of logs and avoid raw-path metric labels. End-to-end trace export/collector is not supplied merely by including Brave. [GlobalFilter ordering](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/global-filters.html).

### GW8 - Medium: nested property validation is not cascaded

GatewayProperties.java:14-16 has nested objects without @Valid, despite child @Min/@NotEmpty. Runtime started with nested CORS maxAge=-1. Root maxBodyBytes validation is separate and does not prove nested validation.

Improve: cascade nested validation, validate relationships such as token cost versus capacity and coherent deadlines, and add binding/fail-fast tests. Bind size enforcement to its property. [Spring Boot nested validation](https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html).

### GW9 - Medium: CORS and error responses are inconsistent

GatewayConfig.java:15-24 and properties permit any origin/method/header policy; untrusted-origin preflight reflected successfully. This does not bypass JWT authorization, but should be a conscious policy for the actual client.

The browser-origin 401 lacked CORS headers, so a browser may hide the JSON error. Custom 401 also omitted WWW-Authenticate. GlobalErrorConfig does not wrap downstream responses or direct filter responses; fixed-length 413 and mock backend 500 had empty bodies. Fallback is a separate JSON shape; public direct fallback is callable.

Improve: configure trusted frontend origins and required/exposed headers, place CORS appropriately before auth rejection, and define a consistent edge error/challenge/correlation contract for 401/403/413/429/503/timeouts. Preserve downstream business errors; do not buffer/rewrite every arbitrary response. Restrict externally callable fallback paths while preserving internal dispatch if policy requires it. Add explicit tests.

### GW10 - Medium: error classification and deployment safeguards need evidence

GlobalErrorConfig.java:79-90 recognizes only ResponseStatusException and java.util.concurrent.TimeoutException (top/immediate cause). Other transport/Netty errors may become generic 500 if not handled by fallback; these are source concerns, not all reproduced. It exposes ResponseStatusException reason and logs toString rather than full exception stack for 5xx.

Deployment settings are localhost HTTP, wildcard CORS, shared Redis without auth/TLS in local Compose, no gateway Compose app or deployment manifests, no explicit connection-pool/pending-request/idle/header/slow-client limits. Framework defaults do exist; this review does not claim no limits. Determine TLS termination, trust boundary, private backend reachability, resources, probes, rolling shutdown and alerts with the real infrastructure. Redis/downstream inclusion in readiness must follow the chosen availability policy; making all dependencies part of liveness causes restart loops.

## File-by-file disposition

All **25 Git-tracked gateway files**, plus ignored HELP.md, were read/reviewed. Excludes generated target artifacts and local IDE metadata; artifacts were inspected only as verification evidence. Paths below are relative to api-gateway.

| File | Responsibility / verdict |
|---|---|
| src/main/java/com/finflow/apigateway/ApiGatewayApplication.java | Correct minimal bootstrap. Comment “production-style” is intent, not evidence; no business logic should be added here. |
| src/main/java/com/finflow/apigateway/security/SecurityConfig.java | Appropriate resource-server/disabled form/basic setup; GW1/GW4/GW9. CSRF disabling fits bearer-header API assumptions; reconsider if cookie authentication is introduced. |
| src/main/java/com/finflow/apigateway/config/GatewayConfig.java | Centralized CORS; GW9 ordering/origin policy. |
| src/main/java/com/finflow/apigateway/config/GatewayProperties.java | Useful typed properties; GW2/GW8 unused size/cascaded validation. |
| src/main/java/com/finflow/apigateway/ratelimit/RateLimitConfig.java | Nonblocking key resolver/Redis limiter; GW5 policy/proxy/quota gaps. |
| src/main/java/com/finflow/apigateway/filter/CorrelationIdFilter.java | Correct route ID propagation; GW7 pre-security scope, validation and browser exposure gaps. |
| src/main/java/com/finflow/apigateway/filter/AccessLogFilter.java | Avoids logging bodies; GW7 attempts versus final-request timing/cancellation. |
| src/main/java/com/finflow/apigateway/error/GlobalErrorConfig.java | Central reactive exception writer with committed-response guard; GW9/GW10 response coverage/classification limitations. |
| src/main/java/com/finflow/apigateway/fallback/FallbackController.java | Simple 503 JSON fallback; GW9 public invocation and inconsistent envelope, GW6 ambiguous operation outcome. |
| src/main/resources/application.properties | Clear local ports, shutdown and client settings; GW1-GW5/GW9/GW10. No production overrides/profile found in tracked gateway files. |
| src/main/resources/application.yml | Correct route families and separate users/me versus users/*/wallet mappings; GW2/GW6/GW10. Six largely repeated filter blocks risk configuration drift; consolidate only after tests justify a simple approach. |
| src/test/java/com/finflow/apigateway/ApiGatewayApplicationTests.java | One empty context-load test, passed; no assertions about auth/routes/failures/limits. Biggest verification gap. |
| src/test/resources/application.properties | Supplies wiring only, does not replace production defaults with real test doubles. No real-JWT/Redis route checks. |
| pom.xml | Correct reactive starters and managed dependencies; GW3 support/patch alignment, no project-level security/contract/load verification pipeline. Do not invent a need for JPA/Kafka in the edge. |
| Dockerfile | Multi-stage JDK21 build, non-root uid10001 runtime and memory percentage are useful. Build skips tests; image tags float, no digest pin or demonstrated scanning/SBOM/runtime resource/probe deployment. Image not built in review. |
| .dockerignore | Excludes generated/IDE files, tests, wrappers and local Maven settings. Smaller context, but image build does not test or use wrapper/local mirror. |
| .mvn/maven.config | Automatically selects maven-settings.xml for local commands. |
| maven-settings.xml | Routes all repositories to Maven Central. Fine for current public dependencies; revisit if private artifacts are introduced; don't embed credentials. |
| .mvn/wrapper/maven-wrapper.properties | Wrapper 3.3.4/Maven 3.10.0 configured, no distributionSha256Sum; wrapper not executed. Align local/CI/Docker Maven choice and pin checksum if adopting it. |
| mvnw | Reviewed Unix wrapper download/cache/checksum paths; no gateway logic. Executes configured distribution; optional checksum is not configured. Not executed. |
| mvnw.cmd | Reviewed Windows batch/PowerShell wrapper with corresponding download/cache/checksum paths; same caveats. Not executed. |
| .gitignore | Appropriate generated/IDE exclusions; HELP.md intentionally ignored. No runtime behaviour. |
| .gitattributes | LF for Unix wrapper/CRLF for .cmd; suitable cross-platform intent. |
| postman/README.md | Useful manual smoke instructions; “production-style” and stable-error claims need the qualifications above. |
| postman/FinFlow-API-Gateway.postman_collection.json | Ten manual GET requests, no test/assertion scripts; cannot substantiate production readiness. Not executed as Postman. |
| HELP.md (ignored) | Stale generated references to Boot 4.1.1 and Gateway MVC conflict with actual Boot 3.5.6/WebFlux module. Correct or omit misleading references in an approved docs cleanup. |

## What to implement, in order

1. **First focused task: management endpoint security with regression tests.** Define public health endpoints and private metrics policy; test anonymous/authenticated/unauthorized management access and unknown paths. Important files: SecurityConfig, test source, management properties only if selected policy needs it. No database/API business-contract change. Management scraping integration may need credentials/private access. Propose precise policy and obtain approval before changes.
2. Fix streaming request limits and bind the size property; add content-length/chunked boundary tests.
3. Address configuration-validation gaps and CORS/security/correlation ordering; test auth rejection from a browser origin.
4. Establish explicit retry/breaker/Redis failure policy and test with controlled backends/Redis; preserve no POST retries.
5. Coordinate issuer/audience and durable key lifecycle with identity; update service dependency/contract docs only after an actual change.
6. Complete deployment/version support and observability checks, then realistic load/failure tests and CI gates.

Useful additions when justified: per-route auth abuse controls, private operational access, final-request/attempt metrics, timeouts/pool budgets, dependency/image scanning and contract tests. Conditional later: mTLS/service identity, ingress/WAF controls, API documentation aggregation, service discovery, caching of explicitly safe public GETs. None is automatically necessary. Avoid a gateway database, business rules, wallet ledger logic, generic “common framework,” dynamic route admin, or retries of money writes merely to add sophistication.

## Sources and completion

Framework links above were checked against official Spring documentation/source, using 4.3.x/6.5.x/3.5.x references where available. Source-level deductions and untested deployment assumptions are labeled separately from runtime observations. Review complete; suggested fixes are pending user review, not implemented.

## Cleanup audit - 2026-11-07

Reviewed all **28 currently tracked gateway files** at HEAD 79d18aa, plus the purpose of ignored HELP.md and generated/local directories. Earlier sections are dated review evidence; references there to the unused max-body setting describe the pre-cleanup version. No entire tracked file was established as unnecessary.

### Applied cleanup

- Removed GatewayProperties.maxBodyBytes, its accessors and app.max-body-bytes. They did not drive RequestSize; the active YAML 1MB limit is unchanged. No streaming-limit fix is claimed.
- Removed GatewayProperties.Http/getHttp and duplicate Java defaults/accessors. app.http.connect-timeout-ms/response-timeout-ms remain active YAML placeholders; the framework binds the resulting values to its own HttpClientProperties.
- Removed the test-only spring-security-test dependency: the current three tests use Boot Test, JUnit and Spring Security core types, not that artifact's helpers. Final Maven verification passed without it. Restore intentionally if later HTTP-security tests need those helpers.
- Retained all nine main classes and all active route/security/resilience/logging behavior. No runtime dependency was removed, no new dependency introduced and no tracked file deleted/renamed.

### Complete tracked-file disposition

| File relative to api-gateway | Evidence / disposition |
|---|---|
| .dockerignore | Keep: Docker build context exclusions; not Java code. |
| .gitattributes | Keep: Unix/Windows wrapper line-ending rules. |
| .gitignore | Keep: generated artifacts and local IDE metadata exclusions. |
| .mvn/maven.config | Keep: Maven automatically selects the service settings file. |
| .mvn/wrapper/maven-wrapper.properties | Keep: wrapper scripts read the distribution URL; wrapper metadata is not application code. |
| Dockerfile | Keep: multi-stage image build, non-root runtime and application entry point. No Docker build/run in this audit. |
| README.md | Keep/update: gateway learning guide, verified cleanup and remaining production tasks. |
| maven-settings.xml | Keep: selected by .mvn/maven.config; repository mirror for local Maven. |
| mvnw | Keep: optional Unix Maven entry point; download/cache/platform/checksum branches are wrapper infrastructure, not unused gateway logic. Not executed. |
| mvnw.cmd | Keep: Windows wrapper entry point; same distinction. Not executed or downloaded. |
| pom.xml | Keep/update: Java/Boot/Cloud, dependencies and executable packaging. Remove only unused spring-security-test. |
| postman/FinFlow-API-Gateway.postman_collection.json | Keep: 52 asserted cases; optional fixture folders are deliberate, not dead requests. All declared variables have references. |
| postman/README.md | Keep: import/setup, fixture switches, actual historical runs and coverage limits. |
| src/main/java/com/finflow/apigateway/ApiGatewayApplication.java | Keep: Spring Boot main, component scan and auto-configuration entry point. |
| src/main/java/com/finflow/apigateway/config/GatewayConfig.java | Keep: enables GatewayProperties and creates the browser CORS filter. |
| src/main/java/com/finflow/apigateway/config/GatewayProperties.java | Keep/trim: rate-limit and CORS bindings are consumed; remove unused maxBodyBytes and Http subtree only. |
| src/main/java/com/finflow/apigateway/error/GlobalErrorConfig.java | Keep: discovered ErrorWebExceptionHandler; private mapping/writing helpers and serialization/committed-response branches are used or defensive, with existing tests. |
| src/main/java/com/finflow/apigateway/fallback/FallbackController.java | Keep: four downstream circuit-breaker fallbackUri values forward here. No direct Java caller is required. |
| src/main/java/com/finflow/apigateway/filter/AccessLogFilter.java | Keep: Spring-discovered GlobalFilter/Ordered logs downstream attempts. |
| src/main/java/com/finflow/apigateway/filter/CorrelationIdFilter.java | Keep: Spring-discovered route GlobalFilter propagates/creates and echoes correlation IDs. |
| src/main/java/com/finflow/apigateway/ratelimit/RateLimitConfig.java | Keep: YAML SpEL resolves rateLimitKeyResolver and rateLimiter; private clientIp helper implements anonymous keys. |
| src/main/java/com/finflow/apigateway/security/SecurityConfig.java | Keep: supplies reactive security chain and JWT authentication/error callbacks; access-denial handling is defensive, not proof it should be deleted. |
| src/main/resources/application.properties | Keep/trim: downstream/JWKS, rate/CORS/Redis/timeouts/metrics/startup settings remain. Remove unused app.max-body-bytes. |
| src/main/resources/application.yml | Keep unchanged: six routes, their predicates, fallback/retry/filter order and resilience policies are active. |
| src/test/java/com/finflow/apigateway/ApiGatewayApplicationTests.java | Keep: Spring bean/context wiring smoke test; not an HTTP integration suite. |
| src/test/java/com/finflow/apigateway/error/GlobalErrorConfigTest.java | Keep: 11 status/error/timeout/correlation/serialization cases. |
| src/test/java/com/finflow/apigateway/ratelimit/RateLimitConfigTest.java | Keep: three principal/peer-IP/missing-address key cases. |
| src/test/resources/application.properties | Keep: test-specific JWKS/route/timeout settings; test resources override main properties, not the gateway YAML routes. |

### Dependencies and local artifacts

Runtime dependencies have framework/configuration uses: Gateway WebFlux routing/Netty; Security/OAuth2 RS256/JWKS; reactive Resilience4j breakers/time limits; Redis token buckets; Actuator/Prometheus/Brave observability; Jakarta validation/configuration binding. A transitive dependency or starter may not have a direct import; that does not make it unused. Explicit runtime declarations were retained. The known missing nested @Valid validation remains a separate behavior-changing fix, not an excuse to remove all validation support.

Ignored HELP.md is generated scaffolding with mismatched Boot/WebMVC links; use the tracked README instead. It is not built or versioned and was left untouched. Ignored target/ contains generated classes/JARs/reports/probe evidence, not source candidates; .idea/.vscode are local editor state and were not deleted. The packaged JAR was checked and contains no obsolete GatewayProperties$Http class. Wrapper distribution availability/download, checksum provisioning, Unix/Windows execution and Docker startup were not verified in this audit; wrappers remain useful optional entry points.

### Executed checks and limits

- Fresh `mvn -B -o -DskipTests compile` passed after cleanup. Initial file-edit helper used the wrong working-directory prefix and stopped before editing; corrected before compilation/verification evidence below.
- Temporary isolated gateway starts before and after the property cleanup produced identical parsed runtime snapshots: all six route definitions, predicates, fallback/retry arguments/order, default filters including RequestSize 1MB, HTTP timeouts 2000/5000 ms, rate 50/100/1 and CORS settings. All nine main application classes resolved as Spring beans. These were local startup/configuration checks, not downstream load/HTTP verification.
- Initial sandboxed verify hit Mockito temporary-agent initialization failure; approved unsandboxed verify passed. After the test-dependency removal, final Java21 `mvn -B -o verify`: **BUILD SUCCESS, 15 tests, zero failures/errors/skips**, executable JAR repackaged. Ignored logs: api-gateway/target/gateway-cleanup-final-verify.log and cleanup-before.log/cleanup-after.log. Temporary probe source/classpath stay ignored under target; startup processes closed after use.
- Collection structure checks retained all 52 request cases with test events, every collection variable has a reference, and all 79 JavaScript event scripts parsed. No Postman HTTP cases were rerun; opt-in fixtures are not passes.
- No source/config/policy changes in identity/wallet/commerce/notification. No live Redis/JWT/downstream, Docker, wrapper-download or load/security-policy rerun. Existing GW1-GW10 production gaps remain; GW2's unused-setting confusion is removed, while its streaming enforcement gap is unresolved.

Next recommended gateway task remains a concrete management-endpoint access policy plus HTTP security regressions, followed by bounded streaming-body enforcement. Cleanup alone does not establish production readiness.
