# Postman — Commerce Service

Port: **18083** · env: `commerceBaseUrl`

## Import

1. `postman/FinFlow-Local.postman_environment.json` → **FinFlow Local**
2. Identity + Wallet collections (prereqs)
3. `commerce-service/postman/FinFlow-Commerce-Service.postman_collection.json`

## Order

1. Identity happy path through **Create Org → Login again** (`{{orgId}}`, `{{accessToken}}`)
2. Wallet: Create → Deposit (needed when `payment.provider=wallet`)
3. Commerce: **Catalogue** → **Coupons** (optional) → **Cart** checkout **or** **Orders** direct → **Fulfilment** → **Returns**

Idempotency: change `{{orderKey}}` / `{{checkoutKey}}` for each new create/checkout.
