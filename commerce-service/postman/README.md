# Commerce Service — Postman

**Collection:** `FinFlow-Commerce-Service.postman_collection.json`  
**Port:** `18083` · variable `commerceBaseUrl`

## Import

1. FinFlow Local environment: `../../postman/FinFlow-Local.postman_environment.json` (repo root `postman/`)
2. Identity + Wallet collections (for tokens and wallet)
3. This collection

## Collection layout

| Folder | Audience | Purpose |
|--------|----------|---------|
| **01 Health** | Ops | Actuator + dependency checks |
| **02 Customer — Browse** | Buyer | Categories, search, product detail |
| **03 Customer — Cart & checkout** | Buyer | Cart CRUD; checkout → `{ "orders": [...] }` |
| **04 Customer — Orders** | Buyer | Direct order, list, pay, cancel |
| **05 Customer — Returns** | Buyer | Request + list |
| **06 Merchant — Catalogue** | Seller | Category, product create/update |
| **07 Merchant — Coupons** | Seller | Create coupon |
| **08 Merchant — Orders & fulfilment** | Seller | List/get, start/complete fulfilment |
| **09 Merchant — Returns** | Seller | Approve, reject, complete |

## Happy-path order

1. Identity: login → create org (merchant) → login again → set `accessToken`
2. Wallet: deposit (if using wallet payments)
3. **06** Create category → product (saves `categoryId`, `productId`)
4. **07** Create coupon (optional)
5. **02** Search / get product
6. **03** Add to cart → **Checkout** (change `checkoutKey` each new run)
7. **04** List/get/pay/cancel as needed
8. **08** Merchant fulfilment
9. **05** + **09** Returns flow

## Variables (collection)

| Variable | Set by |
|----------|--------|
| `accessToken` | You (from Identity) |
| `categoryId` | Create category test script |
| `productId` | Create product test script |
| `orderId` | Checkout or create order test script |
| `returnId` | Request return test script |
| `orderKey` / `checkoutKey` | You — **must change** for each new idempotent create/checkout |

## Marketplace rules (matches code)

- Customers never send `organisationId` or `sellerId`.
- Merchants never send seller id in body; JWT must include the seller org (`orgIds`).
- Cart checkout creates **one order per seller** in the cart in a single HTTP call.
- `paymentOutcome` in JSON applies only when the server uses **simulated** payment.

## Local profile

If Commerce runs with `spring.profiles.active=local`, fixed test user/org may apply without a real JWT — see `commerce-service/README.md`. For integrated testing, always use a real `accessToken`.
