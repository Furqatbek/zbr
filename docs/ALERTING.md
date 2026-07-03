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

Alertmanager delivers to a **Telegram bot**. The bot token is read from a mounted
secret file that is **gitignored** (the repo's global `secrets/` rule), so it
never lands in version control; the chat id is set in `alertmanager.yml`.

```bash
# 1. Create a bot: message @BotFather on Telegram, send /newbot, follow the
#    prompts, and copy the token it gives you (looks like 123456789:AA...).

# 2. Drop the token into the secret file (one line, the token only):
cp docker/alertmanager/secrets/telegram_bot_token.example docker/alertmanager/secrets/telegram_bot_token
$EDITOR docker/alertmanager/secrets/telegram_bot_token   # paste your bot token

# 3. Get the chat id to send alerts to:
#    - For a group: add the bot to the group, then open
#        https://api.telegram.org/bot<YOUR_TOKEN>/getUpdates
#      and read "chat":{"id":-100...}. Group ids are negative.
#    - For a DM: message the bot first, then read the positive "id" from getUpdates.
#    Put that number in chat_id: in docker/alertmanager/alertmanager.yml
$EDITOR docker/alertmanager/alertmanager.yml   # set chat_id

# 4. Start / restart the monitoring stack:
docker compose up -d prometheus alertmanager
```

The chat id is not a credential, so it's fine to commit it in `alertmanager.yml`.
Keep the bot **token** only in the gitignored secret file.

> Until you create `telegram_bot_token`, the Alertmanager container will not
> start — that only affects alert delivery, not the application.

## Verify it works

```bash
# Alertmanager is healthy and has loaded config:
curl -s localhost:9093/-/healthy        # -> "OK"

# Prometheus sees Alertmanager:
#   http://localhost:9090/status  -> "Alertmanager" section lists alertmanager:9093

# Fire a test alert straight into Alertmanager (proves the Telegram path end to end):
curl -s -XPOST localhost:9093/api/v2/alerts -H 'Content-Type: application/json' -d '[
  {"labels":{"alertname":"TestPage","severity":"critical"},
   "annotations":{"summary":"Test alert — ignore","description":"If you see this in Telegram, alerting works."}}
]'
# A message should land in your Telegram chat within a few seconds.
```

If nothing arrives, check `docker compose logs alertmanager` — a wrong token or
chat id shows up there as a Telegram API error.

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

## ⚠️ Lock down the monitoring ports

`docker-compose.yml` publishes Prometheus (9090), Alertmanager (9093), Grafana
(3000), and the exporters (9187/9121) on the host. These have **no
authentication** in front of them (Grafana aside), and Alertmanager's UI lets
anyone create silences — i.e. quietly disable your alerts. Do **not** expose
these ports to the public internet:

- Bind them to localhost or a private interface (e.g. `127.0.0.1:9090:9090`), or
- Drop the `ports:` mappings entirely and reach them over the compose network /
  an SSH tunnel / an authenticated reverse proxy.

Only the app's own port needs to be publicly reachable.

## Other channels

Prefer a different channel? Replace the `telegram_configs` block in the
`team-notifications` receiver. Examples:

**Slack** (Incoming Webhook URL in the secret file):
```yaml
    slack_configs:
      - api_url_file: /etc/alertmanager/secrets/slack_api_url
        channel: '#alerts'
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
