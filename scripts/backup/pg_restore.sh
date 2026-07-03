#!/bin/sh
# Restore a custom-format pg_dump into the live database. DESTRUCTIVE.
#
# Usage (from the backup container):
#   docker compose exec postgres-backup /scripts/pg_restore.sh /backups/<file>.dump
set -eu

: "${PGHOST:=postgres}"
: "${PGPORT:=5432}"
: "${PGUSER:=postgres}"
: "${PGDATABASE:=fooddelivery}"
: "${BACKUP_DIR:=/backups}"

dump="${1:-}"
if [ -z "$dump" ] || [ ! -f "$dump" ]; then
    echo "usage: pg_restore.sh <path-to.dump>" >&2
    echo "available backups:" >&2
    ls -1t "$BACKUP_DIR"/*.dump 2>/dev/null >&2 || echo "  (none)" >&2
    exit 2
fi

echo "[restore] restoring $dump into $PGDATABASE on $PGHOST:$PGPORT"
echo "[restore] WARNING: this OVERWRITES existing data in $PGDATABASE"

# --clean --if-exists drops existing objects before recreating them; --no-owner
# avoids role-mismatch errors; --single-transaction makes the restore all-or-nothing.
pg_restore -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
    --clean --if-exists --no-owner --single-transaction "$dump"

echo "[restore] done"
