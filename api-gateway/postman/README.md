# API Gateway (production-style)

Port: **18080** · env: `gatewayBaseUrl`

## Capabilities

| Concern | Tech |
|--------|------|
| Routing | Spring Cloud Gateway (WebFlux) |
| Auth | OAuth2 Resource Server + identity JWKS |
| Rate limit | **Redis** (shared docker — start infrastructure compose first) |
| Resilience | Resilience4j circuit breaker + fallback JSON |
| Retry | GET only (never POST money/auth) |
| Timeouts | Connect + response timeouts |
| Body limit | RequestSize filter |
| CORS | Configurable origins |
| Observability | Actuator + Prometheus + correlation id + access logs |
| Errors | Stable JSON (`code` / `message` / `status`) |

## Postman

Import `FinFlow-API-Gateway.postman_collection.json` + shared **FinFlow Local** env.

Smoke folders: JWKS / me / wallet-by-user / commerce list / notifications / actuator.

Deep money & catalogue flows live in the service collections (`walletBaseUrl`, `commerceBaseUrl`, …).

## Local run

```bash
cd infrastructure && docker compose up -d   # MySQL :3307 + Redis :6379 (+ Kafka)
# Identity 18081, Wallet 18082, Commerce 18083, Notification 18084, Gateway 18080
```

Call via `http://localhost:18080/...` with Bearer token.

Prometheus: `GET http://localhost:18080/actuator/prometheus`
