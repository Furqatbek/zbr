# Go-Live Runbook — First Vendor, First Courier, First Order

Step-by-step for taking the platform live and getting order #1 through it. Every
call below was verified against the code. Deployment mechanics live in
[DEPLOYMENT.md](DEPLOYMENT.md); this is what to do **after** the stack is up.

Assumes `BASE=https://zbrr.uz`. Set it once:

```bash
BASE=https://zbrr.uz
```

> **MVP scope:** cash-only payments, one app instance, one city/service area.

---

## Step 0 — Prerequisites (blockers)

| # | Item | Why it blocks |
|---|------|---------------|
| 1 | **DNS**: `zbrr.uz` and `www.zbrr.uz` A records point at the server | Certificate issuance fails without it |
| 2 | **TLS: `https://` + `wss://`** — `./scripts/tls/init-letsencrypt.sh you@zbrr.uz` ([DEPLOYMENT.md §3b](DEPLOYMENT.md#3b-tls-with-nginx-production)) | iOS ATS and Android cleartext policy **reject `http`/`ws` in release builds** — the apps cannot connect at all |
| 3 | Port **80 open and left open** | Renewal runs through it every 12h; closing it after issuance breaks renewal silently |
| 4 | `.env` complete, app healthy | `curl -s $BASE/actuator/health` → `{"status":"UP"}` |
| 5 | `IMAGE_BASE_URL=https://zbrr.uz/api/v1/images` | Otherwise every logo and menu photo URL points at localhost |
| 6 | `CORS_ORIGINS` set to real domains (not `*`) | Browser clients only — the Expo apps ignore CORS entirely |
| 7 | `FIREBASE_ENABLED=true` **and** `APNS_ENABLED=true` with real credentials | These ship **off**. The stack comes up healthy either way, but vendors get no new-order alert and couriers get no offers → [PUSH_DELIVERY.md](PUSH_DELIVERY.md) |
| 8 | **Delivery fees set in the admin panel** (`PUT /api/v1/admin/delivery-fee-settings`) | Until saved once, the code falls back to `base 2.00 / per-km 0.50 / max 15.00` — placeholder figures that are nonsense as som. Order #1 would be charged 2 so'm for delivery. |

The apps call `https://zbrr.uz/api/v1/...` and connect to `wss://zbrr.uz/ws`.

Money is **UZS** throughout (`APP_CURRENCY`), stored as plain decimals — `15000`
means 15 000 so'm, not minor units.

---

## Step 1 — Bootstrap a real admin

`SeedAccountGuard` runs on every profile except `dev` and `test`, and **suspends
any seed account still using the committed password** (`admin@fooddelivery.com`,
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
       email         = 'ops@zbrr.uz'   -- optional but recommended
 WHERE email = 'admin@fooddelivery.com';
```

Restart the app (the guard runs at startup) and confirm login:

```bash
curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"emailOrPhone":"ops@zbrr.uz","password":"<your-password>"}'
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

## Step 7 — Turn on push notifications

Until this is done, couriers only see new orders while the app is **open** — a
backgrounded phone misses them. Android and iOS are independent; you can enable
one before the other.

Both credentials can be supplied as **base64 in `.env`**, so neither key has to
sit on the VPS filesystem. (Mounting files into `./secrets/` also works — see
[PUSH_DELIVERY.md](PUSH_DELIVERY.md).)

**7a. Android — Firebase service account**

Firebase Console → Project Settings → **Service accounts** → *Generate new
private key*. Must be the **same Firebase project as the app**
(`push-notifications-for-zbr`).

> This is **not** `google-services.json`. That one is client-side, ships inside
> the app, and is not a secret. The service-account key **is** a secret — it can
> push to every user. Never commit it.

```bash
base64 -w0 firebase-service-account.json                                  # Linux/macOS
# PowerShell:
# [Convert]::ToBase64String([IO.File]::ReadAllBytes("firebase-service-account.json"))
```

```bash
# .env
FIREBASE_ENABLED=true
FIREBASE_CREDENTIALS_BASE64=<paste the base64>
```

⚠️ **`FIREBASE_ENABLED=true` with a missing/invalid credential stops the whole
app from starting** (Firebase initializes at boot). Set the flag and the
credential in the same deploy, and watch the logs on restart.

**7b. iOS — APNs auth key**

Apple Developer portal → Keys → create an **APNs** key. The `.p8` downloads
**once only**; record the 10-character Key ID.

```bash
base64 -w0 AuthKey_ABC123XYZ9.p8                                          # Linux/macOS
# PowerShell:
# [Convert]::ToBase64String([IO.File]::ReadAllBytes("AuthKey_ABC123XYZ9.p8"))
```

```bash
# .env
APNS_ENABLED=true
APNS_KEY_BASE64=<paste the base64>
APNS_KEY_ID=ABC123XYZ9
APNS_TEAM_ID=VQ56W9S7S9
APNS_TOPIC=com.zbr.owner
APNS_PRODUCTION=false      # false = Xcode/dev builds; true = TestFlight/App Store
```

⚠️ **`APNS_PRODUCTION` must match the build the tester installed.** Device tokens
are environment-specific: a dev-build token sent to the production host returns
`400 BadDeviceToken` and the backend prunes it (so it silently stops working).

**7c. Apply and verify**

```bash
./scripts/deploy.sh
docker compose logs -f app | grep -iE "firebase|apns"    # init errors show here
```

Then, on a **physical device** (simulators/emulators cannot receive push), log
into the app so it registers its token, and confirm:

```sql
SELECT user_id, device_type, is_active, left(device_token, 24) AS token
  FROM user_device_tokens WHERE is_active = true;
```

Place a test order and confirm the courier phone buzzes **with the app in the
background**. If a token disappears (`is_active=false`) right after a send, the
provider rejected it — for iOS that's almost always the sandbox/production
mismatch above.

---

## Step 8 — Finish hardening

| Do | Where |
|----|-------|
| Telegram alert bot token — otherwise nothing pages you | [ALERTING.md](ALERTING.md) |
| Validate the FKs on live data | [`scripts/db/README.md`](../scripts/db/README.md) |
| Copy backups **off-host, encrypted**; run the restore drill | [BACKUP_RESTORE.md](BACKUP_RESTORE.md) |
| External uptime monitor (the on-box stack can't page you if the host dies) | [ALERTING.md](ALERTING.md) |

---

## Step 9 — Before submitting the apps to the stores

App reviewers sit outside Uzbekistan and **cannot receive your OTP SMS**, so
without a test number they cannot get past the login screen and the app is
rejected. The backend supports a whitelisted number that accepts a fixed code
and sends no SMS — and it works in the **production** build.

**9a. Enable the review number** (production `.env`, then restart the app):

```bash
OTP_REVIEW_NUMBERS=+998900000000
OTP_REVIEW_CODE=123456
```

Both must be set or the feature stays off. Matching is exact (formatting is
normalised, so `+998 90 000 00 00` is the same number); it is never a prefix or
wildcard. Multiple numbers can be comma-separated — useful for giving each app's
reviewer a separate account.

**9b. Create the account behind that number.** The whitelist only gets the
reviewer past OTP; they still need a usable account, or they log into an empty
or blocked app:

| App | What the review account needs |
|-----|-------------------------------|
| Customer | a `CONSUMER` user, and at least one **open** restaurant with menu items in range of the address they pick |
| Vendor | a `RESTAURANT_OWNER` user **with a restaurant** (Step 3) — otherwise the app has nothing to show |
| Courier | a `COURIER` user that an admin has **verified** (Step 4b) — an unverified courier can never go online |

**9c. Verify it end to end before submitting:**

```bash
curl -s -X POST $BASE/api/v1/auth/phone/request-otp \
  -H 'Content-Type: application/json' -d '{"phone":"+998900000000"}'
# -> success, and NO SMS is sent

curl -s -X POST $BASE/api/v1/auth/phone/verify-otp \
  -H 'Content-Type: application/json' \
  -d '{"phone":"+998900000000","code":"123456"}'
# -> tokens
```

The app logs a `REVIEW NUMBER used …` WARNING on every such login, so you can
confirm it is active (and later confirm it is gone).

**9d. ⚠️ Clear it once the app is approved:**

```bash
# remove both lines from .env, then
./scripts/deploy.sh
```

This is a deliberate auth bypass for one number — it should not outlive the
review. It is config-only, so removing the lines and restarting is enough; no
code change or redeploy needed. Confirm the `REVIEW NUMBER used` warnings stop.

> Give the store reviewer the number **and** the code in the App Review notes,
> along with a test account for the role that app serves.

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
