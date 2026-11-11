# FinFlow Commerce Service

Java 21 / Spring Boot 3.5.6. Run Maven from this directory with Java 21 selected.

## Standalone local testing

In IntelliJ, set **Active profiles** to `local`, then run `CommerceServiceApplication`.
Alternatively:

```sh
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

- Base URL: `http://127.0.0.1:18083` (call Commerce directly).
- No access token or Identity/Wallet service is required. MySQL and Redis retain their existing configuration and must be available for the operations that use them.
- Requests use configured test user **1**, seller organisation **10**, and commerce read/write permissions. Use `organisationId=10` for merchant requests/Postman. Request bodies, custom headers and unsigned tokens cannot select a different caller.
- `payment.provider=simulated`: checkout/refunds move no wallet money. Approved orders have null wallet references. `paymentOutcome` can simulate APPROVED, DECLINED or UNCERTAIN.
- Configure `commerce.security.local-user-id` and `commerce.security.local-organisation-ids` in your IDE run arguments if different fixtures are needed. Existing ownership/org checks still run against those values.

The bypass requires the `local` profile alone and a loopback `server.address`; otherwise startup fails. It does not disable Wallet or Gateway security. Direct local tests are not a JWT/role-isolation test.

## Restore JWT validation

Remove the `local` active profile and restart. Normal startup defaults to JWT validation enabled and `payment.provider=wallet`.

To keep the local profile/simulator while testing JWTs, add the program argument:

```text
--commerce.security.jwt-validation-enabled=true
```

This requires valid Identity-issued tokens again. For wallet integration testing also set `--payment.provider=wallet` and run Wallet.

See [package structure](PACKAGE-STRUCTURE.md), [architecture and flows](COMMERCE_SERVICE_ARCHITECTURE.md), and [migration/cleanup inventory](docs/FEATURE-FIRST-REFACTOR.md).

For the single collection with embedded standalone defaults and ordered scenarios, see [the Postman guide](postman/README.md).
