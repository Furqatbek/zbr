# Go-Live Runbook — First Vendor, First Courier, First Order

Step-by-step for taking the platform live and getting order #1 through it. Every
call below was verified against the code. Deployment mechanics live in
[DEPLOYMENT.md](DEPLOYMENT.md); this is what to do **after** the stack is up.

Assumes `BASE=https://api.your-domain.com`. Set it once:

```bash
BASE=https://api.your-domain.com
```

> **MVP scope:** cash-only payments, one app instance, one city/service area.

---

## Step 0 — Prerequisites (blockers)

| # | Item | Why it blocks |
|---|------|---------------|
| 1 | **TLS: `https://` + `wss://`** via reverse proxy, single upstream | iOS ATS and Android cleartext policy **reject `http`/`ws` in release builds** — the apps cannot connect at all |
| 2 | `.env` complete, app healthy | `curl -s $BASE/actuator/health` → `{"status":"UP"}` |
| 3 | `CORS_ORIGINS` set to real domains (not `*`) | |

---

## Step 1 — Bootstrap a real admin

The `prod` profile runs `SeedAccountGuard`, which **suspends any seed account
still using the committed password** (`admin@fooddelivery.com`,
`platform@fooddelivery.com`, `owner@pizzapalace.com`, `john.doe@example.com`,
`courier@fooddelivery.com`). So you cannot log in with the seeded admin — by
design. The guard keys off the **password hash**, so setting a real password
both secures the account and un-blocks it.

Generate a bcrypt hash (any bcrypt tool, cost 12), then:

```sql
-- run against the prod DB
UPDATE users
   SET password_hash = '<your-bcrypt-hash>',
       status        = 'ACTIVE',
       email         = 'ops@your-domain.com'   -- optional but recommended
 WHERE email = 'admin@fooddelivery.com';
```

Restart the app (the guard runs at startup) and confirm login:

```bash
curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"emailOrPhone":"ops@your-domain.com","password":"<your-password>"}'
```

Save the token: `ADMIN=<accessToken>`.

**Then delete the other seed accounts** (they are demo data, not yours):

```sql
DELETE FROM users WHERE email IN
 ('platform@fooddelivery.com','owner@pizzapalace.com',
  'john.doe@example.com','courier@fooddelivery.com');
```

**Audit for unexpected admins** (a pre-fix build allowed public self-registration
as ADMIN — see the security note at the bottom):

```sql
SELECT u.id, u.email, u.status, u.created_at FROM users u
 WHERE u.role IN ('ADMIN','PLATFORM','SYSTEM')
    OR u.id IN (SELECT user_id FROM user_roles WHERE role IN ('ADMIN','PLATFORM','SYSTEM'));
```

---

## Step 2 — Set delivery-fee settings for your service area

**Do this before any order exists.** Wrong values here produce nonsense fees
(a `MAX_FEE` below `MIN_FEE` clamps every fee to the cap).

```bash
curl -s $BASE/api/v1/admin/delivery-fee-settings -H "Authorization: Bearer $ADMIN"
```

Keys: `BASE_FEE`, `PER_KM_FEE`, `MIN_FEE`, `MAX_FEE`, plus peak-hour and
`ROUTING_*` settings. Update one at a time:

```bash
curl -s -X PATCH "$BASE/api/v1/admin/delivery-fee-settings/BASE_FEE?value=5000" \
  -H "Authorization: Bearer $ADMIN"
```

