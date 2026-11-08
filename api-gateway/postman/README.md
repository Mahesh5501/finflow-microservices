# Gateway Postman checks

Two folders only: edge checks for the gateway, and identity APIs through `gatewayBaseUrl`. No fixture toggles. The identity-service Postman collection is separate and unchanged. Passing these checks does not establish production readiness.

## Start in Postman

1. Import [the collection](FinFlow-API-Gateway.postman_collection.json). Optionally import the shared [FinFlow Local environment](../../postman/FinFlow-Local.postman_environment.json).
2. Set `gatewayBaseUrl` (default `http://localhost:18080`).
3. Run **01 Gateway checks** with only the gateway up.
4. Run **02 Identity via Gateway** when gateway + identity are up (use unique emails if accounts already exist).

| Folder | Purpose |
|---|---|
| 01 Gateway checks | Health, missing routes, fallback, malformed JWT, body limit, CORS, one 401 sample per service family |
| 02 Identity via Gateway | Full Auth + Organisations happy path via `{{gatewayBaseUrl}}` |

Wallet, commerce and notification flows stay in their own service collections.

## Variables

Collection defaults cover `gatewayBaseUrl`, `gatewayOrigin`, demo emails/passwords, and token/id placeholders filled by folder 02 scripts. Environment values override collection defaults. Keep real tokens local and out of exported/committed files.

## Command-line and regeneration

```powershell
npx --yes --package=newman@6.2.1 newman run api-gateway/postman/FinFlow-API-Gateway.postman_collection.json --env-var gatewayBaseUrl=http://localhost:18080 --folder "01 Gateway checks"
```

Regenerate only the gateway collection from the generator:

```powershell
python postman/_gen_extra_collections.py --gateway-only
```

See the [gateway review](../../docs/GATEWAY_REVIEW.md) for production risks. Postman references: [test scripts](https://learning.postman.com/docs/tests-and-scripts/write-scripts/test-scripts), [Newman CLI](https://learning.postman.com/docs/reference/newman-cli/command-line-integration-with-newman/).
