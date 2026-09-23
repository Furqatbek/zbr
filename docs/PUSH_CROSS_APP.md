# Push landing on the wrong app

**Symptom:** one phone, one account, two of our apps. Courier job alerts arrive
on the customer app; order updates arrive on the courier app.

There are two faults behind it. One is a bug we have fixed; the other is
configuration that has never been set. **They have to be dealt with in that
order** — the configuration alone would make things worse.

---

## 1. Two apps on one phone were sharing one token row

`V36` made registration upsert on `(user_id, device_id)`:

```sql
CREATE UNIQUE INDEX uq_user_device_tokens_user_device
    ON user_device_tokens (user_id, device_id) WHERE device_id IS NOT NULL;
```

Right for one app, wrong for three — **our apps report the same `deviceId` on
one phone**. Android's `ANDROID_ID` is scoped to the app signing key and iOS's
`identifierForVendor` to the vendor, so apps published by one developer share
both values. The second app to register therefore found the first app's row and
overwrote `device_token` and `app_id`.

One row survived where there were two apps. Every push for that person went to
whichever app had registered last — which is precisely the reported symptom, in
both directions, depending on which app was opened most recently.

`V37` added `app_id` a release later, for APNs topics, and did not widen the
key. That is how the two ended up disagreeing about what a row identifies.

**Fixed in `V53`**: one row per app per device.

```sql
CREATE UNIQUE INDEX uq_user_device_tokens_user_device_app
    ON user_device_tokens (user_id, device_id, COALESCE(app_id, ''))
    WHERE device_id IS NOT NULL;
```

`COALESCE` rather than a bare `app_id`, because Postgres treats NULLs as
distinct in a unique index — legacy rows with no `app_id` would otherwise be
free to duplicate, reintroducing the N-copies-of-every-push problem `V36`
existed to solve.

Registration matches on `(user, deviceId, appId)`, and an app that now sends an
`appId` **adopts its own legacy row** rather than leaving it behind. An
abandoned row would keep receiving every push for that user, because targeting
fails open for a device whose app is unknown.

Nothing can be backfilled: the overwritten rows lost the other app's token at
the moment of collision. Each app re-registers on next launch.

## 2. Per-app targeting has never been switched on

`PushTargeting` narrows a user's devices to the app the notification is for. It
is deliberately inert until configured, and **nothing is configured**:

```yaml
app:
  push:
    app-ids:
      consumer:   ${PUSH_APP_ID_CONSUMER:}      # empty
      courier:    ${PUSH_APP_ID_COURIER:}       # empty
      restaurant: ${PUSH_APP_ID_RESTAURANT:}    # empty
```

An empty value means "no filtering for this role", so every push goes to every
device the user has. Set them on the server:

```bash
PUSH_APP_ID_CONSUMER=app.zbr.customer
PUSH_APP_ID_COURIER=app.zbr.courier
PUSH_APP_ID_RESTAURANT=com.zbr.owner
```

(Confirm each against the shipped builds before setting it. A wrong value
filters an app's pushes away entirely, and the note below explains why that is
hard to notice.)

**Do not set these before `V53` is deployed.** With one row per person holding
one app's id, filtering by app would not fix the mixing — it would drop the
other app's pushes completely. Mixed notifications are annoying; missing ones
are a courier who never hears about a job, and nothing in any log says so.

## 3. The apps must send `appId`

Filtering **fails open**: a token whose `app_id` is null receives everything.
That is deliberate — the column arrived in `V37` and filtering out every
pre-existing row would have silently stopped push for every registered device —
but it means the fix does nothing for a device that never identifies its app.

```http
POST /api/v1/notifications/device-token
{ "token": "...", "platform": "ANDROID", "deviceId": "<stable per-device id>",
  "appId": "app.zbr.customer", "appVersion": "1.0.3" }
```

Aliases accepted: `bundleId`, `packageName`. **iOS has always required it** —
one APNs key serves all three apps, and a push whose `apns-topic` is not the
app's own bundle id is rejected. Android has not, which is where the null rows
come from.

## Checking it

How many tokens can be targeted at all:

```sql
SELECT COALESCE(app_id, '(null)') AS app, COUNT(*)
FROM user_device_tokens WHERE is_active GROUP BY 1 ORDER BY 2 DESC;
```

Every row in `(null)` is a device that will keep receiving every app's pushes
until that app ships `appId`.

People running more than one app, and whether they now have a row each:

```sql
SELECT user_id, COUNT(*) AS rows,
       COUNT(DISTINCT device_id) AS devices,
       array_agg(DISTINCT COALESCE(app_id, '(null)')) AS apps
FROM user_device_tokens WHERE is_active
GROUP BY user_id HAVING COUNT(DISTINCT COALESCE(app_id, '')) > 1
ORDER BY 2 DESC;
```

Before `V53`, a person using two apps shows **one** row. After it, and once both
apps have re-registered, two.

## What this does not cover

Targeting is by `NotificationRole` on the notification, and every creation site
sets one — so nothing here depends on the sender getting that right. But a
notification legitimately addressed to `ALL` goes to every app by design, and a
role with no configured app id is unfiltered. If a specific alert still lands in
the wrong place after all three steps, send the notification id and we will
trace the role it carried.
