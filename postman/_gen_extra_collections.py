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


def gateway_collection() -> dict:
    """Gateway assertions, with explicit opt-in for live services and controlled fixtures."""

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

    def folder(name, items, description, flag=None):
        result = {"name": name, "description": description, "item": items}
        if flag:
            result["event"] = [event("prerequest", [
                f"if (String(pm.variables.get('{flag}')).toLowerCase() !== 'true') {{",
                f"  console.warn('SKIPPED {name}: set {flag}=true only after preparing its fixtures.');",
                "  pm.execution.skipRequest();",
                "}",
            ])]
        return result

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
    # Every path family and supported business write verb is checked without authorization.
    for label, method, path in [
        ("Identity profile", "GET", "/api/v1/users/me"),
        ("Organisation", "GET", "/api/v1/orgs"),
        ("Wallet by user", "GET", "/api/v1/users/1/wallet"),
        ("Wallet", "GET", "/api/v1/wallets/1"),
        ("Transfer", "GET", "/api/v1/transfers/1"),
        ("Products", "GET", "/api/v1/products?organisationId=1"),
        ("Categories", "GET", "/api/v1/categories?organisationId=1"),
        ("Coupons POST", "POST", "/api/v1/coupons"),
        ("Cart PUT", "PUT", "/api/v1/carts/items/1"),
        ("Cart DELETE", "DELETE", "/api/v1/carts?organisationId=1"),
        ("Product PATCH", "PATCH", "/api/v1/products/1"),
        ("Order POST", "POST", "/api/v1/orders"),
        ("Return POST", "POST", "/api/v1/returns/1/approve"),
        ("Notifications", "GET", "/api/v1/notifications/me"),
        ("Unknown protected API", "GET", "/api/v1/gateway-missing-route"),
    ]:
        core.append(case("No JWT - " + label, method, path, 401, error="UNAUTHORIZED"))

    live = [
        case("JWKS via Gateway", "GET", "/.well-known/jwks.json", 200, fields=("keys",), correlation=True,
             tests=[
                 "pm.test('Public RSA signing keys', () => {",
                 "  const keys = pm.response.json().keys;",
                 "  pm.expect(keys).to.be.an('array').and.not.empty;",
                 "  keys.forEach(key => { ['kid', 'kty', 'n', 'e'].forEach(field => pm.expect(key).to.have.property(field)); pm.expect(key.kty).to.eql('RSA'); pm.expect(key).not.to.have.property('d'); });",
                 "});",
             ]),
        case("Me (profile)", "GET", "/api/v1/users/me", 200, token="accessToken", required=("userId",),
             fields=("userId", "email", "orgIds", "permissions"), correlation=True,
             tests=["pm.test('Own profile', () => pm.expect(String(pm.response.json().userId)).to.eql(String(pm.variables.get('userId'))));"]),
        case("List My Orgs", "GET", "/api/v1/orgs", 200, token="accessToken", array=True, correlation=True),
        case("Wallet by User", "GET", "/api/v1/users/{{userId}}/wallet", 200,
             token="accessToken", required=("userId", "walletId"), fields=("walletId", "userId", "balance", "currency"),
             correlation=True, tests=[
                 "pm.test('Own wallet mapping', () => { const body = pm.response.json(); pm.expect(String(body.userId)).to.eql(String(pm.variables.get('userId'))); pm.expect(String(body.walletId)).to.eql(String(pm.variables.get('walletId'))); });"
             ]),
        case("Wallet by ID", "GET", "/api/v1/wallets/{{walletId}}", 200,
             token="accessToken", required=("walletId", "userId"), fields=("walletId", "userId", "balance"),
             correlation=True, tests=[
                 "pm.test('Requested wallet', () => pm.expect(String(pm.response.json().walletId)).to.eql(String(pm.variables.get('walletId'))));"
             ]),
        case("Wallet history", "GET", "/api/v1/wallets/{{walletId}}/transactions", 200,
             token="accessToken", required=("walletId",), array=True, correlation=True),
        case("List Products", "GET", "/api/v1/products?organisationId={{orgId}}", 200,
             token="accessToken", required=("orgId",), array=True, correlation=True),
        case("Search Products", "GET", "/api/v1/products/search?organisationId={{orgId}}&page=0&size=20", 200,
             token="accessToken", required=("orgId",), fields=("content", "totalElements"), correlation=True,
             tests=["pm.test('Page content', () => pm.expect(pm.response.json().content).to.be.an('array'));"]),
        case("List Categories", "GET", "/api/v1/categories?organisationId={{orgId}}", 200,
             token="accessToken", required=("orgId",), array=True, correlation=True),
        case("Get Cart", "GET", "/api/v1/carts?organisationId={{orgId}}", 200,
             token="accessToken", required=("orgId",), fields=("organisationId", "customerUserId", "items"),
             correlation=True, tests=[
                 "pm.test('Own organisation cart', () => pm.expect(String(pm.response.json().organisationId)).to.eql(String(pm.variables.get('orgId'))));"
             ]),
        case("List Orders", "GET", "/api/v1/orders?organisationId={{orgId}}", 200,
             token="accessToken", required=("orgId",), array=True, correlation=True),
        case("My Notifications", "GET", "/api/v1/notifications/me", 200,
             token="accessToken", array=True, correlation=True),
        case("Aggregate Health", "GET", "/actuator/health", 200, fields=("status",),
             tests=["pm.test('Dependencies healthy', () => pm.expect(pm.response.json().status).to.eql('UP'));"],
             description="A 503 is a failure here, not accepted as healthy. Redis must be reachable."),
        case("Prometheus - current exposure", "GET", "/actuator/prometheus", 200,
             tests=["pm.test('Prometheus exposition', () => pm.expect(pm.response.text()).to.include('# HELP'));"],
             description="Current local behavior only; private access is tested separately as a production target."),
    ]
    auth_cases = [
        case("Expired token rejected", "GET", "/api/v1/users/me", 401,
             token="expiredAccessToken", error="UNAUTHORIZED",
             description="Supply a formerly valid identity-issued token whose exp has passed; no generated fake token."),
        case("Tampered signature rejected", "GET", "/api/v1/users/me", 401, token="gatewayTamperedJwt", generated_token=True,
             error="UNAUTHORIZED", required=("accessToken",), before=[
                 "const parts = String(pm.variables.get('accessToken')).split('.');",
                 "if (parts.length !== 3 || !parts[2]) throw new Error('Provide a valid accessToken for signature tampering');",
                 "parts[2] = (parts[2][0] === 'A' ? 'B' : 'A') + parts[2].slice(1);",
                 "pm.variables.set('gatewayTamperedJwt', parts.join('.'));",
             ]),
        case("Missing wallet permission", "GET", "/api/v1/wallets/{{walletId}}", 403,
             token="noPermissionAccessToken", required=("walletId",), error="FORBIDDEN",
             description="Valid unexpired identity JWT without wallets:read. A malformed/expired token should fail this test with 401."),
        case("Another user's wallet denied", "GET", "/api/v1/wallets/{{walletId2}}", 403,
             token="accessToken", required=("walletId2",), error="FORBIDDEN",
             description="walletId2 must belong to a different user; caller must have wallets:read."),
        case("Another organisation denied", "GET", "/api/v1/products?organisationId={{otherOrgId}}", 403,
             token="accessToken", required=("otherOrgId",), error="FORBIDDEN",
             description="otherOrgId must not be in caller orgIds; caller must have commerce:read."),
    ]
    faults = [
        case("Unavailable identity fallback", "GET", "/.well-known/jwks.json", 503,
             base="gatewayUnavailableBaseUrl", required=("gatewayUnavailableBaseUrl",),
             error="UPSTREAM_UNAVAILABLE", correlation=True,
             tests=["pm.test('Identity fallback', () => pm.expect(pm.response.json().service).to.eql('identity'));"],
             description="Separate gateway instance with identity.uri pointing to an unused loopback port. Do not stop a real shared service."),
        case("Backend HTTP500 after GET retries", "GET", "/.well-known/jwks.json", 500,
             base="gatewayHttp500BaseUrl", required=("gatewayHttp500BaseUrl",), correlation=True,
             description="Separate gateway with a controlled identity mock returning 500. Verify three backend attempts in mock logs; response alone cannot prove the count."),
        case("Backend HTTP500 POST is not retried", "POST", "/api/v1/auth/login", 500,
             base="gatewayHttp500BaseUrl", required=("gatewayHttp500BaseUrl",), correlation=True,
             body="{}", headers=[{"key": "Content-Type", "value": "application/json"}],
             description="Controlled mock only; no valid credentials are sent. Verify one POST in mock logs, not just the final response."),
    ]
    rate = case("Bounded anonymous quota burst", "GET", "/actuator/info", 200,
                base="gatewayRateBaseUrl", required=("gatewayRateBaseUrl",),
                description="Isolated gateway, Redis UP, fast JWKS backend, replenish=1/burst=1. Sends 20 read-only requests; never run as a production load test.",
                tests=[
                    "const base = String(pm.variables.get('gatewayRateBaseUrl')).replace(/\\/$/, '');",
                    "const results = []; let pending = 20;",
                    "for (let i = 0; i < 20; i++) {",
                    "  pm.sendRequest({url: base + '/.well-known/jwks.json', method: 'GET'}, (err, response) => {",
                    "    results.push(err ? 0 : response.code);",
                    "    if (--pending === 0) {",
                    "      pm.test('All burst requests completed without transport error', () => pm.expect(results).not.to.include(0));",
                    "      pm.test('Only successful or rate-limited responses', () => results.forEach(code => pm.expect([200, 429]).to.include(code)));",
                    "      pm.test('Quota was enforced', () => pm.expect(results).to.include(429));",
                    "      pm.test('At least one request reached JWKS', () => pm.expect(results).to.include(200));",
                    "    }",
                    "  });",
                    "}",
                ])
    gaps = [
        case("TARGET - metrics are private", "GET", "/actuator/metrics", 401,
             description="Expected to FAIL current local implementation (200). Agree final operational auth policy before implementing."),
        case("TARGET - Prometheus is private", "GET", "/actuator/prometheus", 401,
             description="Expected to FAIL current local implementation (200)."),
        case("TARGET - 401 has correlation ID", "GET", "/api/v1/wallets/1", 401, error="UNAUTHORIZED",
             correlation=True, description="Status passes today; correlation assertion should FAIL until early request correlation is implemented."),
        case("TARGET - 401 has bearer challenge", "GET", "/api/v1/wallets/1", 401,
             tests=["pm.test('Bearer challenge present', () => pm.expect(pm.response.headers.get('WWW-Authenticate') || '').to.match(/^Bearer/i));"],
             description="Expected to FAIL current implementation."),
        case("TARGET - oversized response has JSON code", "POST", "/api/v1/auth/register", 413,
             before=oversized, body="{{gatewayOversizedBody}}",
             headers=[{"key": "Content-Type", "value": "application/json"}], error="PAYLOAD_TOO_LARGE",
             description="Expected to FAIL current empty-body response. Does not test streamed byte limits."),
    ]
    variables = [
        ("gatewayBaseUrl", "http://localhost:18080"),
        ("gatewayOrigin", "http://localhost:3000"),
        ("accessToken", ""), ("userId", ""), ("orgId", ""), ("walletId", ""), ("walletId2", ""),
        ("expiredAccessToken", ""), ("noPermissionAccessToken", ""), ("otherOrgId", ""),
        ("gatewayRunAuthCases", "false"),
        ("gatewayRunFaults", "false"), ("gatewayRunRateLimit", "false"), ("gatewayRunKnownGaps", "false"),
        ("gatewayUnavailableBaseUrl", ""), ("gatewayHttp500BaseUrl", ""), ("gatewayRateBaseUrl", ""),
    ]
    return {
        "info": {
            "_postman_id": "d4e5f6a7-b8c9-0123-def0-234567890123",
            "name": "FinFlow - API Gateway",
            "description": (
                "Gateway assertions, not a complete business API or production certification suite. "
                "See api-gateway/postman/README.md for prerequisites and coverage limits. "
                "Gateway checks and live routing run without a switch; special fixture folders skip until enabled. "
                "No real credentials are stored. Known-gap targets intentionally fail current behavior."
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
            folder("01 Gateway checks", core, "No credentials or downstream business calls needed. Includes a generated 1 MiB+1 invalid-JSON request, rejected by the edge."),
            folder("02 Live routing", live, "No enable switch. Send individual requests for the service running locally. Wallet requests need gateway, wallet, identity/JWKS and prepared token/user/wallet fixtures; the full folder needs all services."),
            folder("03 Auth and ownership fixtures", auth_cases, "Real identity-issued expired/no-permission tokens and different-user/org resources required.", "gatewayRunAuthCases"),
            folder("04 Controlled downstream faults", faults, "Separate isolated gateway instances only; no automatic shutdown of shared services.", "gatewayRunFaults"),
            folder("05 Isolated rate-limit fixture", [rate], "20 asynchronous read-only requests against a low-quota test gateway, not production.", "gatewayRunRateLimit"),
            folder("06 Known production gaps", gaps, "Opt-in desired-security targets; current code should fail these checks. Not a passing baseline.", "gatewayRunKnownGaps"),
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
