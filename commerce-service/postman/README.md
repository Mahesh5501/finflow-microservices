# Commerce Service Postman collection

The collection matches the current customer/merchant controllers and DTOs. It contains **48 requests covering all 30 current method/path contracts**, including dependency health and two Actuator URLs. Each request has status/JSON assertions; domain scenarios also check state, amounts, IDs or errors.

## Standalone testing

1. Start Commerce with IntelliJ **Active profiles = local**, or follow [the service README](../README.md). Use a dedicated test customer: this collection clears that customer's cart and creates catalogue/order/return fixtures.
2. Import [FinFlow-Commerce-Service.postman_collection.json](FinFlow-Commerce-Service.postman_collection.json), replacing your previously imported Commerce collection.
3. Select **No Environment** in Postman. The collection already contains its standalone defaults under **Variables**; no environment file is required.
4. Run folders **00 through 08 in order** in Collection Runner.

Defaults: `commerceBaseUrl=http://127.0.0.1:18083`, `commerceMode=local`, `commercePaymentProvider=simulated`, `commerceLocalUserId=1`, `commerceLocalOrgId=10`. Edit these values in the collection's **Variables** tab when needed. No token, Identity login or Wallet setup is needed. MySQL/Redis retain their normal manual-run configuration. If the server's local fixture identity changes, update the two commerceLocal variables to match.

The scripts supply the configured organisation at request scope, so a blank/stale shared `orgId` cannot break local merchant JSON. Local mode sends no Authorization header. Captured IDs use `commerce...` collection variables and do not overwrite shared Identity/Wallet environment values.

## Scenarios

| Folder | Purpose / prerequisite |
|---|---|
| 00 Health | Check health/info/dependency report; inspect any DOWN details |
| 01 Catalogue and coupon fixtures | Create unique category/product/coupon; get/search/PATCH catalogue |
| 02 Cart CRUD and coupon checkout | Clear test cart, add/update/remove, checkout two units with 10 percent discount |
| 03 Explicit seller checkout | Add another line and exercise sellerOrganisationId selection |
| 04 Direct order and replay | Create confirmed order, replay same payload/key, list/get customer and merchant views; confirmed pay correctly returns 409 |
| 05 Cancellation on a separate order | Create another order, cancel/refund and verify CANCELLED |
| 06 Fulfilment and approved return | Use folder 04's order; fulfil, request, approve, complete/refund; repeated completion correctly returns 409 |
| 07 Rejected return on a separate order | Create/fulfil a fresh order, request/reject a separate return; completion correctly returns 409 |
| 08 Simulated uncertain and declined payments | Uncertain retry/cancel and declined payment scenarios; skipped before sending in wallet mode |

Run folder 01 first when beginning a new run. Later folders require the IDs created by their setup; missing prerequisites produce a clear script error. Cancellation, approved return and rejected return have separate IDs and cannot invalidate each other. The explicit-seller checkout uses one seller fixture; it exercises the selector used for multi-seller carts without inventing another merchant membership.

Each fixture run gets unique category slugs/coupon codes. New order/checkout requests generate new keys automatically; the replay request retains the original key. Re-running the whole collection is supported. Existing catalogue/order history is retained; this is not a database cleanup tool.

Health may legitimately return `503` with `DOWN`. Assertions verify the report and status agree; a passed health-contract assertion does not mean every dependency is available. Expected business-error requests are labelled `[409]` and assert their specific error codes.

## JWT / Wallet integration mode

Start Commerce with normal JWT validation (remove local, or explicitly re-enable validation). Select the shared **FinFlow Local** environment and set these Commerce collection variables:

- `commerceMode=integrated`
- `commercePaymentProvider=wallet` (or simulated, matching the server)
- Shared `accessToken`, `userId` and `orgId` from Identity login/membership setup
- `commerceBaseUrl` pointing directly to Commerce

Wallet mode requires an owned, funded INR wallet. The script adds the bearer token only in integrated mode. Simulation-only cases are skipped in Wallet mode; hints cannot force a real Wallet payment outcome. The refund scenarios use the same caller who placed the orders: they do not prove another merchant's authority to refund a customer's wallet. Local mode is a fixture-based workflow check, not a multi-user security test.

## Regeneration

From the repository root:

```sh
python postman/_gen_extra_collections.py --commerce-only
```

The source of Commerce scenarios is [../../postman/_commerce_collection.py](../../postman/_commerce_collection.py). The command regenerates this single collection with embedded defaults, preserving other service collections and the shared environment. Avoid hand-editing generated JSON alone.

## Verification - 2026-11-11

Standalone settings are now embedded in the collection; the duplicate environment file was removed. Compared the regenerated file with the previously verified version: all 48 requests, scripts, auth settings and variables are identical; only the import description changed. Every removed environment value is retained as a collection default. No new HTTP run was needed for this configuration-file simplification. The HTTP results below precede that simplification.

- Newman 6.2.1: **two consecutive runs against the same isolated H2 database, each 48 requests and 200 assertions, zero failures**.
- Used a temporary Java 21 Commerce instance with local profile on port 18093, simple cache and in-memory H2; it was stopped afterward. Development MySQL data was not used. Redis was intentionally unreachable, so its dependency report was DOWN/503 rather than fabricated UP. No real Wallet, Identity, Kafka or MySQL integration was exercised.
- Wallet-mode simulation skip check: **zero HTTP requests**, zero failures.
- Static comparison: every generated method/path matches current controllers; all 30 are covered. JSON bodies/idempotency headers and 91 script blocks checked. Repeated generation produces identical files; other service generator functions/collections/shared environment were preserved.
- Reports: `../target/commerce-postman-report-1.json`, `commerce-postman-report-2.json`, `commerce-postman-wallet-skip.json`. Target artifacts can be removed by a clean build.
