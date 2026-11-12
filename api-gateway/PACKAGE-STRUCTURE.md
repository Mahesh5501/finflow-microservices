# API gateway — package layout

Rooted at `com.finflow.apigateway` (reactive Spring Cloud Gateway). Same **feature module** idea as commerce: a `gateway` module for edge behaviour, plus `security` for JWT.

```text
com.finflow.apigateway
|-- ApiGatewayApplication
|-- gateway/
|   |-- api: FallbackController
|   |-- config: GatewayConfig, GatewayProperties
|   |-- filter: CorrelationIdFilter, AccessLogFilter
|   |-- error: GlobalErrorConfig
|   `-- ratelimit: RateLimitConfig
`-- security: SecurityConfig
```

There is no `domain` / `persistence` layer — the gateway proxies HTTP and owns no business entities. Tests mirror `gateway.error` and `gateway.ratelimit`.

Routes and resilience settings live in `src/main/resources/application.yml`.
