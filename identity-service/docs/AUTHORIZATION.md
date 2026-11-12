# Identity authorization (config-driven)

Identity **does not** import wallet or commerce. It only attaches permission **labels** to JWTs.

Full plain-language guide: [../../docs/PERMISSIONS.md](../../docs/PERMISSIONS.md)

## Configure (one line per bucket)

```properties
identity.authorization.verified-consumer=wallet:read,wallet:write,commerce:read,commerce:write
identity.authorization.org-role.ORG_ADMIN=org:manage,members:manage,wallet:read,wallet:write,commerce:read,commerce:write
identity.authorization.org-role.FINANCE_OPERATOR=wallet:read,wallet:write,commerce:read,commerce:write
identity.authorization.org-role.MEMBER=wallet:read,wallet:write,commerce:read,commerce:write
```

| Property | Purpose |
|----------|---------|
| `identity.authorization.verified-consumer` | Comma list for every **email-verified** user |
| `identity.authorization.org-role.<ROLE>` | Extra comma list per `ORG_ADMIN` / `FINANCE_OPERATOR` / `MEMBER` |

Convention: **`{service}:read`** or **`{service}:write`**.

## Downstream

Each service declares which labels it enforces (see [PERMISSIONS.md](../../docs/PERMISSIONS.md)). Strings must match spelling in identity config.

## Reference

Commerce: [COMMERCE_SERVICE_ARCHITECTURE.md](../../commerce-service/COMMERCE_SERVICE_ARCHITECTURE.md)

Wallet shopper API: `/api/v1/customer/wallet/**`