Sanity rules: `MIN_FEE ≤ MAX_FEE`, both in **your currency's minor/major unit
consistently** (e.g. so'm, not a mix), and `MAX_FEE` high enough for your longest
realistic delivery.

Route-based distance (`ROUTING_ENABLED=1`) uses OSRM. If you enable it, point
`ROUTING_OSRM_BASE_URL` at a **self-hosted** OSRM — the default is a public demo
server (rate-limited, and it sees your customers' coordinates).

---

## Step 3 — Onboard vendor #1

**3a. Create the owner account** (self-registration allows `RESTAURANT_OWNER`):

```bash
curl -s -X POST $BASE/api/v1/auth/register -H 'Content-Type: application/json' -d '{
  "email":"owner@vendor1.uz","password":"<strong-password>",
  "phone":"+998901234567","firstName":"Ali","lastName":"Valiyev",
  "role":"RESTAURANT_OWNER"}'
```

Save `VENDOR=<accessToken>`.

**3b. Create the restaurant** (lat/lng must be the real location — the delivery
radius check depends on it):

```bash
curl -s -X POST $BASE/api/v1/restaurants -H "Authorization: Bearer $VENDOR" \
  -H 'Content-Type: application/json' -d '{
  "name":"Vendor One","phone":"+998901234567",
  "addressLine1":"Amir Temur 1","city":"Tashkent","country":"UZ",
  "latitude":41.311081,"longitude":69.240562,
  "acceptsDelivery":true,"minimumOrder":20000,
  "deliveryRadiusKm":10,"averagePrepTimeMinutes":20,
  "opensAt":"09:00","closesAt":"23:00"}'
```

Save the returned `id` as `RID`.

**3c. Add a menu category, then an item:**

```bash
curl -s -X POST $BASE/api/v1/restaurants/$RID/menu/categories \
  -H "Authorization: Bearer $VENDOR" -H 'Content-Type: application/json' \
  -d '{"name":"Main dishes","sortOrder":1}'          # -> CATEGORY_ID

curl -s -X POST $BASE/api/v1/restaurants/$RID/menu/items \
  -H "Authorization: Bearer $VENDOR" -H 'Content-Type: application/json' \
  -d '{"categoryId":<CATEGORY_ID>,"name":"Osh","price":35000,"prepTimeMinutes":15}'
```

Save the item id as `ITEM`.

**3d. Open the restaurant** — nothing is orderable while it is closed:

```bash
curl -s -X PATCH "$BASE/api/v1/restaurants/$RID/toggle-open?isOpen=true" \
  -H "Authorization: Bearer $VENDOR"
```

Verify `GET $BASE/api/v1/restaurants/$RID` shows `"isCurrentlyOpen": true`.

---

## Step 4 — Onboard courier #1

**4a. Account + courier profile:**

```bash
curl -s -X POST $BASE/api/v1/auth/register -H 'Content-Type: application/json' -d '{
  "email":"courier1@zbr.uz","password":"<strong-password>",
  "phone":"+998901112233","firstName":"Bek","lastName":"Karimov","role":"COURIER"}'
# -> COURIER=<accessToken>

curl -s -X POST $BASE/api/v1/couriers/register -H "Authorization: Bearer $COURIER" \
  -H 'Content-Type: application/json' \
  -d '{"vehicleType":"MOTORCYCLE","vehicleNumber":"01A123BC","preferredRadiusKm":10}'
# -> courier id = CID
```

**4b. ⚠️ Admin must verify the courier — this is a hard gate.**
Without it, going online fails with *"Courier must be verified before going
online"*, and no order can ever be delivered.

```bash
curl -s -X POST $BASE/api/v1/couriers/$CID/verify -H "Authorization: Bearer $ADMIN"
```

**4c. Go online** (from the app, or):

```bash
curl -s -X PATCH "$BASE/api/v1/couriers/me/status?status=AVAILABLE" \
  -H "Authorization: Bearer $COURIER"
```

> The courier is auto-set **OFFLINE** when their WebSocket drops. The app
> re-asserts status on reconnect; if testing via curl, re-send this call.

---

## Step 5 — First customer order

**5a. Register a customer** (or use the phone-OTP flow the app uses):

```bash
curl -s -X POST $BASE/api/v1/auth/register -H 'Content-Type: application/json' -d '{
  "email":"customer1@example.com","password":"<strong-password>",
  "phone":"+998907654321","firstName":"Dilnoza","lastName":"A","role":"CONSUMER"}'
# -> CUST=<accessToken>
```

**5b. Preview the delivery fee** (confirms Step 2 and the radius):

```bash
curl -s -X POST $BASE/api/v1/orders/calculate-delivery-fee \
  -H "Authorization: Bearer $CUST" -H 'Content-Type: application/json' \
  -d '{"restaurantId":'$RID',"deliveryLatitude":41.31,"deliveryLongitude":69.25}'
```

**5c. Place the order.** Send an `Idempotency-Key` — a retry with the same key
returns the same order instead of creating a duplicate:

```bash
curl -s -X POST $BASE/api/v1/orders -H "Authorization: Bearer $CUST" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: $(uuidgen)" -d '{
  "restaurantId":'$RID',"orderType":"DELIVERY",
  "items":[{"menuItemId":'$ITEM',"quantity":1}],
  "deliveryAddress":"Chilonzor 5","deliveryLatitude":41.31,"deliveryLongitude":69.25,
  "customerName":"Dilnoza","customerPhone":"+998907654321"}'
# -> order id = OID
```

Order must clear the restaurant's `minimumOrder` and sit inside
`deliveryRadiusKm`, or you get a `400` explaining which.

**5d. Pay (cash):**

```bash
curl -s -X POST $BASE/api/v1/orders/$OID/pay -H "Authorization: Bearer $CUST" \
  -H 'Content-Type: application/json' -d '{"paymentMethod":"CASH"}'
```

---

## Step 6 — Drive it end to end

**Vendor** (`PATCH /api/v1/orders/$OID/status`, `Authorization: Bearer $VENDOR`):

```bash
-d '{"status":"ACCEPTED","estimatedPrepTimeMinutes":20}'   # ETA comes from this
-d '{"status":"PREPARING"}'
-d '{"status":"READY"}'
```

**Courier** (`Authorization: Bearer $COURIER`):

```bash
curl -s -X POST $BASE/api/v1/couriers/me/orders/$OID/accept
curl -s -X PUT  $BASE/api/v1/couriers/me/orders/$OID/pickup    # only after READY
curl -s -X PUT  $BASE/api/v1/couriers/me/orders/$OID/transit
curl -s -X POST $BASE/api/v1/couriers/me/orders/$OID/complete  # -> DELIVERED, cash confirmed
```

Notes:
- A courier may **accept before the kitchen finishes**; pickup stays blocked
  until `READY`.
- `complete` confirms the cash payment server-side — no separate call.
- `DELIVERED` auto-transitions to `COMPLETED` after a grace period.

**Verify:**

```bash
curl -s $BASE/api/v1/orders/$OID -H "Authorization: Bearer $ADMIN"        # status
curl -s $BASE/api/v1/orders/$OID/track -H "Authorization: Bearer $CUST"   # courier + ETA
```

Check the commission row was written (this was historically missed):

```sql
SELECT * FROM restaurant_commissions WHERE order_id = <OID>;
```

---

## Step 7 — Right after order #1 works

| Do | Where |
|----|-------|
| Push credentials (APNs `.p8` + Firebase JSON) — without them couriers miss backgrounded orders | [PUSH_DELIVERY.md](PUSH_DELIVERY.md) |
| Telegram alert bot token — otherwise nothing pages you | [ALERTING.md](ALERTING.md) |
| Validate the FKs on live data | [`scripts/db/README.md`](../scripts/db/README.md) |
| Copy backups **off-host, encrypted**; run the restore drill | [BACKUP_RESTORE.md](BACKUP_RESTORE.md) |
| External uptime monitor (the on-box stack can't page you if the host dies) | [ALERTING.md](ALERTING.md) |

---

## Known limitations at MVP

- **Cash only** — no card acquiring; card UI is hidden in all apps.
- **One app instance** — WebSocket/rate-limit/image state is per-JVM
  ([DEPLOYMENT_CONSTRAINTS.md](DEPLOYMENT_CONSTRAINTS.md)).
- **A `SUSPENDED` courier can clear their own status** via `PATCH /me/status`.
  Harmless today (no admin suspension flow); a guard must land with that feature.
- Other unbuilt items (staff accounts, review replies, notification preferences)
  are listed in [MOBILE_INTEGRATION.md](MOBILE_INTEGRATION.md).

> **Security note:** builds before the `SELF_REGISTERABLE_ROLES` fix let anyone
> self-register as `ADMIN` via the public `/auth/register`. If any instance was
> ever internet-reachable on an older build, run the Step 1 audit query before
> going live.
