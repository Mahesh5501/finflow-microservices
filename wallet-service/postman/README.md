# Postman — FinFlow (all services)

## Import

1. `postman/FinFlow-Local.postman_environment.json` → select **FinFlow Local** (top-right)
2. `identity-service/postman/FinFlow-Identity-Service.postman_collection.json`
3. `wallet-service/postman/FinFlow-Wallet-Service.postman_collection.json`
4. `commerce-service/postman/FinFlow-Commerce-Service.postman_collection.json`
5. `notification-service/postman/FinFlow-Notification-Service.postman_collection.json`
6. `api-gateway/postman/FinFlow-API-Gateway.postman_collection.json` (edge smoke)

Delete old copies first if Postman still has stale URLs/headers.

## Shared variables (environment)

| Variable | Meaning |
|----------|---------|
| `gatewayBaseUrl` | `http://localhost:18080` |
| `identityBaseUrl` | `http://localhost:18081` |
| `walletBaseUrl` | `http://localhost:18082` |
| `commerceBaseUrl` | `http://localhost:18083` |
| `notificationBaseUrl` | `http://localhost:18084` |
| `accessToken` | User A JWT (Identity Login / 1b) |
| `accessToken2` | User B JWT (Member Login 7b) |
| `userId` / `userId2` | From login responses |
| `orgId` | From Create Org |
| `walletId` / `walletId2` | From Create Wallet A/B |
| `productId` / `orderId` / `returnId` | Filled by Commerce test scripts |

## Order

**Identity**

1. Register → Verify Email → Login  
2. Create Org → **1b. Login again** (JWT must include `wallets:read/write`)  
3. (For transfer) Register Member → Verify → Invite → Member Login → Accept Invite → **7b. Member Login again**

**Wallet**

1. Create Wallet A (`Bearer {{accessToken}}`)  
2. Deposit → Transfer → Withdraw → history  

**Commerce**

1. Catalogue (category → product) → optional Coupon  
2. Cart checkout **or** Orders create → Pay / Fulfilment → Returns  

**Notification**

1. List My Notifications (after commerce events + Kafka)

**Gateway**

Edge GETs for the same paths via `:18080` (Redis required for rate limit).

## Regenerate JSON

```bash
cd postman && python _gen_collections.py
```
