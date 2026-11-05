# FinFlow microservices

One Compose file (`docker-compose.yml`), project name **finflow**, shared network `finflow-net`.

| Container | Host port | Notes |
|-----------|-----------|--------|
| `mysql` | `3307` | `root` / `root` · inside Docker: `mysql:3306` |
| `redis` | `6379` | no password (local only) |
| `kafka` | `9092` | KRaft single-node |
| `wallet-service` | `18082` | only with `--profile apps` |

## Run

```bash
# infra only (mysql, redis, kafka)
docker compose up -d

# infra + wallet (and later other profile: apps services)
docker compose --profile apps up -d --build

docker compose ps
```

Stop:

```bash
docker compose --profile apps down   # stops everything including wallet
# or without profile — stops only currently running infra containers if apps not started that way
docker compose down
```

Wipe data volumes:

```bash
docker compose down -v
```

Change the Desktop group name via top-level `name: finflow` in the compose file.

## Services (ports)

| Service | Port |
|---------|------|
| api-gateway | `18080` |
| identity-service | `18081` |
| wallet-service | `18082` |
| commerce-service | `18083` |
| notification-service | `18084` |

## Service Docker images

Each service has a multi-stage `Dockerfile` (Maven + JDK 21 → Temurin JRE 21).

```bash
# from a service folder, e.g. wallet-service
docker build -t finflow/wallet-service .
docker run --rm -p 18082:18082 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3307/wallet \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  finflow/wallet-service
```

Same pattern for the others (`finflow/api-gateway`, `finflow/identity-service`, …). Start root `docker compose` first so MySQL/Redis/Kafka are up. Override hostnames with env vars when containers talk to each other (e.g. `mysql` instead of `host.docker.internal`).

## Redis model (intentional)

Redis is shared infra only — one Docker instance, like MySQL. There is **no** Redis microservice and **no** `finflow-common` / Redis starter jar yet.

- Each service connects with Spring Data Redis (`spring.data.redis.*`).
- Extract a thin shared Maven helper only after **2+ Spring MVC** services copy the same Redis+fallback pattern (e.g. wallet idempotency cache + identity rate limit). Gateway keeps Spring Cloud `RedisRateLimiter` (reactive) — do not force it into that jar.

### Keyspace convention

Prefix every key with the owning service so one Redis stays multi-tenant-safe locally:

| Prefix | Owner | Example |
|--------|--------|---------|
| `identity:` | identity-service | `identity:ratelimit:{ip-or-email}` |
| `gateway:` | api-gateway | `gateway:user:{sub}` / `gateway:ip:{addr}` (SCG wraps as `request_rate_limiter.{id}.*`) |
| `wallet:` | wallet-service | reserved for future Redis cache / SET NX idempotency |

## Who uses what

| Dependency | Consumers |
|------------|-----------|
| MySQL | `identity` · `wallet` · `commerce` · `notification` (each creates its own DB) |
| Redis | `api-gateway` edge rate limit · `identity-service` auth rate limit · `wallet-service` (future, prefixed `wallet:`) |

## Gateway / identity Redis

```properties
# both services
spring.data.redis.host=localhost
spring.data.redis.port=6379

# gateway (always Redis — start compose first)
app.rate-limit.replenish-rate=50
```
