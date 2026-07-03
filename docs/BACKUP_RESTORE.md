# Database Backups & Restore

The platform runs on a single Postgres instance (see `DEPLOYMENT_CONSTRAINTS.md`),
so the database is a single point of failure. Automated backups plus a tested
restore path are mandatory before taking real orders.

## What runs automatically

`docker-compose.yml` includes a `postgres-backup` service (same `postgres:15-alpine`
image as the server, so `pg_dump` matches the server version). It loops:

1. Runs `scripts/backup/pg_backup.sh` — a compressed, custom-format `pg_dump`
   written atomically (`.part` → rename) to the `postgres-backups` volume as
   `fooddelivery-YYYYmmdd-HHMMSS.dump`, and updates a `last_success` marker.
2. Sleeps `BACKUP_INTERVAL_SECONDS` (default 86400 = daily).
3. Prunes dumps older than `BACKUP_KEEP_DAYS` (default 7).

The service **healthcheck fails** if no successful backup has landed within the
interval + a 2h grace window, so `docker compose ps` (and any monitoring on
container health) will show the backup job as unhealthy if it silently stops.

### Configuration (`.env`)

| Var | Default | Meaning |
|-----|---------|---------|
| `BACKUP_KEEP_DAYS` | `7` | Days of dumps to retain |
| `BACKUP_INTERVAL_SECONDS` | `86400` | Seconds between backups |
| `DB_PASSWORD` | — | Reused as `PGPASSWORD` for the backup job |

## ⚠️ Copy backups off the box

The dumps live in a Docker volume **on the same host as the database**. If the
host dies, the backups die with it — which defeats the purpose. For anything
beyond a throwaway demo, copy them off-host on a schedule, e.g.:

```bash
# Pull the newest dump to a local/remote path (run from the host)
docker run --rm -v zbr_postgres-backups:/backups -v "$PWD/offsite":/out \
  postgres:15-alpine sh -c 'cp "$(ls -1t /backups/fooddelivery-*.dump | head -1)" /out/'
# then rsync/aws s3 cp the offsite/ dir somewhere durable
```

## Restore drill (do this regularly — a backup you haven't restored isn't a backup)

`scripts/backup/verify_restore.sh` proves the latest dump is restorable **without
touching the live database**: it restores into a throwaway `fooddelivery_restore_check`
database, checks the core tables (`users`, `restaurants`, `orders`, `payments`,
`restaurant_commissions`) are present and queryable, then drops the scratch DB.

```bash
docker compose exec postgres-backup /scripts/verify_restore.sh
# exit 0 + "PASS" = the backup is good. Non-zero = investigate NOW.
```

Run it on a schedule (cron on the host, or a CI job) so a broken backup is
caught before you actually need it.

## Restoring for real (disaster recovery)

**This overwrites the live database.** Put the app in maintenance / stop it first.

```bash
# 1. List available backups
docker compose exec postgres-backup sh -c 'ls -1t /backups/fooddelivery-*.dump'

# 2. (Recommended) Stop the app so nothing writes mid-restore
docker compose stop app

# 3. Restore a chosen dump into the live DB
docker compose exec postgres-backup /scripts/pg_restore.sh /backups/fooddelivery-YYYYmmdd-HHMMSS.dump

# 4. Bring the app back
docker compose start app
```

`pg_restore.sh` uses `--clean --if-exists --no-owner --single-transaction`, so it
drops and recreates objects and either fully succeeds or rolls back — no
half-restored state.

## Manual one-off backup

```bash
docker compose exec postgres-backup /scripts/pg_backup.sh
```

## Scripts

| Script | Purpose | Destructive? |
|--------|---------|--------------|
| `scripts/backup/pg_backup.sh` | Take a compressed dump + prune old ones | no |
| `scripts/backup/verify_restore.sh` | Restore drill into a scratch DB + sanity check | no (uses a throwaway DB) |
| `scripts/backup/pg_restore.sh` | Restore a dump into the **live** DB | **yes** |

All three honor `PGHOST`/`PGPORT`/`PGUSER`/`PGDATABASE`/`PGPASSWORD`/`BACKUP_DIR`
from the environment, so they also run outside Docker against any reachable
Postgres.
