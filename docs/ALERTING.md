# Alerting

Prometheus scrapes the app, Postgres, and Redis and evaluates the rules in
`docker/prometheus/alerts.yml`. Those alerts are routed through **Alertmanager**
(`docker/alertmanager/alertmanager.yml`) and delivered to a real channel so a
human actually gets paged. Without Alertmanager configured, alerts fire inside
Prometheus and go nowhere.

## What you get paged for

| Alert | Severity | Fires when |
|-------|----------|-----------|
| `ApplicationDown` | critical | app `/actuator/prometheus` unreachable for 1m |
| `DatabaseConnectionPoolExhausted` | critical | threads blocked waiting for a DB connection (`hikaricp_connections_pending > 0`) for 2m |
| `PostgreSQLDown` / `RedisDown` | critical | exporter reports the datastore down for 1m |
| `HighErrorRate` | warning | >5% of HTTP requests return 5xx for 5m |
| `ApplicationErrorLogs` | warning | >5 ERROR log events in 5m (`logback_events_total`) |
| `DatabaseConnectionPoolNearLimit` | warning | >15 of 20 Hikari connections active for 5m |
| `HighHeapUsage`, `HighResponseTime`, cache/deadlock/etc. | warning | see `alerts.yml` |

Critical alerts notify within ~10s and re-nag hourly; warnings batch (30s) and
re-send at most every 4h. A firing critical inhibits the matching warning so you
aren't paged twice for the same root cause.

## Setup (one time)

Alertmanager delivers to **Slack** by default. The webhook URL is read from a
mounted secret file that is **gitignored** (the repo's global `secrets/` rule),
so it never lands in version control.

```bash
# 1. Create an Incoming Webhook in Slack (Slack → Apps → Incoming Webhooks),
#    pick the channel, copy the https://hooks.slack.com/services/... URL.

# 2. Drop it into the secret file (one line, the URL only):
cp docker/alertmanager/secrets/slack_api_url.example docker/alertmanager/secrets/slack_api_url
$EDITOR docker/alertmanager/secrets/slack_api_url   # paste your webhook

# 3. Start / restart the monitoring stack:
docker compose up -d prometheus alertmanager
```

Adjust the target `channel:` in `docker/alertmanager/alertmanager.yml` if you
don't use `#alerts`.

> Until you create `slack_api_url`, the Alertmanager container will not start —
> that only affects alert delivery, not the application.

## Verify it works

```bash
# Alertmanager is healthy and has loaded config:
curl -s localhost:9093/-/healthy        # -> "OK"

# Prometheus sees Alertmanager:
#   http://localhost:9090/status  -> "Alertmanager" section lists alertmanager:9093

# Fire a test alert straight into Alertmanager (proves the Slack path end to end):
curl -s -XPOST localhost:9093/api/v2/alerts -H 'Content-Type: application/json' -d '[
  {"labels":{"alertname":"TestPage","severity":"critical"},
   "annotations":{"summary":"Test alert — ignore","description":"If you see this in Slack, alerting works."}}
]'
# A message should land in your Slack channel within a few seconds.
```

You can also browse firing alerts at `http://localhost:9090/alerts` (Prometheus)
and `http://localhost:9093` (Alertmanager UI).

## ⚠️ Important limitation: the "whole box is gone" case

Prometheus and Alertmanager run **on the same host as the app** (single-instance
deployment — see `DEPLOYMENT_CONSTRAINTS.md`). If that host dies, the monitoring
stack dies with it, so `ApplicationDown` can never fire — there's nothing left to
send it.

For true uptime you need an **external** check that lives off the box: point a
free uptime monitor (UptimeRobot, Better Uptime, a cron on another machine, etc.)
at `https://<your-host>/actuator/health` and have *it* alert you when the host is
unreachable. The internal alerts above cover everything *except* total host loss.

## Other channels

Alertmanager can deliver to email, Telegram, PagerDuty, etc. Replace the
`slack_configs` block in the `team-notifications` receiver. Examples:

**Telegram** (common for teams here; create a bot via @BotFather, get the chat id):
```yaml
    telegram_configs:
      - bot_token_file: /etc/alertmanager/secrets/telegram_bot_token
        chat_id: -1001234567890
        send_resolved: true
```

**Email** (SMTP):
```yaml
    email_configs:
      - to: 'oncall@example.com'
        from: 'alerts@example.com'
        smarthost: 'smtp.example.com:587'
        auth_username: 'alerts@example.com'
        auth_password_file: /etc/alertmanager/secrets/smtp_password
        send_resolved: true
```
Keep any token/password in a file under `docker/alertmanager/secrets/` (already
gitignored) and reference it with the `_file` option.

## Note on backup alerts

Backup staleness is covered at the **container** level, not here: the
`postgres-backup` service's healthcheck goes unhealthy if no backup lands within
the interval + grace (see `docs/BACKUP_RESTORE.md`). The backup job doesn't
export Prometheus metrics, so there's no `alerts.yml` rule for it — watch
`docker compose ps` / container health instead.
