# Operations Runbook

Start here to deploy and run this backend. This is an index — each section links
to the detailed doc. Read the **hard constraint** first; it shapes everything.

> ⚠️ **Run exactly ONE app instance. Do not scale horizontally.** Critical state
> lives in JVM memory and on local disk. → [DEPLOYMENT_CONSTRAINTS.md](DEPLOYMENT_CONSTRAINTS.md)

## Map

| Concern | Doc |
|---------|-----|
| **Deploying (start here)** | [DEPLOYMENT.md](DEPLOYMENT.md) |
| **Going live: first vendor/courier/order** | [GO_LIVE.md](GO_LIVE.md) |
| Single-instance mandate & what breaks with 2+ nodes | [DEPLOYMENT_CONSTRAINTS.md](DEPLOYMENT_CONSTRAINTS.md) |
| Push notifications (FCM / APNs) | [PUSH_DELIVERY.md](PUSH_DELIVERY.md) |
| Mobile app integration contract | [MOBILE_INTEGRATION.md](MOBILE_INTEGRATION.md) |
| Backups, restore, and the restore drill | [BACKUP_RESTORE.md](BACKUP_RESTORE.md) |
| Alerting (Telegram) and monitoring access | [ALERTING.md](ALERTING.md) |
| Promoting the V32 foreign keys to validated | [../scripts/db/README.md](../scripts/db/README.md) |
| Required secrets / env vars | [../.env.example](../.env.example) |

---

## First deploy

1. **Secrets.** Copy `.env.example` → `.env` and fill every required value
   (`JWT_SECRET`, `DB_PASSWORD`, `RABBITMQ_USER/PASSWORD`, `GRAFANA_PASSWORD`).
   Compose refuses to start if any are missing. Generate the JWT secret with
   `openssl rand -base64 48`. → [.env.example](../.env.example)
2. **Alerting secret.** Create the Telegram bot token file and set the chat id,
   or Alertmanager won't start (delivery only — the app is unaffected).
   → [ALERTING.md § Setup](ALERTING.md)
3. **Bring it up.** In production, do this through the TLS bootstrap — it issues
   the certificate, starts the stack, and verifies renewal in one go:
   ```bash
   ./scripts/tls/init-letsencrypt.sh you@zbrr.uz    # → docker compose up -d
   docker compose ps          # every service should be healthy
   ```
   → [DEPLOYMENT.md § 3b](DEPLOYMENT.md#3b-tls-with-nginx-production)
4. **Smoke-check:** `curl -s localhost:8080/actuator/health` → `{"status":"UP"}`.
5. **Prove alerting** end to end with the test-alert curl. → [ALERTING.md § Verify](ALERTING.md)
6. **After the DB has real data and is clean,** promote the foreign keys:
   run the orphan check, then validate. → [scripts/db/README.md](../scripts/db/README.md)

## Ongoing operations

- **Backups** run automatically (daily by default) via the `postgres-backup`
  service. Your jobs: **copy them off-host** and **encrypt** that copy, and run
  the **restore drill** on a schedule — a backup you haven't restored isn't a
  backup. → [BACKUP_RESTORE.md](BACKUP_RESTORE.md)
- **Monitoring** (Prometheus 9090, Alertmanager 9093, exporters) is bound to
  `127.0.0.1`; reach it via SSH tunnel. Grafana (3000) has a password and is
  published. → [ALERTING.md § Monitoring ports](ALERTING.md)
- **Dashboards** (Grafana, provisioned automatically): **Business Overview**
  (home — live orders/revenue/couriers straight from the DB), Application
  Overview (latency, errors, JVM, DB pool), PostgreSQL, Redis, and RabbitMQ
  (queue backlog + redelivery storms).
- **What pages you:** app down, DB/Redis down, DB connection pool exhausted
  (critical); high 5xx rate, error-log spikes, pool near limit, high heap/latency
  (warning). → [ALERTING.md § What you get paged for](ALERTING.md)
- **Add an external uptime monitor** off the box (UptimeRobot/etc.) on
  `/actuator/health` — the internal stack can't page you if the whole host dies.
  Most of them also watch **certificate expiry**; turn that on. Nothing in this
  stack alerts on a stalled renewal, and the failure is silent for ~60 days.
- **TLS certificates** renew themselves (certbot every 12h, nginx reloads every
  6h). The one thing that breaks it is **closing port 80** — renewal runs
  through it. Spot-check quarterly:
  ```bash
  docker compose exec certbot certbot certificates    # expiry dates
  docker compose exec certbot certbot renew --webroot -w /var/www/certbot --dry-run
  ```

## Incident response

| Situation | Action |
|-----------|--------|
| Data loss / corruption | Stop the app, restore the latest good dump, restart. → [BACKUP_RESTORE.md § Restoring for real](BACKUP_RESTORE.md) |
| "Is the latest backup usable?" | Run the restore drill (non-destructive). → [BACKUP_RESTORE.md § Restore drill](BACKUP_RESTORE.md) |
| Pager: DB pool exhausted | Threads are blocked on connections — check for slow queries / a stuck transaction; Hikari max is 20. |
| Alerts went silent | Check `docker compose logs alertmanager` (bad token/chat id) and that Prometheus lists Alertmanager under `/status`. |
| Apps report a TLS error / expired certificate | `docker compose logs certbot` and run the `--dry-run` above. Almost always port 80 got firewalled. After fixing: `docker compose exec certbot certbot renew --webroot -w /var/www/certbot --force-renewal && docker compose exec nginx nginx -s reload`. |
| App crashloops with `28P01 password authentication failed` | `DB_PASSWORD` in `.env` doesn't match what the existing `postgres-data` volume was initialized with (`POSTGRES_PASSWORD` only applies on first init). Either `ALTER USER postgres PASSWORD '...'` inside the container, or (dev) remove the volume and reinit. |
| Tempted to add a second app node | Don't — see the constraints doc. Fix the three blockers first. → [DEPLOYMENT_CONSTRAINTS.md](DEPLOYMENT_CONSTRAINTS.md) |

## Known limits

- **Payments:** no online card gateway wired — the platform is **cash-only**
  until an acquirer is integrated.
- **Single instance only** — stateful app tier; scaling needs the three changes
  in [DEPLOYMENT_CONSTRAINTS.md](DEPLOYMENT_CONSTRAINTS.md).
