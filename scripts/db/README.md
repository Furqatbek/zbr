# Database maintenance scripts

## Promoting the V32 foreign keys to fully validated

Migration `V32__data_integrity_constraints.sql` added the core foreign keys as
`NOT VALID`: they are enforced for every new and modified row, but Postgres did
**not** scan the existing rows (so the migration couldn't fail on legacy drift).
Once the live data is clean, promote them to fully validated.

**1. Check for orphaned rows (read-only):**

```bash
psql "$DATABASE_URL" -f scripts/db/check_fk_orphans.sql
```

Every constraint should report `orphan_rows = 0`. If any is non-zero, that
constraint has child rows referencing a missing parent — fix them first (delete
the orphans, or repoint them at a valid parent) depending on what the data means.

**2. Validate (safe on a live DB):**

```bash
psql "$DATABASE_URL" -f scripts/db/validate_fks.sql
```

Each FK is validated independently; any that still has violations is skipped with
a `WARNING` and the rest proceed. `VALIDATE CONSTRAINT` takes only a
`SHARE UPDATE EXCLUSIVE` lock, so reads and writes continue during the scan.
Both scripts are idempotent — re-run them any time.

Running from a container instead of the host:

```bash
docker compose exec -T postgres \
  psql -U postgres -d fooddelivery -f - < scripts/db/check_fk_orphans.sql
```
