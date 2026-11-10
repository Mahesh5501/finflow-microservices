# Postman — Commerce Service

Port: **18083** · env: `commerceBaseUrl`

## Import

1. `postman/FinFlow-Local.postman_environment.json` → **FinFlow Local**
2. Identity + Wallet collections (prereqs)
3. `commerce-service/postman/FinFlow-Commerce-Service.postman_collection.json`

## Order

1. Identity happy path through **Create Org → Login again** (`{{orgId}}`, `{{accessToken}}`)
2. Wallet: Create → Deposit (needed when `payment.provider=wallet`)
3. Commerce (via gateway or direct 18083):
   - **Health:** `Health` folder — actuator (no auth), `GET /api/v1/health/dependencies` (Bearer + `commerce:read`).
   - **Customer (marketplace):** browse/search/add-to-cart by `productId` only — no `organisationId`. Checkout one seller at a time (`sellerOrganisationId` query param when cart has multiple sellers).
   - **Merchant:** `organisationId` still required when creating categories, products, coupons; list/get orders under **Merchant orders**.
   - **Customer:** browse catalog, cart, orders, return request → `/api/v1/customer/...`
   - **Merchant (org ops):** manage catalog, coupons, fulfilment, return approval → `/api/v1/merchant/...`

Collection covers all `/api/v1/customer/**` and `/api/v1/merchant/**` controllers (not legacy `/api/v1/carts` paths).

Idempotency: change `{{orderKey}}` / `{{checkoutKey}}` for each new create/checkout.
