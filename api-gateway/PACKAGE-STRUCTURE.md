# API gateway — package layout

Spring Cloud Gateway (WebFlux). Smaller surface than MVC services — packages group **infrastructure**, not domain features.

| Package | Responsibility |
|---------|----------------|
| `config` | Routes, `GatewayProperties`, Redis |
| `filter` | Correlation ID, access logging |
| `security` | OAuth2 resource server / JWT (reactive) |
| `ratelimit` | Redis rate limiting |
| `fallback` | Circuit-breaker fallback responses |
| `error` | Global error handling |

No `api/application/domain` split — the gateway proxies HTTP and does not own business entities.

See route targets in `src/main/resources/application.yml` (identity, wallet, commerce, notification).
