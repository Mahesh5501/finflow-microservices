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


def _gateway_identity_via_gateway_items() -> list:
    """Identity HTTP APIs routed through the gateway (gatewayBaseUrl). Does not modify the identity-service collection."""
    base = "{{gatewayBaseUrl}}"
    login_save = [
        "if (pm.response.code === 200) {",
        "  const body = pm.response.json();",
        "  if (body.accessToken) { pm.collectionVariables.set('accessToken', body.accessToken); pm.environment.set('accessToken', body.accessToken); }",
        "  if (body.refreshToken) { pm.collectionVariables.set('refreshToken', body.refreshToken); pm.environment.set('refreshToken', body.refreshToken); }",
        "  if (body.userId != null) { pm.collectionVariables.set('userId', String(body.userId)); pm.environment.set('userId', String(body.userId)); }",
        "  if (body.permissions) console.log('permissions', body.permissions);",
        "}",
    ]
    member_login_save = [
        "if (pm.response.code === 200) {",
        "  const body = pm.response.json();",
        "  if (body.accessToken) { pm.collectionVariables.set('accessToken2', body.accessToken); pm.environment.set('accessToken2', body.accessToken); }",
        "  if (body.refreshToken) { pm.collectionVariables.set('refreshToken2', body.refreshToken); pm.environment.set('refreshToken2', body.refreshToken); }",
        "  if (body.userId != null) { pm.collectionVariables.set('userId2', String(body.userId)); pm.environment.set('userId2', String(body.userId)); }",
        "  if (body.permissions) console.log('permissions', body.permissions);",
        "}",
    ]
    register_save = [
        "if (pm.response.code === 201) {",
        "  const body = pm.response.json();",
        "  if (body.token) { pm.collectionVariables.set('verificationToken', body.token); pm.environment.set('verificationToken', body.token); }",
        "  if (body.profile && body.profile.userId != null) { pm.collectionVariables.set('userId', String(body.profile.userId)); pm.environment.set('userId', String(body.profile.userId)); }",
        "}",
    ]
    register_member_save = [
        "if (pm.response.code === 201) {",
        "  const body = pm.response.json();",
        "  if (body.token) { pm.collectionVariables.set('verificationToken', body.token); pm.environment.set('verificationToken', body.token); }",
        "  if (body.profile && body.profile.userId != null) { pm.collectionVariables.set('userId2', String(body.profile.userId)); pm.environment.set('userId2', String(body.profile.userId)); }",
        "}",
    ]
    auth_folder = {
        "name": "Auth",
        "description": "Public and authenticated identity auth endpoints via the gateway identity route.",
        "item": [
            named("0. JWKS (public)", "GET", f"{base}/.well-known/jwks.json"),
            named(
                "1. Register",
                "POST",
                f"{base}/api/v1/auth/register",
                json_ct(),
                '{\n  "email": "{{email}}",\n  "password": "{{password}}"\n}',
                register_save,
            ),
            named(
                "1b. Resend verification",
                "POST",
                f"{base}/api/v1/auth/resend-verification",
                json_ct(),
                '{\n  "email": "{{email}}"\n}',
                [
                    "if (pm.response.code === 200) {",
                    "  const body = pm.response.json();",
                    "  if (body.token) { pm.collectionVariables.set('verificationToken', body.token); pm.environment.set('verificationToken', body.token); }",
                    "}",
                ],
            ),
            named(
                "2. Verify Email",
                "POST",
                f"{base}/api/v1/auth/verify-email",
                json_ct(),
                '{\n  "token": "{{verificationToken}}"\n}',
            ),
            named(
                "3. Login",
                "POST",
                f"{base}/api/v1/auth/login",
                json_ct(),
                '{\n  "email": "{{email}}",\n  "password": "{{password}}"\n}',
                login_save,
            ),
            named("4. Me", "GET", f"{base}/api/v1/users/me", auth()),
            named(
                "5. Refresh",
                "POST",
                f"{base}/api/v1/auth/refresh",
                json_ct(),
                '{\n  "refreshToken": "{{refreshToken}}"\n}',
                login_save,
            ),
            named(
                "6. Forgot Password",
                "POST",
                f"{base}/api/v1/auth/forgot-password",
                json_ct(),
                '{\n  "email": "{{email}}"\n}',
                [
                    "if (pm.response.code === 200) {",
                    "  const body = pm.response.json();",
                    "  if (body.token) { pm.collectionVariables.set('resetToken', body.token); pm.environment.set('resetToken', body.token); }",
                    "}",
                ],
            ),
            named(
                "7. Reset Password",
                "POST",
                f"{base}/api/v1/auth/reset-password",
                json_ct(),
                '{\n  "resetToken": "{{resetToken}}",\n  "newPassword": "{{password}}"\n}',
            ),
            named(
                "8. Change Password",
                "POST",
                f"{base}/api/v1/auth/change-password",
                auth() + json_ct(),
                '{\n  "currentPassword": "{{password}}",\n  "newPassword": "{{password}}"\n}',
            ),
            named(
                "9. Logout",
                "POST",
                f"{base}/api/v1/auth/logout",
                json_ct(),
                '{\n  "refreshToken": "{{refreshToken}}"\n}',
            ),
        ],
    }
    orgs_folder = {
        "name": "Organisations",
        "description": "Organisation and membership APIs via the gateway. Run Auth first; re-login after create/accept so JWT permissions refresh.",
        "item": [
            named(
                "1. Create Org",
                "POST",
                f"{base}/api/v1/orgs",
                auth() + json_ct(),
                '{\n  "name": "Acme Finance"\n}',
                [
                    "if (pm.response.code === 201) {",
                    "  const body = pm.response.json();",
                    "  if (body.orgId != null) { pm.collectionVariables.set('orgId', String(body.orgId)); pm.environment.set('orgId', String(body.orgId)); }",
                    "}",
                ],
            ),
            named(
                "1b. Login again (wallet permissions in JWT)",
                "POST",
                f"{base}/api/v1/auth/login",
                json_ct(),
                '{\n  "email": "{{email}}",\n  "password": "{{password}}"\n}',
                login_save,
            ),
            named("2. List My Orgs", "GET", f"{base}/api/v1/orgs", auth()),
            named("2b. Get Org", "GET", f"{base}/api/v1/orgs/{{orgId}}", auth()),
            named(
                "3. Register Member (user B)",
                "POST",
                f"{base}/api/v1/auth/register",
                json_ct(),
                '{\n  "email": "{{memberEmail}}",\n  "password": "{{memberPassword}}"\n}',
                register_member_save,
            ),
            named(
                "4. Verify Member Email",
                "POST",
                f"{base}/api/v1/auth/verify-email",
                json_ct(),
                '{\n  "token": "{{verificationToken}}"\n}',
            ),
            named(
                "5. Invite Member",
                "POST",
                f"{base}/api/v1/orgs/{{orgId}}/invites",
                auth() + json_ct(),
                '{\n  "email": "{{memberEmail}}",\n  "role": "FINANCE_OPERATOR"\n}',
                [
                    "if (pm.response.code === 201) {",
                    "  const body = pm.response.json();",
                    "  if (body.token) { pm.collectionVariables.set('inviteToken', body.token); pm.environment.set('inviteToken', body.token); }",
                    "}",
                ],
            ),
            named(
                "6. Member Login (before accept)",
                "POST",
                f"{base}/api/v1/auth/login",
                json_ct(),
                '{\n  "email": "{{memberEmail}}",\n  "password": "{{memberPassword}}"\n}',
                member_login_save,
            ),
            named(
                "7. Accept Invite (as member)",
                "POST",
                f"{base}/api/v1/orgs/invites/accept",
                auth("accessToken2") + json_ct(),
                '{\n  "token": "{{inviteToken}}"\n}',
            ),
            named(
                "7b. Member Login again (wallet permissions → accessToken2)",
                "POST",
                f"{base}/api/v1/auth/login",
                json_ct(),
                '{\n  "email": "{{memberEmail}}",\n  "password": "{{memberPassword}}"\n}',
                member_login_save,
            ),
            named("8. List Members", "GET", f"{base}/api/v1/orgs/{{orgId}}/members", auth()),
            named(
                "9. Change Member Role",
                "PUT",
                f"{base}/api/v1/orgs/{{orgId}}/members/{{userId2}}/role",
                auth() + json_ct(),
                '{\n  "role": "MEMBER"\n}',
            ),
            named("10. Org Audit Log", "GET", f"{base}/api/v1/orgs/{{orgId}}/audit", auth()),
            named(
                "11. Remove Member",
                "DELETE",
                f"{base}/api/v1/orgs/{{orgId}}/members/{{userId2}}",
                auth(),
            ),
        ],
    }
    return [auth_folder, orgs_folder]


