# Permissions — simple model

You only need to remember **three ideas**.

## 1. What is a permission?

A short label on the JWT, like `wallet:read` or `commerce:write`.

- **`read`** = call GET-style APIs (view balance, browse catalogue).
- **`write`** = call POST/PUT-style APIs (top up, pay, place order).

Pattern everywhere: **`{service}:{read|write}`**.

## 2. Who gets which labels? (identity only)

**Identity** loads permission config in `AuthorizationProperties` (`com.finflow.identity.security`). It does not know wallet or commerce code. At login it copies labels from `application.properties` onto the token:

| Config key | Who gets it |
|------------|-------------|
| `identity.authorization.verified-consumer` | Every user with verified email (normal shopper) |
| `identity.authorization.org-role.<ROLE>` | Extra labels when user has that org membership |

One line per bucket — comma-separated, no `[0][1][2]`:

```properties
identity.authorization.verified-consumer=wallet:read,wallet:write,commerce:read,commerce:write
identity.authorization.org-role.ORG_ADMIN=org:manage,members:manage,wallet:read,wallet:write,commerce:read,commerce:write
```

To give shoppers a new capability later (e.g. `notifications:read`), add it here and teach the new service which label to require.

## 3. Who checks labels? (each service)

Each service is independent. It only reads **its own** config and the JWT:

| Service | Config | Meaning |
|---------|--------|---------|
| wallet | `wallet.security.read-permission` | Required for wallet GETs |
| wallet | `wallet.security.write-permission` | Required for wallet POSTs (top-up, etc.) |
| commerce | `commerce:read` / `commerce:write` in its security config today | Same idea for shop APIs |

**Rule:** the string in wallet/commerce config must appear in identity’s lists for users who should access that API.

Identity and wallet do **not** share a Java library — you keep the same spelling in both property files (or copy from this doc).

## Flow (one picture)

```
Login → Identity reads application.properties → JWT contains permissions: ["wallet:read", ...]
     → Wallet sees GET /customer/wallet → needs wallet:read on token ✓
     → Commerce sees POST order → needs commerce:write on token ✓
```

## Scalable / generic

- Add a **new microservice**: pick two labels (`foo:read`, `foo:write`), add them to `verified-consumer` (or a role), configure that service’s security the same way as wallet.
- Add a **new org role**: add enum value + one new `identity.authorization.org-role.NEW_ROLE=...` line.
- Change access **without redeploying logic**: edit properties, restart service, users re-login for a fresh JWT.

## Keep in sync (copy-paste checklist)

When you change wallet access:

1. `identity-service` — include `wallet:read` and/or `wallet:write` in the right comma lines.
2. `wallet-service` — `wallet.security.read-permission` / `write-permission` must match exactly.

Commerce: same for `commerce:read` and `commerce:write`.
