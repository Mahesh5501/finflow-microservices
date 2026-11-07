# Gateway Postman checks

This collection tests gateway behavior, not every business scenario in all services. It contains **52 requests with assertions**, grouped by the fixtures they require. Passing these checks does not establish production readiness.

## Start in Postman

1. Import [the collection](FinFlow-API-Gateway.postman_collection.json). Optionally import the shared [FinFlow Local environment](../../postman/FinFlow-Local.postman_environment.json).
2. Start the gateway and set `gatewayBaseUrl` (default `http://localhost:18080`). Do not start/reset shared infrastructure just to run the basic checks.
3. For wallet-only testing, open **02 Live routing** and send **Wallet by User**, **Wallet by ID**, or **Wallet history** individually. There is no live-routing enable switch. For gateway-only checks, run folder 01. Running the whole collection now also runs live routing, so unrelated stopped services will fail.
4. Folders 03-06 still skip until explicitly enabled; prepare their special fixtures first. Environment values override collection defaults; check for stale values in your selected environment. Keep real tokens local and out of exported/committed files.

| Folder | Requests | Enable with | Purpose |
|---|---:|---|---|
| 01 Gateway checks | 24 | Always enabled | Info/probes, missing routes, direct fallback, malformed/missing JWT, fixed-length body limit, CORS preflight |
| 02 Live routing | 14 | No switch; send the desired request | Actual JWKS, identity, wallet, commerce, notification routing, dependency health, current Prometheus exposure |
| 03 Auth and ownership fixtures | 5 | `gatewayRunAuthCases=true` | Expired/tampered JWT, missing permission, another user's wallet, another organisation |
| 04 Controlled downstream faults | 3 | `gatewayRunFaults=true` | Unavailable identity fallback, GET 500 retries, POST 500 without retry |
| 05 Isolated rate-limit fixture | 1 | `gatewayRunRateLimit=true` | Twenty concurrent read-only JWKS calls; expect both success and 429 |
| 06 Known production gaps | 5 | `gatewayRunKnownGaps=true` | Deliberately failing checks for proposed production behavior |

Every request checks its expected status. Applicable cases also check response fields, error codes, ownership, correlation headers or CORS headers. A skipped case is **not verified**. Enabling a group without required variables produces errors; do not treat that as an application verdict.

## Fixtures and expected behavior

**Wallet-only via gateway:** run the gateway and wallet, with identity/JWKS available to validate the token and the wallet database available. Set `gatewayBaseUrl`, `accessToken`, `userId` and `walletId`; use a token with `wallets:read`. Send just the three wallet requests listed above. No `orgId` or running commerce/notification service is needed for those requests. You may delete any old `gatewayRunLive` environment variable; it is no longer read. Missing required fixture values still produce an explicit pre-request error.

**Full live-routing folder:** run identity, wallet, commerce, notification and Redis. Set `accessToken`, `userId`, `orgId`, and `walletId` for the same user and accessible organisation. The token needs the relevant wallet/commerce read permissions. Prepare these using existing service collections; refresh login after membership changes. Aggregate health must be UP: a 503 is a failure, not an accepted healthy response. The Prometheus 200 check records current local behavior; the separate production-gap check challenges its anonymous exposure.

**Authentication/ownership:** additionally set `expiredAccessToken` to a genuinely expired signed identity token, `noPermissionAccessToken` to a valid unexpired token lacking `wallets:read`, `walletId2` to another user's wallet, and `otherOrgId` to an organisation outside the caller's memberships. The normal `accessToken` must have permissions, so a 403 tests ownership rather than simply missing permissions. Signature tampering is generated from that token. These cases need running downstream services and correct fixtures; they are not simulated authorization checks.

**Controlled faults:** use separate temporary gateway instances and an isolated mock backend. Set `gatewayUnavailableBaseUrl` to a gateway whose `identity.uri` points to an unused loopback port. Set `gatewayHttp500BaseUrl` to another gateway whose identity backend always responds HTTP 500. That mock must handle both GET and POST and consume request bodies. Check its request log: one collection GET to JWKS should reach the backend **three times**; one POST to `/api/v1/auth/login` with `{}` should reach it **once**. Response assertions alone cannot prove attempt counts. Do not point the POST fixture at a real login backend or stop shared services to manufacture a fault. The direct fallback check in folder 01 only tests the handler, not a circuit opening.

