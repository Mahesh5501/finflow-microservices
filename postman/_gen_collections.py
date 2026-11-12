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


def req(method, url, headers=None, body=None, tests=None):
    item = {"request": {"method": method, "header": headers or [], "url": url}}
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


def save_tokens(primary=True):
    at = "accessToken" if primary else "accessToken2"
    rt = "refreshToken" if primary else "refreshToken2"
    uid = "userId" if primary else "userId2"
    return [
        "if (pm.response.code === 200) {",
        "  const body = pm.response.json();",
        f"  if (body.accessToken) {{ pm.collectionVariables.set('{at}', body.accessToken); pm.environment.set('{at}', body.accessToken); }}",
        f"  if (body.refreshToken) {{ pm.collectionVariables.set('{rt}', body.refreshToken); pm.environment.set('{rt}', body.refreshToken); }}",
        f"  if (body.userId != null) {{ pm.collectionVariables.set('{uid}', String(body.userId)); pm.environment.set('{uid}', String(body.userId)); }}",
        "  if (body.permissions) console.log('permissions', body.permissions);",
        "}",
    ]


def write(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print("wrote", path)


def main(*, identity_only=False) -> None:
    identity = {
        "info": {
            "name": "FinFlow — Identity Service",
            "description": (
                "Select environment **FinFlow Local** (shared with Wallet).\n\n"
                "Happy path:\n"
                "1. Auth: Register → Verify Email → Login\n"
                "   (If already registered but unverified: skip Register, run **1b. Resend verification** → Verify Email → Login)\n"
                "2. Optional: Organizations (merchant admins)\n"
                "3. Wallet collection — same {{accessToken}}; shopper APIs need wallet:read / wallet:write on JWT (email verified)\n\n"
                "URLs use {{identityBaseUrl}} = http://localhost:18081"
            ),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "variable": [
            {"key": "identityBaseUrl", "value": "http://localhost:18081"},
            {"key": "email", "value": "demo@finflow.test"},
            {"key": "password", "value": "password12"},
            {"key": "memberEmail", "value": "operator@finflow.test"},
            {"key": "memberPassword", "value": "password12"},
            {"key": "accessToken", "value": ""},
            {"key": "accessToken2", "value": ""},
            {"key": "refreshToken", "value": ""},
            {"key": "refreshToken2", "value": ""},
            {"key": "userId", "value": ""},
            {"key": "userId2", "value": ""},
            {"key": "orgId", "value": ""},
            {"key": "verificationToken", "value": ""},
            {"key": "inviteToken", "value": ""},
            {"key": "resetToken", "value": ""},
        ],
        "item": [
            {
                "name": "Auth (auth.api)",
                "description": "AuthController + JwksController",
                "item": [
                    {
                        "name": "Public",
                        "item": [],
                    },
                    {
                        "name": "Authenticated",
                        "item": [],
                    },
                ],
            },
            {
                "name": "Organizations (organization.api)",
                "description": "OrgController — /api/v1/orgs/**",
                "item": [],
            },
        ],
    }

    auth_public = identity["item"][0]["item"][0]["item"]
    auth_authenticated = identity["item"][0]["item"][1]["item"]
    org_items = identity["item"][1]["item"]

    auth_public.extend(
        [
            {
                "name": "JWKS",
                "request": {
                    "method": "GET",
                    "header": [],
                    "url": "{{identityBaseUrl}}/.well-known/jwks.json",
                },
            },
            {
                "name": "Register",
                        "event": [
                            {
                                "listen": "test",
                                "script": {
                                    "type": "text/javascript",
                                    "exec": [
                                        "if (pm.response.code === 201) {",
                                        "  const body = pm.response.json();",
                                        "  if (body.token) { pm.collectionVariables.set('verificationToken', body.token); pm.environment.set('verificationToken', body.token); }",
                                        "  if (body.profile && body.profile.userId != null) { pm.collectionVariables.set('userId', String(body.profile.userId)); pm.environment.set('userId', String(body.profile.userId)); }",
                                        "}",
                                    ],
                                },
                            }
                        ],
                        "request": {
                            "method": "POST",
                            "header": json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "email": "{{email}}",\n  "password": "{{password}}"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/auth/register",
                        },
                    },
                    {
                "name": "Resend verification",
                        "event": [
                            {
                                "listen": "test",
                                "script": {
                                    "type": "text/javascript",
                                    "exec": [
                                        "if (pm.response.code === 200) {",
                                        "  const body = pm.response.json();",
                                        "  if (body.token) { pm.collectionVariables.set('verificationToken', body.token); pm.environment.set('verificationToken', body.token); }",
                                        "}",
                                    ],
                                },
                            }
                        ],
                        "request": {
                            "method": "POST",
                            "header": json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "email": "{{email}}"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/auth/resend-verification",
                        },
                    },
                    {
                "name": "Verify email",
                        "request": {
                            "method": "POST",
                            "header": json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "token": "{{verificationToken}}"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/auth/verify-email",
                        },
                    },
                    {
                "name": "Login",
                        "event": [
                            {
                                "listen": "test",
                                "script": {
                                    "type": "text/javascript",
                                    "exec": save_tokens(True),
                                },
                            }
                        ],
                        "request": {
                            "method": "POST",
                            "header": json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "email": "{{email}}",\n  "password": "{{password}}"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/auth/login",
                        },
                    },
            {
                "name": "Refresh",
                        "event": [
                            {
                                "listen": "test",
                                "script": {
                                    "type": "text/javascript",
                                    "exec": save_tokens(True),
                                },
                            }
                        ],
                        "request": {
                            "method": "POST",
                            "header": json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "refreshToken": "{{refreshToken}}"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/auth/refresh",
                        },
                    },
                    {
                "name": "Forgot password",
                        "event": [
                            {
                                "listen": "test",
                                "script": {
                                    "type": "text/javascript",
                                    "exec": [
                                        "if (pm.response.code === 200) {",
                                        "  const body = pm.response.json();",
                                        "  if (body.token) { pm.collectionVariables.set('resetToken', body.token); pm.environment.set('resetToken', body.token); }",
                                        "}",
                                    ],
                                },
                            }
                        ],
                        "request": {
                            "method": "POST",
                            "header": json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "email": "{{email}}"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/auth/forgot-password",
                        },
                    },
                    {
                "name": "Reset password",
                        "request": {
                            "method": "POST",
                            "header": json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "resetToken": "{{resetToken}}",\n  "newPassword": "{{password}}"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/auth/reset-password",
                        },
                    },
            {
                "name": "Logout",
                        "request": {
                            "method": "POST",
                            "header": json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "refreshToken": "{{refreshToken}}"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/auth/logout",
                        },
            },
        ]
    )

    auth_authenticated.extend(
        [
            {
                "name": "Me",
                "request": {
                    "method": "GET",
                    "header": auth("accessToken"),
                    "url": "{{identityBaseUrl}}/api/v1/users/me",
                },
            },
            {
                "name": "Change password",
                "request": {
                    "method": "POST",
                    "header": auth("accessToken") + json_ct(),
                    "body": {
                        "mode": "raw",
                        "raw": '{\n  "currentPassword": "{{password}}",\n  "newPassword": "{{password}}"\n}',
                    },
                    "url": "{{identityBaseUrl}}/api/v1/auth/change-password",
                },
            },
        ]
    )

    org_items.extend(
        [
            {
                "name": "Org",
                "item": [
                    {
                        "name": "Create org",
                        "event": [
                            {
                                "listen": "test",
                                "script": {
                                    "type": "text/javascript",
                                    "exec": [
                                        "if (pm.response.code === 201) {",
                                        "  const body = pm.response.json();",
                                        "  if (body.orgId != null) { pm.collectionVariables.set('orgId', String(body.orgId)); pm.environment.set('orgId', String(body.orgId)); }",
                                        "}",
                                    ],
                                },
                            }
                        ],
                        "request": {
                            "method": "POST",
                            "header": auth("accessToken") + json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "name": "Acme Finance"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/orgs",
                        },
                    },
                    {
                        "name": "List my orgs",
                        "request": {
                            "method": "GET",
                            "header": auth("accessToken"),
                            "url": "{{identityBaseUrl}}/api/v1/orgs",
                        },
                    },
                    {
                        "name": "Get org",
                        "request": {
                            "method": "GET",
                            "header": auth("accessToken"),
                            "url": "{{identityBaseUrl}}/api/v1/orgs/{{orgId}}",
                        },
                    },
                ],
            },
            {
                "name": "Members & invites",
                "item": [
                    {
                        "name": "Invite member",
                        "event": [
                            {
                                "listen": "test",
                                "script": {
                                    "type": "text/javascript",
                                    "exec": [
                                        "if (pm.response.code === 201) {",
                                        "  const body = pm.response.json();",
                                        "  if (body.token) { pm.collectionVariables.set('inviteToken', body.token); pm.environment.set('inviteToken', body.token); }",
                                        "}",
                                    ],
                                },
                            }
                        ],
                        "request": {
                            "method": "POST",
                            "header": auth("accessToken") + json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "email": "{{memberEmail}}",\n  "role": "FINANCE_OPERATOR"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/orgs/{{orgId}}/invites",
                        },
                    },
                    {
                        "name": "Accept invite (member Bearer)",
                        "request": {
                            "method": "POST",
                            "header": auth("accessToken2") + json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "token": "{{inviteToken}}"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/orgs/invites/accept",
                        },
                    },
                    {
                        "name": "List members",
                        "request": {
                            "method": "GET",
                            "header": auth("accessToken"),
                            "url": "{{identityBaseUrl}}/api/v1/orgs/{{orgId}}/members",
                        },
                    },
                    {
                        "name": "Change member role",
                        "request": {
                            "method": "PUT",
                            "header": auth("accessToken") + json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "role": "MEMBER"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/orgs/{{orgId}}/members/{{userId2}}/role",
                        },
                    },
                    {
                        "name": "Remove member",
                        "request": {
                            "method": "DELETE",
                            "header": auth("accessToken"),
                            "url": "{{identityBaseUrl}}/api/v1/orgs/{{orgId}}/members/{{userId2}}",
                        },
                    },
                ],
            },
            {
                "name": "Audit",
                "item": [
                    {
                        "name": "Org audit log",
                        "request": {
                            "method": "GET",
                            "header": auth("accessToken"),
                            "url": "{{identityBaseUrl}}/api/v1/orgs/{{orgId}}/audit",
                        },
                    },
                ],
            },
            {
                "name": "Demo — second user (optional)",
                "description": "Registers {{memberEmail}}, logs in as member, then run Members & invites.",
                "item": [
                    {
                        "name": "Register member (user B)",
                        "event": [
                            {
                                "listen": "test",
                                "script": {
                                    "type": "text/javascript",
                                    "exec": [
                                        "if (pm.response.code === 201) {",
                                        "  const body = pm.response.json();",
                                        "  if (body.token) { pm.collectionVariables.set('verificationToken', body.token); pm.environment.set('verificationToken', body.token); }",
                                        "  if (body.profile && body.profile.userId != null) { pm.collectionVariables.set('userId2', String(body.profile.userId)); pm.environment.set('userId2', String(body.profile.userId)); }",
                                        "}",
                                    ],
                                },
                            }
                        ],
                        "request": {
                            "method": "POST",
                            "header": json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "email": "{{memberEmail}}",\n  "password": "{{memberPassword}}"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/auth/register",
                        },
                    },
                    {
                        "name": "Verify member email",
                        "request": {
                            "method": "POST",
                            "header": json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "token": "{{verificationToken}}"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/auth/verify-email",
                        },
                    },
                    {
                        "name": "Member login",
                        "event": [
                            {
                                "listen": "test",
                                "script": {
                                    "type": "text/javascript",
                                    "exec": save_tokens(False),
                                },
                            }
                        ],
                        "request": {
                            "method": "POST",
                            "header": json_ct(),
                            "body": {
                                "mode": "raw",
                                "raw": '{\n  "email": "{{memberEmail}}",\n  "password": "{{memberPassword}}"\n}',
                            },
                            "url": "{{identityBaseUrl}}/api/v1/auth/login",
                        },
                    },
                ],
            },
        ]
    )

    save_wallet_a = [
        "if (pm.response.code === 200 || pm.response.code === 201) {",
        "  const body = pm.response.json();",
        "  if (body.walletId != null) { pm.collectionVariables.set('walletId', String(body.walletId)); pm.environment.set('walletId', String(body.walletId)); }",
        "  if (body.userId != null) { pm.collectionVariables.set('userId', String(body.userId)); pm.environment.set('userId', String(body.userId)); }",
        "}",
    ]
    save_wallet_b = [
        "if (pm.response.code === 200 || pm.response.code === 201) {",
        "  const body = pm.response.json();",
        "  if (body.walletId != null) { pm.collectionVariables.set('walletId2', String(body.walletId)); pm.environment.set('walletId2', String(body.walletId)); }",
        "}",
    ]

    customer_folder = {
        "name": "Customer (customer.api)",
        "description": "CustomerWalletController — /api/v1/customer/wallet/** (wallet:read / wallet:write)",
        "item": [
            {
                "name": "Open wallet",
                "event": [{"listen": "test", "script": {"type": "text/javascript", "exec": save_wallet_a}}],
                "request": {
                    "method": "POST",
                    "header": auth("accessToken") + json_ct(),
                    "body": {"mode": "raw", "raw": '{\n  "currency": "INR"\n}'},
                    "url": "{{walletBaseUrl}}/api/v1/customer/wallet",
                },
            },
            {
                "name": "Get my wallet",
                "request": {
                    "method": "GET",
                    "header": auth("accessToken"),
                    "url": "{{walletBaseUrl}}/api/v1/customer/wallet",
                },
            },
            {
                "name": "Top up",
                "request": {
                    "method": "POST",
                    "header": auth("accessToken") + json_ct() + idem("topUpKey"),
                    "body": {"mode": "raw", "raw": '{\n  "amount": "500.00",\n  "paymentMethod": "UPI",\n  "paymentReference": "pg-demo-001"\n}'},
                    "url": "{{walletBaseUrl}}/api/v1/customer/wallet/top-ups",
                },
            },
            {
                "name": "List my transactions",
                "request": {
                    "method": "GET",
                    "header": auth("accessToken"),
                    "url": "{{walletBaseUrl}}/api/v1/customer/wallet/transactions",
                },
            },
        ],
    }

    integration_folder = {
        "name": "Integration (integration.api)",
        "description": "IntegrationWalletController — commerce / ops (same JWT; wallet:write for money moves)",
        "item": [
            {
                "name": "Prerequisites",
                "description": "Open customer wallet first so {{walletId}} is set.",
                "item": [
                    {
                        "name": "Open wallet B (accessToken2)",
                        "event": [{"listen": "test", "script": {"type": "text/javascript", "exec": save_wallet_b}}],
                        "request": {
                            "method": "POST",
                            "header": auth("accessToken2") + json_ct(),
                            "body": {"mode": "raw", "raw": '{\n  "currency": "INR"\n}'},
                            "url": "{{walletBaseUrl}}/api/v1/customer/wallet",
                        },
                    },
                ],
            },
            {
                "name": "Read",
                "item": [
                    {
                        "name": "Get wallet by user id",
                        "request": {
                            "method": "GET",
                            "header": auth("accessToken"),
                            "url": "{{walletBaseUrl}}/api/v1/users/{{userId}}/wallet",
                        },
                    },
                    {
                        "name": "Get wallet by wallet id",
                        "request": {
                            "method": "GET",
                            "header": auth("accessToken"),
                            "url": "{{walletBaseUrl}}/api/v1/wallets/{{walletId}}",
                        },
                    },
                    {
                        "name": "List transactions",
                        "request": {
                            "method": "GET",
                            "header": auth("accessToken"),
                            "url": "{{walletBaseUrl}}/api/v1/wallets/{{walletId}}/transactions",
                        },
                    },
                ],
            },
            {
                "name": "Money moves",
                "item": [
                    {
                        "name": "Deposit (refund / credit)",
                        "request": {
                            "method": "POST",
                            "header": auth("accessToken") + json_ct() + idem("depositKey"),
                            "body": {"mode": "raw", "raw": '{\n  "amount": "100.00"\n}'},
                            "url": "{{walletBaseUrl}}/api/v1/wallets/{{walletId}}/deposits",
                        },
                    },
                    {
                        "name": "Withdraw (checkout / debit)",
                        "request": {
                            "method": "POST",
                            "header": auth("accessToken") + json_ct() + idem("withdrawKey"),
                            "body": {"mode": "raw", "raw": '{\n  "amount": "10.00"\n}'},
                            "url": "{{walletBaseUrl}}/api/v1/wallets/{{walletId}}/withdrawals",
                        },
                    },
                ],
            },
            {
                "name": "Negative cases",
                "item": [
                    {
                        "name": "No token (401)",
                        "request": {
                            "method": "GET",
                            "header": [],
                            "url": "{{walletBaseUrl}}/api/v1/wallets/{{walletId}}",
                        },
                    },
                    {
                        "name": "Invalid currency (400)",
                        "request": {
                            "method": "POST",
                            "header": auth("accessToken") + json_ct(),
                            "body": {"mode": "raw", "raw": '{\n  "currency": "XYZ"\n}'},
                            "url": "{{walletBaseUrl}}/api/v1/customer/wallet",
                        },
                    },
                    {
                        "name": "Insufficient balance (422)",
                        "request": {
                            "method": "POST",
                            "header": auth("accessToken") + json_ct() + [{"key": "Idempotency-Key", "value": "withdraw-too-much-001"}],
                            "body": {"mode": "raw", "raw": '{\n  "amount": "999999.00"\n}'},
                            "url": "{{walletBaseUrl}}/api/v1/wallets/{{walletId}}/withdrawals",
                        },
                    },
                    {
                        "name": "Missing Idempotency-Key (400)",
                        "request": {
                            "method": "POST",
                            "header": auth("accessToken") + json_ct(),
                            "body": {"mode": "raw", "raw": '{\n  "amount": "10.00"\n}'},
                            "url": "{{walletBaseUrl}}/api/v1/wallets/{{walletId}}/deposits",
                        },
                    },
                ],
            },
        ],
    }

    wallet = {
        "info": {
            "_postman_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            "name": "FinFlow — Wallet Service",
            "description": (
                "Customer folder = shopper UI (CustomerWalletController).\n"
                "Integration folder = commerce checkout/refunds (IntegrationWalletController).\n\n"
                "Identity: Register → Verify → Login (JWT needs wallet:read / wallet:write when email is verified).\n\n"
                "Bearer {{accessToken}} · {{walletBaseUrl}} = http://localhost:18082"
            ),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "variable": [
            {"key": "walletBaseUrl", "value": "http://localhost:18082"},
            {"key": "accessToken", "value": ""},
            {"key": "accessToken2", "value": ""},
            {"key": "userId", "value": ""},
            {"key": "walletId", "value": ""},
            {"key": "walletId2", "value": ""},
            {"key": "topUpKey", "value": "topup-demo-001"},
            {"key": "depositKey", "value": "deposit-demo-001"},
            {"key": "withdrawKey", "value": "withdraw-demo-001"},
        ],
        "item": [customer_folder, integration_folder],
    }

    # Direct identity checks: gateway may reject the request before identity sees it.
    security_checks = []
    for name, headers in [
        ("Missing bearer token", []),
        ("Malformed bearer token", [{"key": "Authorization", "value": "Bearer not-a-jwt"}]),
    ]:
        item = req("GET", "{{identityBaseUrl}}/api/v1/users/me", headers, tests=[
            "pm.test('HTTP 401', () => pm.response.to.have.status(401));",
            "pm.test('JSON response', () => pm.expect(pm.response.headers.get('Content-Type') || '').to.include('application/json'));",
            "pm.test('Authentication error', () => { const body = pm.response.json(); pm.expect(body.code).to.eql('UNAUTHORIZED'); pm.expect(body.message).to.eql('Valid Bearer access token required'); });",
            "pm.test('Bearer challenge', () => pm.expect(pm.response.headers.get('WWW-Authenticate')).to.eql('Bearer'));",
        ])
        item["name"] = name
        item["request"]["auth"] = {"type": "noauth"}
        security_checks.append(item)
    item = req("GET", "{{identityBaseUrl}}/.well-known/jwks.json", tests=[
        "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
        "pm.test('Public RSA keys', () => { const keys = pm.response.json().keys; pm.expect(keys).to.be.an('array').and.not.empty; keys.forEach(key => { pm.expect(key.kty).to.eql('RSA'); pm.expect(key).not.to.have.property('d'); }); });",
    ])
    item["name"] = "Public JWKS without bearer token"
    item["request"]["auth"] = {"type": "noauth"}
    security_checks.append(item)
    identity["item"].append({
        "name": "Security response checks",
        "description": "Read-only checks; no credentials or account setup needed. Run directly against identityBaseUrl (default :18081), not gatewayBaseUrl. For authenticated permission-denial checks use the HTTP regression suite with controlled H2 fixtures.",
        "item": security_checks,
    })
    write(ROOT / "identity-service/postman/FinFlow-Identity-Service.postman_collection.json", identity)
    if identity_only:
        return
    write(ROOT / "wallet-service/postman/FinFlow-Wallet-Service.postman_collection.json", wallet)

    # Commerce + Notification + Gateway collections + shared env extras
    from _gen_extra_collections import main as gen_extra

    gen_extra()


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Regenerate FinFlow Postman collections.")
    parser.add_argument("--identity-only", action="store_true", help="Update identity only; preserve other collections and environment.")
    main(identity_only=parser.parse_args().identity_only)