def gateway_collection() -> dict:
    """Gateway edge checks plus identity APIs via gatewayBaseUrl. No fixture toggles."""

    def event(listen, lines):
        return {"listen": listen, "script": {"type": "text/javascript", "exec": lines}}

    def case(name, method, path, status, *, token=None, generated_token=False, required=(), fields=(),
             array=False, error=None, correlation=False, tests=(), before=(), body=None,
             headers=None, base="gatewayBaseUrl", description=""):
        request_headers = list(headers or [])
        if token:
            request_headers += auth(token)
            if not generated_token:
                required = tuple(required) + (token,)
        item = named(name, method, "{{" + base + "}}" + path, request_headers, body)
        item["request"]["auth"] = {"type": "noauth"}
        item["request"]["description"] = description
        checks = [
            f"pm.test('HTTP {status}', () => pm.response.to.have.status({status}));"
        ]
        if fields or array or error:
            checks += [
                "pm.test('JSON content type', () => pm.expect(pm.response.headers.get('Content-Type') || '').to.include('json'));"
            ]
        if array:
            checks += ["pm.test('Array response', () => pm.expect(pm.response.json()).to.be.an('array'));"]
        if fields:
            checks += [
                "pm.test('Required response fields', () => {",
                "  const body = pm.response.json();",
                "  pm.expect(body).to.be.an('object');",
                f"  {json.dumps(list(fields))}.forEach(key => pm.expect(body).to.have.property(key));",
                "});",
            ]
        if error:
            checks += [
                f"pm.test('Error code {error}', () => pm.expect(pm.response.json().code).to.eql({json.dumps(error)}));",
                "pm.test('Error message', () => pm.expect(pm.response.json().message).to.be.a('string').and.not.empty);",
            ]
        if correlation:
            checks += [
                "pm.test('Routed response has correlation ID', () => pm.expect(pm.response.headers.get('X-Correlation-Id')).to.be.a('string').and.not.empty);"
            ]
        checks += list(tests)
        setup = []
        if required:
            setup = [
                f"{json.dumps(list(dict.fromkeys(required)))}.forEach(key => {{",
                "  const value = pm.variables.get(key);",
                "  if (value == null || String(value).trim() === '' || String(value).includes('{{')) {",
                "    throw new Error('Missing fixture variable: ' + key + '. See the gateway Postman README.');",
                "  }",
                "});",
            ]
        setup += list(before)
        item["event"] = ([event("prerequest", setup)] if setup else []) + [event("test", checks)]
        return item

    def folder(name, items, description):
        return {"name": name, "description": description, "item": items}

    oversized = [
        "pm.variables.set('gatewayOversizedBody', 'x'.repeat(1048577));"
    ]
    cors_headers = [
        {"key": "Origin", "value": "{{gatewayOrigin}}"},
        {"key": "Access-Control-Request-Method", "value": "POST"},
        {"key": "Access-Control-Request-Headers", "value": "authorization,content-type,idempotency-key"},
    ]
    core = [
        case("Info", "GET", "/actuator/info", 200,
             tests=["pm.test('Info object', () => pm.expect(pm.response.json()).to.be.an('object'));"]),
        case("Liveness", "GET", "/actuator/health/liveness", 200, fields=("status",),
             tests=["pm.test('Liveness UP', () => pm.expect(pm.response.json().status).to.eql('UP'));"]),
        case("Readiness", "GET", "/actuator/health/readiness", 200, fields=("status",),
             tests=["pm.test('Readiness UP', () => pm.expect(pm.response.json().status).to.eql('UP'));"]),
        case("Unknown non-API path", "GET", "/gateway-collection-missing-route", 404, error="ROUTE_NOT_FOUND"),
        case("Gateway actuator is disabled by default", "GET", "/actuator/gateway/routes", 404,
             error="ROUTE_NOT_FOUND", description="Current default configuration; not proof of private route administration."),
        case("Direct fallback response", "GET", "/fallback/wallet", 503,
             error="UPSTREAM_UNAVAILABLE", fields=("code", "message", "status", "service"),
             tests=["pm.test('Fallback service', () => pm.expect(pm.response.json().service).to.eql('wallet'));"],
             description="Calls the local handler directly; does not test a circuit opening."),
        case("Malformed bearer token", "GET", "/api/v1/wallets/1", 401, error="UNAUTHORIZED",
             headers=[{"key": "Authorization", "value": "Bearer not-a-jwt"}]),
        case("Fixed-length body over 1 MiB", "POST", "/api/v1/auth/register", 413,
             before=oversized, body="{{gatewayOversizedBody}}", correlation=True,
             headers=[{"key": "Content-Type", "value": "application/json"}],
             description="Generated invalid JSON body cannot create a user. Let Postman calculate Content-Length. Does not test chunked streaming."),
        case("Browser preflight", "OPTIONS", "/api/v1/orders", 200, headers=cors_headers,
             tests=[
                 "pm.test('Origin permitted by current local policy', () => pm.expect(pm.response.headers.get('Access-Control-Allow-Origin')).to.eql(pm.variables.get('gatewayOrigin')));",
                 "pm.test('POST allowed', () => pm.expect(pm.response.headers.get('Access-Control-Allow-Methods') || '').to.include('POST'));",
                 "pm.test('Requested headers allowed', () => {",
                 "  const allowed = (pm.response.headers.get('Access-Control-Allow-Headers') || '').toLowerCase();",
                 "  ['authorization', 'content-type', 'idempotency-key'].forEach(header => pm.expect(allowed).to.include(header));",
                 "});",
             ]),
    ]
    # One unauthenticated 401 sample per routed service family (not every write verb).
    for label, method, path in [
        ("Identity profile", "GET", "/api/v1/users/me"),
        ("Organisation", "GET", "/api/v1/orgs"),
        ("Wallet", "GET", "/api/v1/wallets/1"),
        ("Commerce product", "GET", "/api/v1/products?organisationId=1"),
        ("Notifications", "GET", "/api/v1/notifications/me"),
        ("Unknown protected API", "GET", "/api/v1/gateway-missing-route"),
    ]:
        core.append(case("No JWT - " + label, method, path, 401, error="UNAUTHORIZED"))

    variables = [
        ("gatewayBaseUrl", "http://localhost:18080"),
        ("gatewayOrigin", "http://localhost:3000"),
        ("email", "demo@finflow.test"),
        ("password", "password12"),
        ("memberEmail", "operator@finflow.test"),
        ("memberPassword", "password12"),
        ("accessToken", ""), ("accessToken2", ""),
        ("refreshToken", ""), ("refreshToken2", ""),
        ("userId", ""), ("userId2", ""), ("orgId", ""),
        ("verificationToken", ""), ("inviteToken", ""), ("resetToken", ""),
    ]
    return {
        "info": {
            "_postman_id": "d4e5f6a7-b8c9-0123-def0-234567890123",
            "name": "FinFlow - API Gateway",
            "description": (
                "Edge checks for the gateway, plus identity Auth/Organisations through gatewayBaseUrl. "
                "See api-gateway/postman/README.md. No fixture toggles. "
                "Use wallet/commerce/notification service collections for those APIs. "
                "No real credentials are stored."
            ),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "auth": {"type": "noauth"},
        "variable": [{"key": key, "value": value} for key, value in variables],
        "event": [event("prerequest", [
            "const base = pm.variables.get('gatewayBaseUrl');",
            "if (!base || !/^https?:\\/\\//.test(String(base))) throw new Error('Set gatewayBaseUrl to the gateway under test');",
        ])],
        "item": [
            folder(
                "01 Gateway checks",
                core,
                "Gateway-only edge checks: health, routing errors, fallback, body limit, CORS, and one 401 sample per service family.",
            ),
            folder(
                "02 Identity via Gateway",
                _gateway_identity_via_gateway_items(),
                "Identity Auth + Organisations through gatewayBaseUrl. Run when gateway + identity are up. "
                "identity-service Postman collection is left unchanged.",
            ),
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
        ("categoryId", "", "default"),
        ("productId", "", "default"),
        ("orderId", "", "default"),
        ("returnId", "", "default"),
        ("couponCode", "SAVE10", "default"),
        ("orderKey", "order-demo-001", "default"),
        ("checkoutKey", "checkout-demo-001", "default"),
    ]
    # Drop vars that belonged to removed transaction-service
    for drop in (
        "transactionBaseUrl",
        "transactionId",
        "txDepositKey",
        "txWithdrawKey",
        "txTransferKey",
    ):
        if drop in by_key:
            values[:] = [v for v in values if v["key"] != drop]
            by_key.pop(drop, None)
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


def main(*, gateway_only=False) -> None:
    if gateway_only:
        write(
            ROOT / "api-gateway/postman/FinFlow-API-Gateway.postman_collection.json",
            gateway_collection(),
        )
        return
    write(
        ROOT / "commerce-service/postman/FinFlow-Commerce-Service.postman_collection.json",
        commerce_collection(),
    )
    write(
        ROOT / "notification-service/postman/FinFlow-Notification-Service.postman_collection.json",
        notification_collection(),
    )
    write(
        ROOT / "api-gateway/postman/FinFlow-API-Gateway.postman_collection.json",
        gateway_collection(),
    )
    update_environment()


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--gateway-only", action="store_true", help="Regenerate only gateway tests, preserving other collections and the shared environment.")
    main(gateway_only=parser.parse_args().gateway_only)