**Rate limit:** set `gatewayRateBaseUrl` to an isolated gateway with reachable Redis, a fast successful JWKS backend and the JWKS route configured with `replenishRate=1`, `burstCapacity=1`. Avoid competing traffic; allow its bucket to refill first. The request's script sends 20 JWKS reads and requires some 200s and some 429s, with no transport errors. This is a small fixture check, not a load test. Redis unavailable/fail-open behavior cannot prove working quotas.

**Known production gaps:** these five requests express proposed targets, not implemented fixes or a final deployment policy. They expect anonymous metrics/Prometheus rejection, correlation and a Bearer challenge on protected 401 responses, and JSON `PAYLOAD_TOO_LARGE` on 413. Current code fails these expectations. Keep failures visible; do not weaken assertions to make the report green. Agree the actual management access policy before implementing it.

The body-limit case generates 1 MiB + 1 byte of invalid JSON, so it cannot register a user. Let Postman calculate Content-Length. It tests declared-length rejection only; it does not cover chunked/streaming enforcement. No business money writes or automatic account setup run in the gateway-only group.

## Command-line and regeneration

Postman's collection runner is sufficient. The following pinned Newman version was used for local verification; it is an optional tool, not a project dependency. `npx` may download it when absent from the local cache.

```powershell
npx --yes --package=newman@6.2.1 newman run api-gateway/postman/FinFlow-API-Gateway.postman_collection.json --env-var gatewayBaseUrl=http://localhost:18080
```

Run from the repository root. Add `--folder "04 Controlled downstream faults"` and the required `--env-var key=value` flags/URLs to select a prepared fixture. Avoid putting real tokens in shell history; use a local uncommitted environment instead. Inspect both failures and skipped cases; never discard a failing exit code.

The collection is generated by `gateway_collection()` in [the existing generator](../../postman/_gen_extra_collections.py). Make persistent case/assertion changes there and regenerate only the gateway:

```powershell
python postman/_gen_extra_collections.py --gateway-only
```

This option preserves other service collections and the shared environment. Running the generator without it retains the existing broader regeneration behavior.

## Last executed checks: 2026-11-07

- Before removing the live-routing switch, temporary loopback gateway default run: **24 HTTP requests, 94 assertions, zero failures**; 28 optional requests skipped.
- Separate unavailable/HTTP-500 identity fixtures: **3 requests, 10 assertions, zero failures**. Mock log independently confirmed **3 GET attempts and 1 POST attempt**.
- Known-gap folder: **5 requests, 13 assertions, 7 failures**, expected for current implementation. Failures include public metrics/Prometheus, missing 401 headers and the empty non-JSON 413 response. Several assertions can fail on the same request.
- Live routing (14), real-token/ownership cases (5), and Redis quota fixture (1): **not executed**. These need prepared services, users/tokens and Redis.
- Generator syntax, generated JavaScript syntax, assertion presence and deterministic regeneration checked. Shared environment and other collections preserved. Application Maven tests were not rerun for this collection-only task; earlier Java verification is recorded in [project status](../../PROJECT_STATUS.md).

Local Newman JSON reports and temporary process logs are ignored under `api-gateway/target/gateway-postman-*`. Temporary gateway/mock processes were stopped after verification. These results are isolated local checks, not production validation.

## Coverage still needed

This collection does not cover every CRUD/business flow, money replay/concurrency, all invalid issuer/audience/key-rotation combinations, streaming/chunked limits, the full circuit-breaker lifecycle, Redis recovery, sustained load, TLS/proxy trust or deployment security. Use service collections for business workflows; in a copied local environment, point the relevant service base URL to the gateway to exercise them through routing. Do not change the shared committed environment just for one run.

See the [gateway review](../../docs/GATEWAY_REVIEW.md) for existing production risks. The next recommended gateway task is agreeing management-endpoint access policy and adding HTTP security regressions before implementing that policy.

Postman references: [test scripts](https://learning.postman.com/docs/tests-and-scripts/write-scripts/test-scripts), [skipRequest semantics](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-execution), [Newman CLI](https://learning.postman.com/docs/reference/newman-cli/command-line-integration-with-newman/).
