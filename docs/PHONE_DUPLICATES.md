# Duplicate accounts from unnormalised phone numbers

If the application log carries a line like

```
WARNING: phone normalisation SKIPPED: user 12 has "+998941143232" which
normalises to "998941143232", already held by another account.
```

this is the page it means.

## What happened

Two signup paths disagreed about how a phone number is stored.

| Path | Stored |
|---|---|
| OTP (`/auth/phone/*`) | `998941143232` — normalised |
| `/auth/register`, `PUT /users/me` | `+998941143232` — exactly as the client sent it |

The `UNIQUE` constraint on `users.phone` cannot see that those are one number,
and the `existsByPhone` duplicate checks compared raw input, so nothing stopped
a second account being created. One person ends up with two, and which one they
land in depends on which door they came through.

`PhoneNumbers.normalize` is now applied on every write path, so no new
duplicates can be created. `V41__normalize_user_phones.sql` canonicalised the
rows it safely could — but it deliberately left the collisions alone, because
choosing which of two live accounts survives is a business decision and a
migration that guessed would destroy real data.

## Find them

```sql
WITH canonical AS (
  SELECT id, phone,
         CASE
           WHEN regexp_replace(phone, '[^0-9]', '', 'g') = '' THEN phone
           WHEN length(regexp_replace(phone, '[^0-9]', '', 'g')) = 9
             THEN '998' || regexp_replace(phone, '[^0-9]', '', 'g')
           WHEN length(regexp_replace(phone, '[^0-9]', '', 'g')) = 10
                AND left(regexp_replace(phone, '[^0-9]', '', 'g'), 1) = '8'
             THEN '998' || substr(regexp_replace(phone, '[^0-9]', '', 'g'), 2)
           ELSE regexp_replace(phone, '[^0-9]', '', 'g')
         END AS c
  FROM users WHERE phone IS NOT NULL
)
SELECT c AS canonical_phone, count(*), array_agg(id ORDER BY id) AS user_ids
FROM canonical GROUP BY c HAVING count(*) > 1;
```

## Decide which account survives

Before touching anything, look at what each side actually holds:

```sql
SELECT u.id, u.phone, u.email, u.status, u.created_at, u.last_seen_at,
       (SELECT count(*) FROM orders o WHERE o.consumer_id = u.id)            AS orders,
       (SELECT count(*) FROM consumer_addresses a WHERE a.user_id = u.id)    AS addresses,
       (SELECT count(*) FROM couriers c WHERE c.user_id = u.id)              AS courier_profile,
       (SELECT count(*) FROM restaurants r WHERE r.owner_id = u.id)          AS restaurants
FROM users u WHERE u.id IN (10, 12);
```

Keep the account with the order history — that is the one the customer thinks
is theirs, and orders are the hardest thing to move. If both have orders, this
is a support conversation, not a SQL exercise.

## Resolve

**If the duplicate is empty** (no orders, addresses, courier profile or
restaurants) — the common case, someone who registered twice and never got
anywhere — retire it and free the number:

```sql
BEGIN;
  UPDATE users SET phone = NULL, status = 'INACTIVE' WHERE id = 12;
  UPDATE users SET phone = '998941143232'            WHERE id = 10;  -- already canonical
COMMIT;
```

Clearing `phone` rather than deleting the row keeps any audit and login history
intact, and `status = 'INACTIVE'` stops it being used: `JwtAuthenticationFilter`
re-checks account state on every request, so existing tokens for it stop working
immediately.

**If the duplicate holds data**, move it before retiring the account. Do it in
one transaction so a partial move cannot happen:

```sql
BEGIN;
  UPDATE orders             SET consumer_id = 10 WHERE consumer_id = 12;
  UPDATE consumer_addresses SET user_id     = 10 WHERE user_id     = 12;
  -- Check for a default-address clash before committing: a user must not end up
  -- with two.
  UPDATE consumer_addresses SET is_default = false
    WHERE user_id = 10 AND id <> (SELECT min(id) FROM consumer_addresses
                                  WHERE user_id = 10 AND is_default);
  UPDATE users SET phone = NULL, status = 'INACTIVE' WHERE id = 12;
COMMIT;
```

A courier profile cannot be moved this way — `couriers.user_id` is `UNIQUE`, so
if both accounts have one you must delete the loser's row first. Restaurants
should be moved with `PATCH /restaurants/{id}/owner` rather than SQL, so the
cache is evicted and the transfer is audited.

## Afterwards

Confirm the collision query returns nothing, then tell the customer which
account survived — if they had been using the one that is now inactive, their
app will sign them out and they will log in to the surviving account by OTP.
