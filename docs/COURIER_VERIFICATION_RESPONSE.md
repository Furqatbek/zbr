# Backend Response — Courier App `BACKEND_VERIFICATION.md`

Point-by-point response to the courier team's verification checklist, verified
against the backend source (file:line evidence available on request). Updated to
reflect the fixes already shipped on branch `claude/investigate-delivery-fee-clvYR`
(CI green through commit `68b7b9c`).

**Verification result:** 11 items + appendix, 31 sub-checks →
20 confirmed OK · 5 gaps · 5 mismatches · 1 partial.
**Shipped:** 6 backend fixes. **Awaiting your decision:** 2. **Deferred:** 1.

## Status at a glance

| Item | Concern | Verdict | Resolution | Owner |
|------|---------|---------|------------|-------|
| 1 | STOMP subscribe authz / PII | GAP | **FIXED** — per-destination subscribe authorization | backend ✅ |
| 2 | Refresh-token rotation | CONFIRMED | **Decided:** keep reuse-tolerant for MVP | closed |
| 3 | Expo vs FCM push | MISMATCH | **FIXED** — backend Expo Push transport added | backend ✅ |
| 4a–4d,4f,4g,4h | Notification endpoints | CONFIRMED | Exist as specified | — |
| 4e | `read-batch` method | MISMATCH | **FIXED** — now accepts POST + PATCH | backend ✅ |
| 5 | `DELETE /users/me` | GAP | **FIXED** — added | backend ✅ |
| 6 | Courier rates order | GAP | **FIXED** — endpoint + storage added | backend ✅ |
| 7 | Reviews endpoint | PARTIAL | Exists; field is `courierRating` | app |
| 8 | Earnings fields | CONFIRMED | Your fields win; `period` param ignored | app |
| 9a | Auto-offline on disconnect | CONFIRMED | Works (AVAILABLE/ON_BREAK only) | — |
| 9b | Status endpoint / clobber | CONFIRMED | Works; guard **deferred w/ condition** (before admin suspension ships) | backend |
| 10 | `ORDER_TAKEN` broadcast | CONFIRMED | Exactly as expected | — |
| 11.1 | Timestamp `Z` suffix | CONFIRMED | **FIXED** — all `LocalDateTime` now UTC+`Z` ⚠️ | backend ✅ |
| 11.2 | JWT size | MISMATCH | Tiny (no roles in token); read role via `/me` | app |
| 11.3 | Auth/user endpoints exist | CONFIRMED | All four exist | — |
| 11.4 | `PUT /couriers/me` personal fields | MISMATCH | Vehicle-only; use `PUT /users/me` | app |
| 11.5 | OSRM routing | CONFIRMED | Defaults to public demo; set prod URL | ops |
| A | Phone normalization / OTP | GAP | **FIXED** — +998 validation, invalids 400 | backend ✅ |

---

## 🔴 Launch blockers

### Item 1 — STOMP subscribe authorization (PII) → FIXED
Was: the broker only authenticated CONNECT; any connected client could
`SUBSCRIBE` to another user's `/topic/**` and harvest customer name/phone/
address, notifications, and courier location.

