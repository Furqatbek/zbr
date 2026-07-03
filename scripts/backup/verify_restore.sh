#!/bin/sh
# Restore-drill: prove a backup is actually restorable by loading it into a
# throwaway database on the same server and running sanity checks. This does
# NOT touch the live database, so it is safe to run (and schedule) in prod.
#
# Exit 0 = the backup restored cleanly and all core tables are present.
# Exit non-zero = the backup is unusable — investigate immediately.
#
# Usage:
#   docker compose exec postgres-backup /scripts/verify_restore.sh           # newest backup
#   docker compose exec postgres-backup /scripts/verify_restore.sh /backups/<file>.dump
set -eu

: "${PGHOST:=postgres}"
: "${PGPORT:=5432}"
: "${PGUSER:=postgres}"
: "${PGDATABASE:=fooddelivery}"
: "${BACKUP_DIR:=/backups}"
CHECK_DB="${PGDATABASE}_restore_check"

dump="${1:-}"
if [ -z "$dump" ]; then
    dump="$(ls -1t "$BACKUP_DIR/$PGDATABASE-"*.dump 2>/dev/null | head -n1 || true)"
fi
if [ -z "$dump" ] || [ ! -f "$dump" ]; then
    echo "[verify] no backup found to test in $BACKUP_DIR" >&2
    exit 1
fi
echo "[verify] testing restore of $dump"

# Drop any leftover scratch DB from a previous run, then create a fresh one.
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d postgres -v ON_ERROR_STOP=1 \
    -c "DROP DATABASE IF EXISTS \"$CHECK_DB\";" \
    -c "CREATE DATABASE \"$CHECK_DB\";"

cleanup() {
    psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d postgres \
        -c "DROP DATABASE IF EXISTS \"$CHECK_DB\";" >/dev/null 2>&1 || true
}
trap cleanup EXIT

# Restore into the scratch DB (ownership notices are benign here).
pg_restore -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$CHECK_DB" \
    --no-owner --single-transaction "$dump"

# Sanity checks: core tables must exist and be queryable.
tables="users restaurants orders payments restaurant_commissions"
for t in $tables; do
    if n="$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$CHECK_DB" -tAc "SELECT count(*) FROM $t;")"; then
        echo "[verify] ok: $t rows=$n"
    else
        echo "[verify] FAIL: table '$t' not restorable" >&2
        exit 1
    fi
done

echo "[verify] PASS: $dump restores cleanly with all core tables present"
