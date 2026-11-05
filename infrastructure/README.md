# FinFlow infrastructure (Docker)

Shared local dependencies for all services.

| Container | Host port | Credentials / notes |
|-----------|-----------|---------------------|
| `mysql` | `3307` | `root` / `root` · DBs created by services: `wallet`, `identity`, `commerce`, `notification`, `transaction` |
| `redis` | `6379` | no password (local only) |
| `kafka` | `9092` | KRaft single-node · topic `commerce.events` |

## Redis model (intentional)

Redis is **infrastructure only** — one Docker instance, like MySQL. There is **no** Redis microservice and **no** `finflow-common` / Redis starter jar yet.

- Each service connects with Spring Data Redis (`spring.data.redis.*`).
- Extract a thin shared Maven helper only after **2+ Spring MVC** services copy the same Redis+fallback pattern (e.g. wallet idempotency cache + identity rate limit). Gateway keeps Spring Cloud `RedisRateLimiter` (reactive) — do not force it into that jar.

### Keyspace convention

Prefix every key with the owning service so one Redis stays multi-tenant-safe locally:

| Prefix | Owner | Example |
|--------|--------|---------|
| `identity:` | identity-service | `identity:ratelimit:{ip-or-email}` |
| `gateway:` | api-gateway | `gateway:user:{sub}` / `gateway:ip:{addr}` (SCG wraps as `request_rate_limiter.{id}.*`) |
| `wallet:` | wallet-service | reserved for future Redis cache / SET NX idempotency |

## Run

```bash
cd infrastructure
docker compose up -d
docker compose ps
```

Stop (keep data):

```bash
docker compose down
```

Wipe data volumes:

```bash
docker compose down -v
```

## Who uses what

| Dependency | Consumers |
|------------|-----------|
| MySQL | `identity` · `wallet` · `commerce` (each service creates its own DB) |
| Redis | `api-gateway` edge rate limit · `identity-service` auth rate limit · `wallet-service` (future, prefixed `wallet:`) |

## Gateway / identity Redis

```properties
# both services
spring.data.redis.host=localhost
spring.data.redis.port=6379

# gateway (always Redis — start compose first)
app.rate-limit.replenish-rate=50
```