Now: `WebSocketDestinationAuthorizer` checks every SUBSCRIBE frame:
- `/topic/orders/{id}` (full OrderDto w/ customer PII) → **party-to-order only**
  (consumer, assigned courier, that restaurant's owner/staff, admin/platform).
- `/topic/users/{id}/notifications` → **that user or admin/platform**.
- `/topic/couriers/{id}/location` → **that courier or admin/platform** (consumers
  get the assigned courier's position via `GET /orders/{id}/tracking`).
- `/topic/orders/{id}/taken`, the offer feed, `/queue/**`, `/user/**` → any
  authenticated user (no PII).
- Unauthenticated session → **denied** for all of the above.

App impact: none if you only subscribe to your own order/notification/location
topics. A rejected subscribe returns a STOMP `ERROR` frame — handle it as
"not authorized" (usually a bug in which id you subscribed to).

### Item 3 — Expo push token vs raw FCM → FIXED (backend Expo transport)
Decision: backend adds the Expo transport (iOS native tokens would have needed
an APNs import step or the Firebase iOS SDK — a native dependency change too late
in the cycle; Expo handles the FCM/APNs fan-out).

Now: `ExpoPushService` posts to `https://exp.host/--/api/v2/push/send`.
`PushNotificationConsumer` routes **by token format** — `ExponentPushToken[...]`
/ `ExpoPushToken[...]` → Expo; raw registration tokens → Firebase (unchanged) —
so both transports coexist. Expo tokens are **no longer deactivated by the FCM
error path**; they are only deactivated when Expo itself reports
`DeviceNotRegistered`. Expo works even when Firebase is disabled.

App impact: keep sending `ExponentPushToken[...]` as you do — no change needed.
Config (ops): `EXPO_PUSH_ENABLED` (default true), `EXPO_PUSH_URL`,
`EXPO_ACCESS_TOKEN` (optional).

---

## ✅ Fixed on the backend (shipped, CI green)

- **Item 5** — `DELETE /api/v1/users/me` (self account deletion; app-store req).
- **Item 6** — `POST /api/v1/couriers/me/orders/{orderId}/rating`, body
  `{"rating":1..5,"comment":"..."}`. Assigned courier only, after
  DELIVERED/COMPLETED. Stored on the order (distinct from the consumer `reviews`).
- **Item 4e** — `/api/v1/notifications/read-batch` now accepts **POST** (and
  PATCH). Body is a raw JSON array of IDs: `[1,2,3]`.
- **Appendix A** — phone validation tightened to `^(\+?998[0-9]{9}|[0-9]{9})$`
  on request-otp / verify-otp / complete-registration. `+9988901234567` (13
  digits), `+79001234567` (non-Uzbek), and empty now return **400** before any
  SMS. Bare 9-digit and `+998…` still accepted.
- **Item 11.1** — all `LocalDateTime` fields now serialize as UTC with a trailing
  `Z` (e.g. `2026-07-05T14:30:00Z`).
  > ⚠️ **Coordinate before deploy:** this changes the wire format for **all
  > three apps**. If any client manually appended `Z`, it will now double up.
  > Test date parsing in customer, vendor, and courier apps on stage first.

---

## 🟡 App-side adjustments (backend is correct as-is)

- **Item 2 — refresh is reuse-tolerant, NOT rotated.** `/auth/refresh` returns
  the **same** refresh token (only the access token changes) and never revokes
  it. Good news: your three uncoordinated refresh stacks will **not** force-
  logout couriers. Don't expect the returned `refreshToken` to change.
- **Item 4b — it's `PATCH`** `/{id}/read` (docs said PUT — docs are wrong).
- **Item 4c — `PATCH /read-all`: `userId` is a REQUIRED query param** (omit → 400).
- **Item 4h — `NEW_DELIVERY_AVAILABLE` is valid.** `NEW_ORDER_NEARBY` and
  `ORDER_ASSIGNED` do **not** exist (closest: `COURIER_ASSIGNED`).
- **Item 7 — reviews field is `courierRating`** (Integer, nullable), not
  `rating`. `orderId`/`comment`/`createdAt` match.
- **Item 8 — earnings: your fields win** (`averagePerDelivery`, `cashEarnings`,
  `cardEarnings`, `withdrawableBalance`; docs are stale). ⚠️ The endpoint takes
  **no `period` param — `period=TODAY` is ignored**; you get the backend's
  computed window.
- **Item 11.2 — token has no roles** (only `sub`/`type`/`iat`/`exp`), tiny, no
  secure-store risk. Read the user's role from `GET /users/me`, not the JWT.
- **Item 11.4 — `PUT /couriers/me` persists vehicle fields only.**
  `firstName/lastName/email/phone` are ignored — send personal edits to
  **`PUT /api/v1/users/me`**.

---

## ✅ Confirmed working as you assumed

- **4a,4d,4f,4g** endpoints exist as specified.
- **9a — auto-offline on WS disconnect** works, but only from `AVAILABLE`/
  `ON_BREAK` (a `BUSY` courier stays BUSY on a dropped socket — intentional/safe).
- **9b — both `PUT` and `PATCH /me/status` exist;** `PUT` reads `{"status":...}`;
  same-status is idempotent.
- **10 — `ORDER_TAKEN` is exactly right:** broadcast on **both**
  `/topic/couriers/orders/available` and `/topic/orders/{orderId}/taken`, payload
  `{type:'ORDER_TAKEN', orderId, externalOrderNo, courierId, courierName, timestamp}`.
- **11.3 — all four endpoints exist** (`auth/register`, `auth/logout`,
  `users/me/logout-all`, `PUT users/me`).
- **A.3 — empty phone → 400.**

---

## Decisions recorded

- **Item 2 (refresh rotation)** — CLOSED: keep **reuse-tolerant** for the MVP
  (the courier token manager handles it either way; rotation safety stays as
  future-proofing on the client).
- **Item 3 (Expo vs FCM)** — CLOSED: backend Expo transport shipped (above).

## Deferred (with condition)

- **Item 9b (status transition guard)** — DEFERRED, **on the written condition
  that it lands before any admin/dispatcher suspension flow ships.** Today a
  `SUSPENDED` / `PENDING_APPROVAL` courier can un-suspend themselves with one
  `PATCH /me/status`; that is only harmless because no suspension flow exists
  yet. A `MUST-DO` comment is pinned in `CourierService.updateStatus` so the
  guard is added alongside the first suspension feature.

## Ops

- **Item 11.5 (OSRM)** — point `DELIVERY_ROUTING_OSRM_URL` at a self-hosted OSRM
  for prod (default is the public `router.project-osrm.org` demo).
