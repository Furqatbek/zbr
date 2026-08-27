# Mobile Integration Notes — Stage MVP

For the three app teams (Customer, Vendor, Courier) sharing the single stage
backend. This is the delta the backend introduced recently: **breaking changes
first**, then per-team instructions. Endpoint details live in `docs/*-api.md`
and Swagger (`/swagger-ui.html` on stage).

---

## Breaking changes — act on these

| Change | Who | What to do |
|--------|-----|------------|
| **Token refresh is not implemented in any app** — caused a live outage | **All** | Access tokens expire; without refresh every session dies within the hour and WebSockets stop reconnecting. → [MOBILE_TOKEN_REFRESH.md](MOBILE_TOKEN_REFRESH.md) |
| **Staging is available** at `https://staging.zbrr.uz` | All | Build against it instead of production. Push is OFF there and tokens expire in 60s, both on purpose — read the cautions first. → [STAGING.md](STAGING.md#cautions--read-before-pointing-an-app-at-this) |
| **Production base URL is the bare apex** `https://zbrr.uz/api/v1` | All | WebSockets: `wss://zbrr.uz/ws`. Do **not** use `www.` — see below. |
| `POST /api/v1/orders/{id}/pay/confirm` is now PLATFORM/ADMIN-only | Customer | Remove any client call to `/pay/confirm`. Payment confirmation is server-side (cash is confirmed when the courier completes delivery). |
| Legacy courier endpoints `POST /api/v1/couriers/{courierId}/accept/{orderId}` and `/complete/{orderId}` were **removed** (IDOR) | Courier | Use the `/me` equivalents: `POST /me/orders/{orderId}/accept`, `PUT .../pickup`, `PUT .../transit`, `POST .../complete`. |
| `GET /api/v1/consumers/{id}` is now ADMIN/PLATFORM-only | Customer | Use the `/me` profile endpoints for the logged-in user. |
| No per-event SMS/email anymore | All | Order/status updates arrive **only** via WebSocket + push. Don't tell users to "check SMS" (OTP SMS for login still works). |
| Online card payment is **not** available in this MVP (no acquiring contract) | Customer, Vendor | Offer **CASH only** as the payment method (`paymentMethod: "CASH"` on `POST /{orderId}/pay`). Hide/disable card UI. |
| iOS device-token registration must send `appId` (your bundle id) | **All (iOS)** | One APNs key serves all three apps, so each push needs your app's own `apns-topic`. Omit it and your tokens get rejected **and auto-deactivated**. → [see below](#push-notifications--device-token-registration-all-teams) |

## Production base URL (all three teams)

```
REST       https://zbrr.uz/api/v1
WebSocket  wss://zbrr.uz/ws
Images     https://zbrr.uz/api/v1/images/...   (returned by the API; don't build these yourself)
```

**Use the bare apex — no `www.`.** `www.zbrr.uz` answers with a `308` to the
apex, so REST calls survive a mis-pointed build (308 preserves the method and
body; a 301 would not). **WebSockets do not**: a redirect cannot carry the
`Upgrade` handshake, so `wss://www.zbrr.uz/ws` fails outright and the app gets no
live order updates. Hardcode the apex.

`http://` is redirected but should never be used — iOS ATS and Android's
cleartext policy block plain `http`/`ws` in release builds anyway.

## New: Idempotency-Key on order creation (Customer team — strongly recommended)

`POST /api/v1/orders` now accepts an optional header:

```
Idempotency-Key: <uuid>
```

Generate **one UUID per checkout attempt** and reuse it for every retry of that
attempt (timeouts, flaky network, user double-tap). The backend guarantees the
same key resolves to the **same order** — no more duplicate orders/deliveries
from retries. New checkout = new UUID. Without the header, behavior is
unchanged (but you don't get the protection).

Also note: the backend now drains in-flight requests on deploys, but a request
can still fail mid-deploy — with the idempotency key, **retrying order creation
is always safe**.

## Server-driven order lifecycle (all teams)

The backend now moves orders on its own; apps must render whatever status
arrives rather than assuming only user actions change state:

- **CREATED** orders unpaid for ~30 min → auto-**CANCELLED**.
- **READY** delivery orders with no courier for the configured timeout →
  auto-**CANCELLED** (+ auto-refund if paid).
- **DELIVERED** orders → auto-**COMPLETED** after a grace period.
- Cancelling a paid order triggers an automatic refund; **REFUNDED** is a
  terminal status and can follow DELIVERED/COMPLETED/CANCELLED.

Full status set:
`CREATED → ACCEPTED → PREPARING → READY → COURIER_ASSIGNED → PICKED_UP → IN_TRANSIT → DELIVERED → COMPLETED`, plus `CANCELLED`, `REFUNDED`.

**Couriers may accept before food is ready**: `COURIER_ASSIGNED` can occur while
the kitchen is still at ACCEPTED/PREPARING, and the order can then move to
PREPARING/READY *while a courier is already assigned*. Vendor and courier UIs
must not treat COURIER_ASSIGNED as "kitchen done". Pickup is only allowed once
the order has been READY.

## WebSocket destinations (STOMP)

| Destination | Audience | Payload |
|-------------|----------|---------|
| `/topic/orders/{orderId}` | anyone tracking one order | OrderDto on every status change |
| `/user/queue/orders` | logged-in **customer** | OrderDto for your orders |
| `/topic/restaurants/{restaurantId}/orders` | **vendor** | OrderDto: new orders + status changes |
| `/topic/couriers/orders/available` | all **couriers** | new delivery order available (broadcast) |
| `/user/queue/orders/new` | online **courier** | new delivery order available (direct) |
| `/topic/users/{userId}/notifications` | any logged-in user | persistent notifications (**canonical — subscribe to this one only**) |
| `/topic/broadcast/notifications` | everyone | platform-wide announcements |

> **Notifications: subscribe to exactly ONE user destination.**
> `/topic/users/{userId}/notifications` is the only per-user destination the
> backend publishes to. `/user/queue/notifications` appeared in older API docs
> but nothing is ever sent there — drop that subscription (double-subscribing
> was also the suspected unread-badge double-count).

---

## Push notifications — device token registration (all teams)

Register after login and again whenever the OS rotates the token:

```
POST /api/v1/device-tokens
{ "token": "<raw token>", "platform": "IOS", "deviceId": "<stable device id>",
  "appId": "com.zbr.owner" }
```

| Field | Notes |
|-------|-------|
| `token` | Raw **FCM** registration token (Android) or raw **APNs** device token, 64-char hex (iOS). `ExponentPushToken[...]` is still accepted and routed via Expo, but native tokens are preferred. |
| `platform` | `ANDROID` \| `IOS` (alias: `deviceType`) |
| `deviceId` | Stable per-device id. Registration **upserts on (user, deviceId)** — a rotated token replaces the row instead of creating a duplicate (duplicates = N copies of every push). |
| `appId` | **iOS: required.** Your bundle identifier. Aliases: `bundleId`, `packageName`. |

Unregister on logout: `DELETE /api/v1/device-tokens` with `{"deviceToken": "..."}`.

### ⚠️ iOS: `appId` is mandatory once more than one app exists

One APNs `.p8` key serves **all three apps** (the key is scoped to the Apple Team,
not to an app), but every push must carry that app's own bundle id in the
`apns-topic` header — the backend takes it from the `appId` you registered.

If you omit `appId`, your tokens fall back to the backend's single default topic.
For two of the three apps that topic is wrong, Apple rejects the push
(`400 BadDeviceToken` / `403 TopicDisallowed`), and the backend's dead-token
pruning then **deactivates the token**. The symptom is *"push worked once, then
stopped forever for our app"*. Send `appId` and this cannot happen.

Android needs no equivalent — all three Android apps live in **one Firebase
project** (each with its own client-side `google-services.json`), and FCM routes
by the registration token itself.

## Team 1 — Customer app

1. **Add the `Idempotency-Key` header** to `POST /api/v1/orders` (see above).
2. **Remove `/pay/confirm` calls** (403 now). Flow: create order →
   `POST /api/v1/orders/{id}/pay` with `{"paymentMethod": "CASH"}` → order
   proceeds; confirmation happens on delivery.
3. **Cash-only UI** for this MVP.
4. **Delivery fee preview**: `POST /api/v1/orders/calculate-delivery-fee` —
   distance is now route-based (road distance, not straight line); response
   includes the min/max fee bounds so you can explain clamped prices.
5. **Handle server-driven statuses** (auto-cancel of unpaid orders, no-courier
   cancellation with refund, auto-complete). Show REFUNDED as terminal.
6. Auth: `POST /api/v1/auth/refresh` body is `{"refreshToken": "..."}` — send
   the *refresh* token, not the access token. Phone OTP flow:
   `/api/v1/auth/phone/request-otp → verify-otp → complete-registration`.
7. Profile: use `/me` endpoints only; `GET /consumers/{id}` is admin-only now.
8. **Courier contact + ETA in order payloads**: `OrderDto` (REST and WebSocket)
   carries `courierId`, `courierName`, and now **`courierPhone`** — all null
   until a courier is assigned, so gate the call-courier button on non-null.
   For ETA, use `estimatedDeliveryTime` (set when the restaurant accepts with a
   prep time; may be null before that — hide the ETA text when null). For live
   courier coordinates use `GET /api/v1/orders/{orderId}/tracking`.

## Team 2 — Vendor (restaurant) app

1. **COURIER_ASSIGNED can arrive before READY.** Kitchen flow continues:
   ACCEPTED → PREPARING → READY still applies while a courier is assigned. Don't
   hide kitchen actions when the courier appears early.
2. Order feed: subscribe `/topic/restaurants/{restaurantId}/orders` — new
   orders and every status change (including customer cancellations and
   auto-cancellations) arrive there. No SMS/email fallbacks.
3. Location can be updated without resending the whole profile:
   `PATCH /api/v1/restaurants/{id}/location` (lat/lng only). Status toggles:
   `PATCH /{id}/status`, `PATCH /{id}/toggle-open`.
4. Expect orders to disappear from "active" on their own: DELIVERED orders
   auto-complete, stale unpaid orders auto-cancel.
5. **Always send `estimatedPrepTimeMinutes`** in the status-update body when
   moving an order to ACCEPTED/PREPARING (`PATCH /api/v1/orders/{orderId}/status`).
   It is what powers the customer app's ETA display — omit it and customers see
   no ETA at all.
6. **READY orders can be auto-cancelled by the system** (no courier found within
   the timeout; the customer is auto-refunded). The kitchen may have prepared
   food for a cancelled order — surface `cancellationReason` (present on
   OrderDto) so staff see why, instead of it looking like a glitch.
7. `courierPhone` is now on OrderDto (null until assigned) — useful for
   handoff coordination at pickup.

## Team 3 — Courier app

1. **Switch to `/me` order endpoints** (the old `/{courierId}/...` ones are
   gone): `GET /me/available-orders`, `POST /me/orders/{orderId}/accept`,
   `PUT /me/orders/{orderId}/pickup`, `PUT /me/orders/{orderId}/transit`,
   `POST /me/orders/{orderId}/complete`, `GET /me/orders/active|history`,
   `POST /me/orders/{orderId}/issue`.
2. **Accepting early is allowed**: orders can be accepted once the restaurant
   has ACCEPTED them (before READY). **Pickup stays blocked until READY** — show
   "waiting for kitchen" between accept and pickup if needed.
3. **Auto-offline on WebSocket disconnect**: if the socket drops, the backend
   marks the courier OFFLINE. Implement WS auto-reconnect and, after
   reconnecting, re-assert availability via `PATCH /api/v1/couriers/me/status`.
4. New-order alerts: subscribe `/topic/couriers/orders/available` (broadcast)
   and `/user/queue/orders/new` (direct); payload includes restaurant/dropoff
   coordinates and the delivery fee.
5. Cash on delivery: just call `POST /me/orders/{orderId}/complete` — the
   backend records/confirms the cash payment server-side; there is no separate
   client call.

---

## Shared plumbing notes

- Access tokens expire in ~1h, refresh tokens in ~7d; on 401, refresh via
  `/api/v1/auth/refresh` and retry once.
- Deploys drain connections gracefully, but build clients to tolerate brief
  5xx/connection errors with a short retry (order creation is safe to retry
  when you send the Idempotency-Key).
- One backend serves all three apps on stage — coordinate breaking-change
  testing in a shared channel before shipping app updates.
- `POST /api/v1/auth/refresh` returns the **same** refresh token (it is not
  rotated) — keep reusing it; don't expect a new one back.

## Not implemented (don't build UI against these yet)

Confirmed absent on the backend — keep them feature-flagged off:

- **No self-push** — the push pipeline addresses a *user*, not the originating
  *device*, so the vendor who caused an event still receives its push. Clients
  dedupe by `orderId` against the WebSocket event.
- **Vendor review reply** — no `POST /restaurants/{id}/reviews/{reviewId}/reply`.
- **Vendor → courier rating** — the only rating endpoint is
  `POST /couriers/me/orders/{orderId}/rating` (courier → order, a different
  direction).
- **Notification preferences** — `category` is a read-time filter only; nothing
  is persisted or honored server-side at send time.
- **Financial report `refunds` / `cancellations` fields** and **`soldItems[]`
  breakdown** — not on the report DTO.
- **Staff accounts CRUD** — no staff entity/endpoints; only the restaurant owner
  has access today.

## Known backend caveat

A courier whose status is `SUSPENDED` / `PENDING_APPROVAL` can currently clear it
themselves via `PATCH /couriers/me/status`. Harmless while no admin suspension
flow exists; a server-side guard must land with the first suspension feature.
