#!/bin/sh
# Take a compressed pg_dump of the food-delivery database and prune old ones.
#
# Runs inside the postgres:15-alpine backup container (pg_dump client version
# matches the server). Writes an atomic, timestamped custom-format dump to
# BACKUP_DIR and drops dumps older than BACKUP_KEEP_DAYS.
set -eu

: "${PGHOST:=postgres}"
: "${PGPORT:=5432}"
: "${PGUSER:=postgres}"
: "${PGDATABASE:=fooddelivery}"
: "${BACKUP_DIR:=/backups}"
: "${BACKUP_KEEP_DAYS:=7}"
# PGPASSWORD must be supplied via the environment.

mkdir -p "$BACKUP_DIR"
stamp="$(date -u +%Y%m%d-%H%M%S)"
tmp="$BACKUP_DIR/.$PGDATABASE-$stamp.dump.part"
final="$BACKUP_DIR/$PGDATABASE-$stamp.dump"

echo "[backup] $(date -u +%FT%TZ) dumping $PGDATABASE from $PGHOST:$PGPORT"
# -Fc = custom compressed format; restore with pg_restore. Write to a .part file
# first and rename on success so a partial dump is never mistaken for a good one.
if pg_dump -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -Fc -f "$tmp"; then
    mv "$tmp" "$final"
    echo "[backup] wrote $final ($(du -h "$final" | cut -f1))"
    date -u +%FT%TZ > "$BACKUP_DIR/last_success"
else
    rc=$?
    rm -f "$tmp"
    echo "[backup] FAILED (pg_dump exit $rc)" >&2
    exit "$rc"
fi

# Retention: remove dumps older than BACKUP_KEEP_DAYS.
find "$BACKUP_DIR" -maxdepth 1 -name "$PGDATABASE-*.dump" -type f -mtime "+$BACKUP_KEEP_DAYS" -print -delete
echo "[backup] retention: kept last $BACKUP_KEEP_DAYS day(s)"
