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


def main() -> None:
    identity = {
        "info": {
            "name": "FinFlow — Identity Service",
            "description": (
                "Select environment **FinFlow Local** (shared with Wallet).\n\n"
                "Happy path:\n"
                "1. Auth: Register → Verify Email → Login\n"
                "2. Orgs: Create Org → **1b. Login again** (JWT gets wallets:* permissions)\n"
                "3. Switch to Wallet collection — same {{accessToken}} / {{userId}}\n\n"
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
                "name": "Auth",
                "item": [
                    {
                        "name": "0. JWKS (public)",
                        "request": {
                            "method": "GET",
                            "header": [],
                            "url": "{{identityBaseUrl}}/.well-known/jwks.json",
                        },
                    },
                    {
                        "name": "1. Register",
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
                        "name": "2. Verify Email",
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
                        "name": "3. Login",
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
                        "name": "4. Me",
                        "request": {
                            "method": "GET",
                            "header": auth("accessToken"),
                            "url": "{{identityBaseUrl}}/api/v1/users/me",
                        },
                    },
                    {
                        "name": "5. Refresh",
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
                        "name": "6. Forgot Password",
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
                        "name": "7. Reset Password",
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
                        "name": "8. Change Password",
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
                    {
                        "name": "9. Logout",
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
                ],
            },
            {
                "name": "Organisations",
                "item": [
                    {
                        "name": "1. Create Org",
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
                        "name": "1b. Login again (wallet permissions in JWT)",
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
                        "name": "2. List My Orgs",
                        "request": {
                            "method": "GET",
                            "header": auth("accessToken"),
                            "url": "{{identityBaseUrl}}/api/v1/orgs",
                        },
                    },
                    {
                        "name": "3. Register Member (user B)",
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
                        "name": "4. Verify Member Email",
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
                        "name": "5. Invite Member",
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
                        "name": "6. Member Login (before accept)",
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
                    {
                        "name": "7. Accept Invite (as member)",
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
                        "name": "7b. Member Login again (wallet permissions → accessToken2)",
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
                    {
                        "name": "8. List Members",
                        "request": {
                            "method": "GET",
                            "header": auth("accessToken"),
                            "url": "{{identityBaseUrl}}/api/v1/orgs/{{orgId}}/members",
                        },
                    },
                    {
                        "name": "9. Change Member Role",
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
                        "name": "10. Org Audit Log",
                        "request": {
                            "method": "GET",
                            "header": auth("accessToken"),
                            "url": "{{identityBaseUrl}}/api/v1/orgs/{{orgId}}/audit",
                        },
                    },
                    {
                        "name": "11. Remove Member",
                        "request": {
                            "method": "DELETE",
                            "header": auth("accessToken"),
                            "url": "{{identityBaseUrl}}/api/v1/orgs/{{orgId}}/members/{{userId2}}",
                        },
                    },
                ],
            },
        ],
    }

    wallet_items = [
        {
            "name": "1. Create Wallet A",
            "event": [
                {
                    "listen": "test",
                    "script": {
                        "type": "text/javascript",
                        "exec": [
                            "if (pm.response.code === 201) {",
                            "  const body = pm.response.json();",
                            "  if (body.walletId != null) { pm.collectionVariables.set('walletId', String(body.walletId)); pm.environment.set('walletId', String(body.walletId)); }",
                            "  if (body.userId != null) { pm.collectionVariables.set('userId', String(body.userId)); pm.environment.set('userId', String(body.userId)); }",
                            "}",
                        ],
                    },
                }
            ],
            "request": {
                "method": "POST",
                "header": auth("accessToken") + json_ct(),
                "body": {"mode": "raw", "raw": '{\n  "currency": "INR"\n}'},
                "url": "{{walletBaseUrl}}/api/v1/wallets",
            },
        },
        {
            "name": "2. Create Wallet B (accessToken2)",
            "event": [
                {
                    "listen": "test",
                    "script": {
                        "type": "text/javascript",
                        "exec": [
                            "if (pm.response.code === 201) {",
                            "  const body = pm.response.json();",
                            "  if (body.walletId != null) { pm.collectionVariables.set('walletId2', String(body.walletId)); pm.environment.set('walletId2', String(body.walletId)); }",
                            "}",
                        ],
                    },
                }
            ],
            "request": {
                "method": "POST",
                "header": auth("accessToken2") + json_ct(),
                "body": {"mode": "raw", "raw": '{\n  "currency": "INR"\n}'},
                "url": "{{walletBaseUrl}}/api/v1/wallets",
            },
        },
        {
            "name": "3. Get Wallet A",
            "request": {
                "method": "GET",
                "header": auth("accessToken"),
                "url": "{{walletBaseUrl}}/api/v1/wallets/{{walletId}}",
            },
        },
        {
            "name": "3b. Get Wallet by User (self)",
            "request": {
                "method": "GET",
                "header": auth("accessToken"),
                "url": "{{walletBaseUrl}}/api/v1/users/{{userId}}/wallet",
            },
        },
        {
            "name": "4. Deposit A",
            "request": {
                "method": "POST",
                "header": auth("accessToken")
                + json_ct()
                + [{"key": "Idempotency-Key", "value": "{{depositKey}}"}],
                "body": {"mode": "raw", "raw": '{\n  "amount": "100.00"\n}'},
                "url": "{{walletBaseUrl}}/api/v1/wallets/{{walletId}}/deposits",
            },
        },
        {
            "name": "5. Transfer A → B",
            "event": [
                {
                    "listen": "test",
                    "script": {
                        "type": "text/javascript",
                        "exec": [
                            "if (pm.response.code === 200) {",
                            "  const body = pm.response.json();",
                            "  if (body.transferId != null) { pm.collectionVariables.set('transferId', String(body.transferId)); pm.environment.set('transferId', String(body.transferId)); }",
                            "}",
                        ],
                    },
                }
            ],
            "request": {
                "method": "POST",
                "header": auth("accessToken")
                + json_ct()
                + [{"key": "Idempotency-Key", "value": "{{transferKey}}"}],
                "body": {
                    "mode": "raw",
                    "raw": '{\n  "fromWalletId": {{walletId}},\n  "toWalletId": {{walletId2}},\n  "amount": "25.00"\n}',
                },
                "url": "{{walletBaseUrl}}/api/v1/transfers",
            },
        },
        {
            "name": "5b. Get Transfer",
            "request": {
                "method": "GET",
                "header": auth("accessToken"),
                "url": "{{walletBaseUrl}}/api/v1/transfers/{{transferId}}",
            },
        },
        {
            "name": "6. Withdraw A",
            "request": {
                "method": "POST",
                "header": auth("accessToken")
                + json_ct()
                + [{"key": "Idempotency-Key", "value": "{{withdrawKey}}"}],
                "body": {"mode": "raw", "raw": '{\n  "amount": "10.00"\n}'},
                "url": "{{walletBaseUrl}}/api/v1/wallets/{{walletId}}/withdrawals",
            },
        },
        {
            "name": "7. Get Transactions A",
            "request": {
                "method": "GET",
                "header": auth("accessToken"),
                "url": "{{walletBaseUrl}}/api/v1/wallets/{{walletId}}/transactions",
            },
        },
        {
            "name": "8. Block A",
            "request": {
                "method": "POST",
                "header": auth("accessToken"),
                "url": "{{walletBaseUrl}}/api/v1/wallets/{{walletId}}/block",
            },
        },
        {
            "name": "9. Unblock A",
            "request": {
                "method": "POST",
                "header": auth("accessToken"),
                "url": "{{walletBaseUrl}}/api/v1/wallets/{{walletId}}/unblock",
            },
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
                        "url": "{{walletBaseUrl}}/api/v1/wallets",
                    },
                },
                {
                    "name": "Insufficient balance (422)",
                    "request": {
                        "method": "POST",
                        "header": auth("accessToken")
                        + json_ct()
                        + [{"key": "Idempotency-Key", "value": "withdraw-too-much-001"}],
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
    ]

    wallet = {
        "info": {
            "_postman_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            "name": "FinFlow — Wallet Service",
            "description": (
                "Select environment **FinFlow Local** (shared with Identity).\n\n"
                "Requires Identity first:\n"
                "Register → Verify → Login → Create Org → **1b. Login again**\n"
                "(for transfer: invite flow through **7b** so {{accessToken2}} has wallets:write)\n\n"
                "All money/read APIs send: Authorization: Bearer {{accessToken}}\n"
                "URLs use {{walletBaseUrl}} = http://localhost:18082"
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
            {"key": "transferId", "value": ""},
            {"key": "depositKey", "value": "deposit-demo-001"},
            {"key": "withdrawKey", "value": "withdraw-demo-001"},
            {"key": "transferKey", "value": "transfer-demo-001"},
        ],
        "item": wallet_items,
    }

    write(ROOT / "identity-service/postman/FinFlow-Identity-Service.postman_collection.json", identity)
    write(ROOT / "wallet-service/postman/FinFlow-Wallet-Service.postman_collection.json", wallet)

    # Commerce + Notification + Gateway collections + shared env extras
    from _gen_extra_collections import main as gen_extra

    gen_extra()


if __name__ == "__main__":
    main()
