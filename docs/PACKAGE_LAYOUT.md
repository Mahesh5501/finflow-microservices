# FinFlow package layout (all services)

Common pattern for MVC microservices: **feature module** + **`api` / `application` / `domain` / `persistence` / `dto`**.

| Service | Doc | Notes |
|---------|-----|--------|
| identity-service | [PACKAGE-STRUCTURE.md](../identity-service/PACKAGE-STRUCTURE.md) | `auth`, `organization` (+ `organization.audit`) |
| wallet-service | [PACKAGE-STRUCTURE.md](../wallet-service/PACKAGE-STRUCTURE.md) | `wallet` feature; `api/customer` + `api/integration` |
| commerce-service | [PACKAGE-STRUCTURE.md](../commerce-service/PACKAGE-STRUCTURE.md) | `cart`, `order`, `catalogue`, … |
| notification-service | [PACKAGE-STRUCTURE.md](../notification-service/PACKAGE-STRUCTURE.md) | `notification` + `integration.*` |
| api-gateway | [PACKAGE-STRUCTURE.md](../api-gateway/PACKAGE-STRUCTURE.md) | `gateway` module (api/config/filter/error/ratelimit) + `security` |

Permissions across services: [PERMISSIONS.md](PERMISSIONS.md).
