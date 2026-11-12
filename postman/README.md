# FinFlow Postman

## Import

1. `postman/FinFlow-Local.postman_environment.json` → **FinFlow Local**
2. Service collections under each `*-service/postman/`

Re-import after `python _gen_collections.py` if Postman shows stale folders or URLs.

## Identity + Wallet (e-commerce shopper)

1. **Identity → Auth → Public:** Register → Verify email → Login (`wallet:read` / `wallet:write` on verified JWT)
2. **Wallet → Customer:** Open wallet → Top up → List my transactions
3. **Wallet → Integration:** Deposit / withdraw for commerce simulation (uses `{{walletId}}`)

Organizations are optional (merchant admins); see **Organizations** folder in the identity collection.

## Regenerate

```bash
cd postman && python _gen_collections.py
```
