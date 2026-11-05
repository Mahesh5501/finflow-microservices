"""Generate Commerce, Notification, Transaction, and API Gateway Postman collections."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def json_ct():
    return [{"key": "Content-Type", "value": "application/json"}]


def auth(token="accessToken"):
    return [{"key": "Authorization", "value": f"Bearer {{{{{token}}}}}"}]


def idem(key_var):
    return [{"key": "Idempotency-Key", "value": f"{{{{{key_var}}}}}"}]


def save_id(field, var, codes=(200, 201)):
    codes_js = " || ".join(f"pm.response.code === {c}" for c in codes)
    return [
        f"if ({codes_js}) {{",
        "  const body = pm.response.json();",
        f"  if (body.{field} != null) {{ pm.collectionVariables.set('{var}', String(body.{field})); pm.environment.set('{var}', String(body.{field})); }}",
        "}",
    ]


def named(name, method, url, headers=None, body=None, tests=None):
    item = {
        "name": name,
        "request": {"method": method, "header": headers or [], "url": url},
    }
    if body is not None:
        item["request"]["body"] = {"mode": "raw", "raw": body}
    if tests:
        item["event"] = [
            {
                "listen": "test",
                "script": {"type": "text/javascript", "exec": tests},
            }
        ]
    return item


def write(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print("wrote", path)


SHIPPING = """{
  "line1": "10 FinFlow Street",
  "line2": "Suite 2",
  "city": "London",
  "state": "ENG",
  "postalCode": "EC1A1BB",
  "country": "GB"
}"""


def commerce_collection() -> dict:
    return {
        "info": {
            "_postman_id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
            "name": "FinFlow — Commerce Service",
            "description": (
                "Select environment **FinFlow Local**.\n\n"
                "Prereqs (Identity + Wallet):\n"
                "1. Identity: Register → Verify → Login → Create Org → Login again\n"
                "2. Wallet: Create wallet → Deposit (checkout debits wallet when payment.provider=wallet)\n"
                "3. {{orgId}} set from Identity Create Org\n\n"
                "Happy path: Catalogue → Cart checkout **or** Orders direct → Fulfilment → Returns\n\n"
                "{{commerceBaseUrl}} = http://localhost:18083\n"
                "paymentOutcome is only for payment.provider=simulated. "
                "With wallet provider omit it / null and keep wallet funded."
            ),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "variable": [
            {"key": "commerceBaseUrl", "value": "http://localhost:18083"},
            {"key": "accessToken", "value": ""},
            {"key": "orgId", "value": ""},
            {"key": "categoryId", "value": ""},
            {"key": "productId", "value": ""},
            {"key": "orderId", "value": ""},
            {"key": "returnId", "value": ""},
            {"key": "couponCode", "value": "SAVE10"},
            {"key": "orderKey", "value": "order-demo-001"},
            {"key": "checkoutKey", "value": "checkout-demo-001"},
        ],
        "item": [
            {
                "name": "Catalogue",
                "item": [
                    named(
                        "1. Create Category",
                        "POST",
                        "{{commerceBaseUrl}}/api/v1/categories",
                        auth() + json_ct(),
                        '{\n  "organisationId": {{orgId}},\n  "name": "Electronics",\n  "slug": "electronics"\n}',
                        save_id("categoryId", "categoryId"),
                    ),
                    named(
                        "2. List Categories",
                        "GET",
                        "{{commerceBaseUrl}}/api/v1/categories?organisationId={{orgId}}",
                        auth(),
                    ),
                    named(
                        "3. Create Product",
                        "POST",
                        "{{commerceBaseUrl}}/api/v1/products",
                        auth() + json_ct(),
                        '{\n  "organisationId": {{orgId}},\n  "name": "Demo Laptop",\n  "unitPrice": "999.00",\n  "availableQuantity": 25,\n  "categoryId": {{categoryId}}\n}',
                        save_id("productId", "productId"),
                    ),
                    named(
                        "4. List Products",
                        "GET",
                        "{{commerceBaseUrl}}/api/v1/products?organisationId={{orgId}}",
                        auth(),
                    ),
                    named(
                        "5. Search Products",
                        "GET",
                        "{{commerceBaseUrl}}/api/v1/products/search?organisationId={{orgId}}&q=Laptop&categoryId={{categoryId}}&page=0&size=20",
                        auth(),
                    ),
                    named(
                        "6. Update Product (PATCH)",
                        "PATCH",
                        "{{commerceBaseUrl}}/api/v1/products/{{productId}}",
                        auth() + json_ct(),
                        '{\n  "unitPrice": "949.00",\n  "availableQuantity": 20\n}',
                    ),
                ],
            },
            {
                "name": "Coupons",
                "item": [
                    named(
                        "1. Create Coupon",
                        "POST",
                        "{{commerceBaseUrl}}/api/v1/coupons",
                        auth() + json_ct(),
                        '{\n  "organisationId": {{orgId}},\n  "code": "{{couponCode}}",\n  "percentOff": "10.00",\n  "expiresAt": null\n}',
                    ),
                ],
            },
            {
                "name": "Cart",
                "description": "Add → checkout creates PAID/PENDING order (depends on payment provider).",
                "item": [
                    named(
                        "1. Get Cart",
                        "GET",
                        "{{commerceBaseUrl}}/api/v1/carts?organisationId={{orgId}}",
                        auth(),
                    ),
                    named(
                        "2. Add Item",
                        "POST",
                        "{{commerceBaseUrl}}/api/v1/carts/items",
                        auth() + json_ct(),
                        '{\n  "organisationId": {{orgId}},\n  "productId": {{productId}},\n  "quantity": 1\n}',
                    ),
                    named(
                        "3. Upsert Item Qty",
                        "PUT",
                        "{{commerceBaseUrl}}/api/v1/carts/items/{{productId}}",
                        auth() + json_ct(),
                        '{\n  "organisationId": {{orgId}},\n  "quantity": 2\n}',
                    ),
                    named(
                        "4. Checkout Cart",
                        "POST",
                        "{{commerceBaseUrl}}/api/v1/carts/checkout",
                        auth() + idem("checkoutKey") + json_ct(),
                        '{\n  "organisationId": {{orgId}},\n  "shippingAddress": '
                        + SHIPPING
                        + ',\n  "couponCode": "{{couponCode}}",\n  "paymentOutcome": null\n}',
                        save_id("orderId", "orderId"),
                    ),
                    named(
                        "5. Remove Item",
                        "DELETE",
                        "{{commerceBaseUrl}}/api/v1/carts/items/{{productId}}?organisationId={{orgId}}",
                        auth(),
                    ),
                    named(
                        "6. Clear Cart",
                        "DELETE",
                        "{{commerceBaseUrl}}/api/v1/carts?organisationId={{orgId}}",
                        auth(),
                    ),
                ],
            },
            {
                "name": "Orders",
                "description": "Direct place-order path (no cart). Use a fresh Idempotency-Key each new order.",
                "item": [
                    named(
                        "1. Create Order",
                        "POST",
                        "{{commerceBaseUrl}}/api/v1/orders",
                        auth() + idem("orderKey") + json_ct(),
                        '{\n  "organisationId": {{orgId}},\n  "items": [\n    { "productId": {{productId}}, "quantity": 1 }\n  ],\n  "shippingAddress": '
                        + SHIPPING
                        + ',\n  "couponCode": null,\n  "paymentOutcome": null\n}',
                        save_id("orderId", "orderId"),
                    ),
                    named(
                        "2. List Orders (mine)",
                        "GET",
                        "{{commerceBaseUrl}}/api/v1/orders?organisationId={{orgId}}&mineOnly=true",
                        auth(),
                    ),
                    named(
                        "3. Get Order",
                        "GET",
                        "{{commerceBaseUrl}}/api/v1/orders/{{orderId}}",
                        auth(),
                    ),
                    named(
                        "4. Pay Order",
                        "POST",
                        "{{commerceBaseUrl}}/api/v1/orders/{{orderId}}/pay",
                        auth(),
                    ),
                    named(
                        "5. Cancel Order",
                        "POST",
                        "{{commerceBaseUrl}}/api/v1/orders/{{orderId}}/cancel",
                        auth(),
                    ),
                ],
            },
            {
                "name": "Fulfilment",
                "item": [
                    named(
                        "1. Start Fulfilment",
                        "POST",
                        "{{commerceBaseUrl}}/api/v1/orders/{{orderId}}/fulfilment/start",
                        auth(),
                    ),
                    named(
                        "2. Complete Fulfilment",
                        "POST",
                        "{{commerceBaseUrl}}/api/v1/orders/{{orderId}}/fulfilment/complete",
                        auth(),
                    ),
                ],
            },
            {
                "name": "Returns",
                "item": [
                    named(
                        "1. Request Return",
                        "POST",
                        "{{commerceBaseUrl}}/api/v1/orders/{{orderId}}/returns",
                        auth() + json_ct(),
                        '{\n  "reason": "Damaged on arrival"\n}',
                        save_id("returnId", "returnId"),
                    ),
                    named(
                        "2. List Returns for Order",
                        "GET",
                        "{{commerceBaseUrl}}/api/v1/orders/{{orderId}}/returns",
                        auth(),
                    ),
                    named(
                        "3. Approve Return",
                        "POST",
                        "{{commerceBaseUrl}}/api/v1/returns/{{returnId}}/approve",
                        auth(),
                    ),
                    named(
                        "4. Reject Return",
                        "POST",
                        "{{commerceBaseUrl}}/api/v1/returns/{{returnId}}/reject",
                        auth(),
                    ),
                    named(
                        "5. Complete Return (refund)",
                        "POST",
                        "{{commerceBaseUrl}}/api/v1/returns/{{returnId}}/complete",
                        auth(),
                    ),
                ],
            },
        ],
    }


def transaction_collection() -> dict:
    return {
        "info": {
            "_postman_id": "e5f6a7b8-c9d0-1234-ef01-345678901234",
            "name": "FinFlow — Transaction Service",
            "description": (
                "Select environment **FinFlow Local**.\n\n"
                "Ledger orchestration over wallet balances (saga transfer).\n"
                "Prereqs: Identity login with wallets:* + Wallet Create A (/B for transfer).\n\n"
                "{{transactionBaseUrl}} = http://localhost:18085\n"
                "Change Idempotency-Key vars for each new money move."
            ),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "variable": [
            {"key": "transactionBaseUrl", "value": "http://localhost:18085"},
            {"key": "accessToken", "value": ""},
            {"key": "walletId", "value": ""},
            {"key": "walletId2", "value": ""},
            {"key": "transactionId", "value": ""},
            {"key": "txDepositKey", "value": "tx-deposit-001"},
            {"key": "txWithdrawKey", "value": "tx-withdraw-001"},
            {"key": "txTransferKey", "value": "tx-transfer-001"},
        ],
        "item": [
            {
                "name": "Money moves",
                "item": [
                    named(
                        "1. Deposit",
                        "POST",
                        "{{transactionBaseUrl}}/api/v1/transactions/deposits",
                        auth() + idem("txDepositKey") + json_ct(),
                        '{\n  "walletId": {{walletId}},\n  "amount": "100.00"\n}',
                        save_id("transactionId", "transactionId"),
                    ),
                    named(
                        "2. Transfer (saga)",
                        "POST",
                        "{{transactionBaseUrl}}/api/v1/transfers",
                        auth() + idem("txTransferKey") + json_ct(),
                        '{\n  "sourceWalletId": {{walletId}},\n  "destinationWalletId": {{walletId2}},\n  "amount": "25.00"\n}',
                        save_id("transactionId", "transactionId"),
                    ),
                    named(
                        "3. Withdraw",
                        "POST",
                        "{{transactionBaseUrl}}/api/v1/transactions/withdrawals",
                        auth() + idem("txWithdrawKey") + json_ct(),
                        '{\n  "walletId": {{walletId}},\n  "amount": "10.00"\n}',
                        save_id("transactionId", "transactionId"),
                    ),
                ],
            },
            {
                "name": "History / ledger",
                "item": [
                    named(
                        "1. Get Transaction",
                        "GET",
                        "{{transactionBaseUrl}}/api/v1/transactions/{{transactionId}}",
                        auth(),
                    ),
                    named(
                        "2. History by Wallet",
                        "GET",
                        "{{transactionBaseUrl}}/api/v1/transactions?walletId={{walletId}}",
                        auth(),
                    ),
                    named(
                        "3. Ledger Balance (reconcile)",
                        "GET",
                        "{{transactionBaseUrl}}/api/v1/transactions/wallets/{{walletId}}/ledger-balance",
                        auth(),
                    ),
                ],
            },
        ],
    }


def notification_collection() -> dict:
    return {
        "info": {
            "_postman_id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
            "name": "FinFlow — Notification Service",
            "description": (
                "Select environment **FinFlow Local**.\n\n"
                "Inbox is always for the JWT subject (`/me`) — never pass a userId.\n"
                "Events land via Kafka from commerce (order paid / fulfilled / etc.). "
                "With Kafka down, commerce still works; inbox stays empty until events flow.\n\n"
                "{{notificationBaseUrl}} = http://localhost:18084"
            ),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "variable": [
            {"key": "notificationBaseUrl", "value": "http://localhost:18084"},
            {"key": "accessToken", "value": ""},
        ],
        "item": [
            named(
                "1. List My Notifications",
                "GET",
                "{{notificationBaseUrl}}/api/v1/notifications/me",
                auth(),
            ),
        ],
    }


def gateway_collection() -> dict:
    """Smoke checks through the edge (JWT + Redis rate limit + CB)."""
    return {
        "info": {
            "_postman_id": "d4e5f6a7-b8c9-0123-def0-234567890123",
            "name": "FinFlow — API Gateway",
            "description": (
                "Select environment **FinFlow Local**.\n\n"
                "Same paths as services, via {{gatewayBaseUrl}} = http://localhost:18080.\n"
                "Requires Redis (rate limit) + Identity JWKS for protected routes.\n\n"
                "Use for edge smoke tests; deep flows stay in service collections."
            ),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "variable": [
            {"key": "gatewayBaseUrl", "value": "http://localhost:18080"},
            {"key": "accessToken", "value": ""},
            {"key": "orgId", "value": ""},
            {"key": "userId", "value": ""},
            {"key": "walletId", "value": ""},
        ],
        "item": [
            {
                "name": "Public / Identity",
                "item": [
                    named(
                        "1. JWKS via Gateway",
                        "GET",
                        "{{gatewayBaseUrl}}/.well-known/jwks.json",
                    ),
                    named(
                        "2. Me (profile)",
                        "GET",
                        "{{gatewayBaseUrl}}/api/v1/users/me",
                        auth(),
                    ),
                    named(
                        "3. List My Orgs",
                        "GET",
                        "{{gatewayBaseUrl}}/api/v1/orgs",
                        auth(),
                    ),
                ],
            },
            {
                "name": "Wallet via Gateway",
                "item": [
                    named(
                        "1. Wallet by User",
                        "GET",
                        "{{gatewayBaseUrl}}/api/v1/users/{{userId}}/wallet",
                        auth(),
                    ),
                ],
            },
            {
                "name": "Commerce via Gateway",
                "item": [
                    named(
                        "1. List Products",
                        "GET",
                        "{{gatewayBaseUrl}}/api/v1/products?organisationId={{orgId}}",
                        auth(),
                    ),
                    named(
                        "2. List Categories",
                        "GET",
                        "{{gatewayBaseUrl}}/api/v1/categories?organisationId={{orgId}}",
                        auth(),
                    ),
                    named(
                        "3. Get Cart",
                        "GET",
                        "{{gatewayBaseUrl}}/api/v1/carts?organisationId={{orgId}}",
                        auth(),
                    ),
                ],
            },
            {
                "name": "Notification via Gateway",
                "item": [
                    named(
                        "1. My Notifications",
                        "GET",
                        "{{gatewayBaseUrl}}/api/v1/notifications/me",
                        auth(),
                    ),
                ],
            },
            {
                "name": "Transaction via Gateway",
                "item": [
                    named(
                        "1. History by Wallet",
                        "GET",
                        "{{gatewayBaseUrl}}/api/v1/transactions?walletId={{walletId}}",
                        auth(),
                    ),
                ],
            },
            {
                "name": "Observability",
                "item": [
                    named(
                        "1. Health",
                        "GET",
                        "{{gatewayBaseUrl}}/actuator/health",
                    ),
                    named(
                        "2. Prometheus",
                        "GET",
                        "{{gatewayBaseUrl}}/actuator/prometheus",
                    ),
                ],
            },
        ],
    }


def update_environment() -> None:
    env_path = ROOT / "postman" / "FinFlow-Local.postman_environment.json"
    data = json.loads(env_path.read_text(encoding="utf-8"))
    values = data["values"]
    by_key = {v["key"]: v for v in values}

    extras = [
        ("commerceBaseUrl", "http://localhost:18083", "default"),
        ("notificationBaseUrl", "http://localhost:18084", "default"),
        ("transactionBaseUrl", "http://localhost:18085", "default"),
        ("categoryId", "", "default"),
        ("productId", "", "default"),
        ("orderId", "", "default"),
        ("returnId", "", "default"),
        ("transactionId", "", "default"),
        ("couponCode", "SAVE10", "default"),
        ("orderKey", "order-demo-001", "default"),
        ("checkoutKey", "checkout-demo-001", "default"),
        ("txDepositKey", "tx-deposit-001", "default"),
        ("txWithdrawKey", "tx-withdraw-001", "default"),
        ("txTransferKey", "tx-transfer-001", "default"),
    ]
    for key, value, typ in extras:
        if key in by_key:
            by_key[key]["value"] = value
            by_key[key]["enabled"] = True
        else:
            values.append({"key": key, "value": value, "type": typ, "enabled": True})

    # Keep gateway first, then service bases clustered
    preferred = [
        "gatewayBaseUrl",
        "identityBaseUrl",
        "walletBaseUrl",
        "commerceBaseUrl",
        "notificationBaseUrl",
        "transactionBaseUrl",
    ]
    rest = [v for v in values if v["key"] not in preferred]
    ordered = []
    for k in preferred:
        match = next((v for v in values if v["key"] == k), None)
        if match:
            ordered.append(match)
    seen = {v["key"] for v in ordered}
    for v in rest:
        if v["key"] not in seen:
            ordered.append(v)
            seen.add(v["key"])
    data["values"] = ordered
    write(env_path, data)


def main() -> None:
    write(
        ROOT / "commerce-service/postman/FinFlow-Commerce-Service.postman_collection.json",
        commerce_collection(),
    )
    write(
        ROOT / "notification-service/postman/FinFlow-Notification-Service.postman_collection.json",
        notification_collection(),
    )
    write(
        ROOT / "transaction-service/postman/FinFlow-Transaction-Service.postman_collection.json",
        transaction_collection(),
    )
    write(
        ROOT / "api-gateway/postman/FinFlow-API-Gateway.postman_collection.json",
        gateway_collection(),
    )
    update_environment()


if __name__ == "__main__":
    main()
