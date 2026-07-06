# 📣 Backend Update — Mobile Teams Broadcast

Forwardable summary of backend changes for the three apps (Customer, Vendor,
Courier) sharing the stage backend. Full per-item detail lives in:

- Courier → [`COURIER_VERIFICATION_RESPONSE.md`](COURIER_VERIFICATION_RESPONSE.md)
- Vendor → [`VENDOR_VERIFICATION_RESPONSE.md`](VENDOR_VERIFICATION_RESPONSE.md)
- All teams → [`MOBILE_INTEGRATION.md`](MOBILE_INTEGRATION.md)

All changes below are shipped and CI-green on branch
`claude/investigate-delivery-fee-clvYR`.

---

## ⚠️ One coordinated change — test before shipping (ALL teams)

All `LocalDateTime` fields now serialize as UTC **with a trailing `Z`**
(e.g. `2026-07-05T14:30:00Z`). This fixes the Tashkent time-shift, but if any app
manually appended `Z`, it will now double up. **Confirm date parsing in all three
apps on stage before deploying.**

## Shared / breaking (everyone)

- **Cash-only MVP** — hide card UI; `paymentMethod: "CASH"`.
- **No per-event SMS/email** — order updates arrive via WebSocket + push only.
- **Notifications:** subscribe to **`/topic/users/{userId}/notifications`** only;
  drop `/user/queue/notifications` (nothing is published there).
- **Device tokens:** `POST /api/v1/device-tokens` (register) /
  `DELETE /api/v1/device-tokens` (unregister) — not under `/notifications`.
- **`POST /api/v1/auth/refresh` does NOT rotate** — reuse the same refresh token;
  don't expect a new one back (reuse is tolerated, so refresh-once-retry is fine).
- **Server-driven order lifecycle:** unpaid orders auto-cancel; READY-with-no-courier
  times out (cancel + auto-refund); DELIVERED auto-completes. Render whatever status
  arrives; `REFUNDED` is terminal.
- **WebSocket:** CONNECT now **rejects** a missing/invalid `Authorization: Bearer`
  token (no more silent unauthenticated sockets). Per-user/-restaurant topics are
  access-controlled — subscribe only to your own ids.

## Customer team

- **Send an `Idempotency-Key` header** on `POST /api/v1/orders` (one UUID per
  checkout attempt, reused on retries) — eliminates duplicate orders.
- **Push works with your Expo tokens** — no change needed (backend added an Expo
  transport).
- `courierPhone` and `estimatedDeliveryTime` are on `OrderDto` (null until a
  courier is assigned / kitchen accepts). Live courier position:
  `GET /api/v1/orders/{orderId}/track`.
- Delivery-fee preview is route-based and returns min/max bounds.

## Vendor team

- **`isCurrentlyOpen` fixed** — never true while `isOpen` is false/null.
- **`OrderDto` now includes** `pickedUpAt`, `inTransitAt`, `completedAt`,
  `cancelledAt` (for your kitchen-state derivation).
- **Order status/cancel are now idempotent** — replaying the same status, or a
  repeat cancel of an already-cancelled order, returns 200 (no more 422 on
  deploy-drain retries).
- **Restaurant order/kitchen WS topics are owner-only now** (PII fix) — connect
  with the owner's token.
- **Restos import:** `overwriteExisting=false` now correctly **skips** existing
  products (was overwriting).
- Use **`GET /api/v1/restaurants/{id}/financial-report`** (the analytics payouts
  endpoint is admin-only → 403 for owners). `GET /orders/restaurant/{id}/active`
  returns a plain array, not a paged wrapper.

## Courier team

- **Push fixed** (Expo transport) — keep sending `ExponentPushToken[...]`.
- Use the `/me` order endpoints (legacy `/{courierId}/...` were removed — IDOR).
- Reviews score field is `courierRating`; personal profile edits go to
  `PUT /api/v1/users/me` (not `PUT /couriers/me`, which is vehicle-only).
- After a WebSocket drop, reconnect and **re-assert status via
  `PATCH /api/v1/couriers/me/status`** (backend auto-offlines on disconnect).
- New: `POST /api/v1/couriers/me/orders/{orderId}/rating` (rate a delivered order).

## Ops reminders (not app changes)

- Terminate TLS at the reverse proxy for **HTTPS/WSS**.
- Point **OSRM** routing at a self-hosted instance for prod.
- Create the **Telegram alert bot token** and drop it in the alertmanager secret.

---

Questions → reply in the shared channel. Exact paths, fields, and verdicts are in
the three linked docs.
