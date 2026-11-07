# FinFlow service catalog

Extracted from the existing source-based assessment on 2026-11-07; no new implementation analysis. All services use Java 21 / Spring Boot 3.5.6 and independent Maven POMs. No root reactor/shared application library exists.

| Service / port | Ownership | Distinct technologies | Entry point under src/main/java |
|---|---|---|---|
| api-gateway / 18080 | Edge routing/authentication, rate limit, resilience, access/correlation logs | Cloud BOM 2025.0.3, Gateway WebFlux, OAuth2 resource server, reactive Redis, Resilience4j, Prometheus/Brave | com.finflow.apigateway.ApiGatewayApplication |
| identity-service / 18081 | Users, organisations/memberships, tokens, JWT/JWKS, identity-local audit | MVC, Security, JPA/MySQL, JJWT 0.12.6, blocking Redis, scheduled cleanup | com.finflow.identity.IdentityServiceApplication |
| wallet-service / 18082 | Personal wallet balances, transfers/history and wallet lifecycle | MVC, Security, JPA/MySQL, JJWT, optimistic locking; no Redis implementation | com.finflow.wallet.WalletServiceApplication |
| commerce-service / 18083 | Catalogue/categories, stock/cart, coupons/orders, fulfilment/returns and payment work | MVC, Security, JPA/MySQL, JJWT, PaymentPort HTTP/simulator adapters, optional Kafka, scheduled maintenance | com.finflow.commerce.CommerceServiceApplication |
| notification-service / 18084 | Customer inbox records, commerce-event dedupe, log delivery | MVC, Security, JPA/MySQL, JJWT, optional Kafka consumer; mail is a stub | com.finflow.notification.NotificationServiceApplication |

Business services also use Jakarta Validation, Actuator and H2 tests. Gateway has no database; business schemas belong exclusively to their service. Notification's old notificationservice package is a deprecated placeholder, not another application.

- [PROJECT_STATUS.md](../PROJECT_STATUS.md): overall/per-service progress, test evidence and risks.
- [ARCHITECTURE.md](ARCHITECTURE.md): end-to-end workflows, API inventory and configurations.
- [SERVICE_DEPENDENCIES.md](SERVICE_DEPENDENCIES.md): communication and contract impact.
- [DATABASE_DESIGN.md](../DATABASE_DESIGN.md): schema ownership and mappings.

Root AGENTS.md covers current conventions; no service-specific copies were needed during setup. Update this catalog only when ownership, technology or entry points change.
