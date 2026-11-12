# Identity service — package layout

Same **feature-first** style as `commerce-service`: each capability is a module with familiar layer names used across the industry.

## Modules

| Module | Package root | Responsibility |
|--------|--------------|----------------|
| **auth** | `com.finflow.identity.auth` | Register, login, JWT issue, JWKS, tokens, users |
| **organization** | `com.finflow.identity.organization` | Orgs, memberships, invites, org roles, audit sub-module |
| **organization.audit** | `com.finflow.identity.organization.audit` | Org compliance trail (also records auth events with nullable `orgId`) |
| **security** | `com.finflow.identity.security` | JWT filter, signing keys, `AuthorizationProperties`, rate limits |
| **integration.mail** | `com.finflow.identity.integration.mail` | Outbound email |
| **exception** | `com.finflow.identity.exception` | API errors (shared) |

## Layers (per module)

```
auth/
  api/           → REST controllers (AuthController, JwksController)
  application/   → AuthService, TokenCleanupJob
  domain/        → User, RefreshToken, …
  persistence/   → Spring Data repositories
  dto/           → Request/response records (no request/response subfolders)

organization/
  api/           → OrgController (includes GET /orgs/{id}/audit)
  application/   → OrgService
  domain/        → Org, Membership, OrgRole, …
  persistence/
  dto/
  audit/         → AuditService, AuditLog, AuditLogResponse (sub-feature, not a root module)
    application/
    domain/
    persistence/
    dto/
```

## Conventions (aligned with commerce)

- **api** — HTTP only: routing, validation, principal extraction.
- **application** — use cases and transactions.
- **domain** — JPA entities and enums.
- **persistence** — repositories.
- **dto** — API contracts (flat package, like `commerce.order.dto`).

Shared cross-cutting:

- **security** — `AuthorizationProperties` binds `identity.authorization.*` (permission labels on JWT). Name matches the usual `*SecurityProperties` / `*Properties` pattern (`CommerceSecurityProperties` in commerce).
- **exception** — `ApiException`, `GlobalExceptionHandler`.

## HTTP surface (unchanged)

- `/api/v1/auth/**` — auth module
- `/api/v1/orgs/**` — organization module
- `/.well-known/jwks.json` — auth module (JwksController)

## Docs

- Permissions: [../docs/PERMISSIONS.md](../docs/PERMISSIONS.md)
- Detail: [docs/AUTHORIZATION.md](docs/AUTHORIZATION.md)
