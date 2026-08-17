# Deployment

Short guide to deploying this backend. Everything runs via `docker compose`.

> ⚠️ **Run exactly ONE app instance** — state lives in JVM memory and local disk.
> → [DEPLOYMENT_CONSTRAINTS.md](DEPLOYMENT_CONSTRAINTS.md)

## 0. Server sizing

The stack is 10 containers on one host (app, Postgres, Redis, RabbitMQ, backup
job, Prometheus, Alertmanager, Grafana, 2 exporters). **Memory is the binding
constraint, not CPU** — at MVP scale (~100 restaurants / 100 couriers / 100
customers) the request load is trivial; the containers' baseline footprint is not.

| | vCPU | RAM | SSD | Notes |
|---|------|-----|-----|-------|
| **Recommended** | 4 | **8 GB** | 60 GB | Comfortable; room for growth and a rebuild while running |
| **Minimum** | 2 | **4 GB** | 40 GB | Works, but tight — see the trim option below |
| Too small | — | 2 GB | — | The JVM alone wants ~1 GB; do not attempt |

Per-container limits are set in `docker-compose.yml` (`mem_limit`) and total
**~4.2 GB**, leaving headroom for the OS and Docker on an 8 GB box.

**On a 4 GB VPS**, drop the monitoring stack (frees ~1 GB) and keep alerting via
an external uptime monitor:

```bash
docker compose up -d app postgres postgres-backup redis rabbitmq
```

Disk goes mostly to: Docker images (~3 GB), Prometheus 15-day retention (~2 GB),
7 days of DB backups, and **restaurant/menu images on local disk** — 100
restaurants with photos is roughly 0.5–1 GB and grows.

> ⚠️ **Never run the app container without a memory limit.** The JVM sizes its
> heap from the container limit (`MaxRAMPercentage`); with no limit it sizes
> against the *whole host*, claims most of it, and the kernel OOM-killer takes
> out Postgres. The compose file sets these — keep them if you edit it.

Vertical scaling is the only option: the app tier is **single-instance**
([DEPLOYMENT_CONSTRAINTS.md](DEPLOYMENT_CONSTRAINTS.md)).

## 1. Configure secrets

```bash
cp .env.example .env      # production   (cp .env.dev.example .env for local)
```

Compose **refuses to start** unless these are set:

| Variable | How to get it |
|----------|---------------|
| `JWT_SECRET` | `openssl rand -base64 48` (≥32 chars) |
| `DB_PASSWORD` | choose a strong password |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | choose |
| `GRAFANA_PASSWORD` | choose |

Production should also set:

```bash
CORS_ORIGINS=https://your-domain.com   # local defaults to "*"; lock this down
SMS_ENABLED=true                       # + SMS_EMAIL / SMS_PASSWORD
```

Push notifications are optional and off by default → [PUSH_DELIVERY.md](PUSH_DELIVERY.md).

## 2. Start

```bash
docker compose up -d --build
docker compose ps            # every service should be healthy
```

Flyway applies all migrations automatically on boot. First start takes ~1 min
(DB init + migrations).

## 3. Verify

```bash
curl -s localhost:8080/actuator/health     # {"status":"UP"}
docker compose logs -f app                 # "Started FoodDeliveryApplication"
```

- API: `http://<host>:8080`
- Swagger: `http://<host>:8080/swagger-ui.html` (disabled in the `prod` profile)
- Grafana: `http://<host>:3000` (admin / `GRAFANA_PASSWORD`)

## 4. Production-only steps

1. **Put it behind a reverse proxy with TLS** (nginx/Caddy/Traefik) — a single
   upstream, no round-robin. iOS and Android release builds reject plain
   `http://` and `ws://`, so `https://` + `wss://` are mandatory for the apps.
2. **Validate the foreign keys** once the data is clean →
   [`scripts/db/README.md`](../scripts/db/README.md).
3. **Set up alerting** (Telegram bot token) → [ALERTING.md](ALERTING.md).
4. **Verify backups**: they run automatically; you must copy them **off-host and
   encrypted**, and run the restore drill → [BACKUP_RESTORE.md](BACKUP_RESTORE.md).
5. **Lock down monitoring ports** — Prometheus/Alertmanager/exporters are bound
   to `127.0.0.1`; reach them over an SSH tunnel. Postgres/Redis/RabbitMQ host
   ports are published for debugging — close them on an internet-facing host.

## Updating

```bash
git pull
docker compose up -d --build app     # new migrations apply on boot
```

Shutdown is graceful (in-flight requests drain, 30s), and order creation is
idempotent, so a brief mid-deploy retry from a client is safe.

## Troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| `password authentication failed for user "postgres"` | `DB_PASSWORD` doesn't match the existing volume (it's only applied on first init). Either `docker compose exec postgres psql -U postgres -c "ALTER USER postgres PASSWORD '<pw>';"` or, in dev, `docker compose down && docker volume rm zbr_postgres-data`. |
| `Schema-validation: wrong column type` | Entity/migration mismatch — the app runs `ddl-auto: validate`. Fix with a migration; don't edit an applied one. |
| CORS errors from the browser | Add the exact origin to `CORS_ORIGINS` (scheme + host + port), or `*` for local. |
| Alertmanager won't start | Missing `docker/alertmanager/secrets/telegram_bot_token`. Affects alert delivery only, not the app. |
| App won't start, no obvious cause | `docker compose logs app` — the deepest `Caused by:` names the real failure. |

Onboarding the first vendor, courier and order → [GO_LIVE.md](GO_LIVE.md).

Day-2 operations (backups, alerts, incident response) →
[OPERATIONS.md](OPERATIONS.md).
