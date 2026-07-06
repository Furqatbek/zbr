# Backend Response — Vendor App `BACKEND_HANDOFF.md`

Point-by-point response to the vendor (restaurant-owner) app's handoff,
verified against the backend source. Reflects fixes shipped on branch
`claude/investigate-delivery-fee-clvYR`.

**Verification:** 36 checks → 17 confirmed OK · 9 gaps (all "awaited" features) ·
7 mismatches · 2 bugs · 1 partial. **Shipped:** 6 backend fixes.

## Critical asks (blocking launch)

| # | Ask | Status |
|---|-----|--------|
| 1 | HTTPS/WSS transport | **Ops** — terminate TLS at your reverse proxy (single upstream, see `DEPLOYMENT_CONSTRAINTS.md`). The app serves HTTP behind it; `/ws` becomes `wss://` via the proxy. No app code needed. |
| 2 | Restos server-side SSRF allowlist | **CONFIRMED** — enforced server-side (blocks localhost/private/link-local/`.local` before any fetch), plus redirects disabled and an allowlist. Defense-in-depth beyond your client checks. |
| 3 | JWT on STOMP CONNECT | **CONFIRMED + hardened** — `Authorization: Bearer <token>` on CONNECT authenticates; **now also rejects** a missing/invalid token instead of soft-failing. |
| 4 | `courierPhone` + `estimatedDeliveryTime` on OrderDto | **CONFIRMED** — both on `OrderDto`; and the WebSocket sends the **full OrderDto**, so `courierPhone` rides `/topic/restaurants/{id}/orders`, not just REST. |
| 5 | `isOpen` vs `isCurrentlyOpen` | **FIXED** — `isCurrentlyOpen` is now `ACTIVE && isOpen==true`; can never be true while `isOpen` is false/null. |
| 6 | Idempotency on status/cancel | **Not idempotent** — see below; client must handle 422 on replay (offer to make same-state a no-op). |

## ✅ Fixed on the backend (this cycle)

- **PII (critical, we found this while verifying):** the WebSocket subscribe
  authorizer did **not** cover restaurant topics — any authenticated user could
  subscribe to `/topic/restaurants/{anyId}/orders` (customer name/phone/address)
  and `/kitchen`. Now restricted to that restaurant's **owner or admin/platform**
  (fails closed). CONNECT also rejects unauthenticated sockets outright.
- **Item 5 — `isCurrentlyOpen`:** manual toggle is authoritative; null/false → closed.
- **Item 4 (timestamps) — `OrderDto` now exposes `pickedUpAt`, `inTransitAt`,
  `completedAt`, `cancelledAt`** (the entity already had them). Your
  `COURIER_ASSIGNED` timestamp-derivation logic now has all the fields it reads.
- **Notifications `read-all` now accepts POST** (you call POST; was PATCH-only →
  405). Still needs `?userId=<self>`.
- **Restos `overwriteExisting=false` now SKIPS existing products** (Import
  semantics). Previously it always overwrote them — real bug. `true` (Sync)
  still updates in place.

## ⚠️ Open questions — answered

1. **`isCurrentlyOpen` false when `isOpen` false** → FIXED (above). Note: your
   "manual ∧ schedule" definition is **not** what the code does — the manual
   toggle is authoritative and the schedule is not layered on. Say if you want
   true schedule-gating (that would auto-close a manually-opened restaurant
   outside hours — a behavior change).
2. **`courierPhone` on WS `/topic/restaurants/{id}/orders`** → YES, confirmed
   (full OrderDto payload).
3. **Status/cancel idempotent?** → **No.** The order state machine has no
   self-transitions, so replaying an already-applied status (e.g. `status=ACCEPTED`
   when already ACCEPTED) returns **422 "Cannot transition"**, and a second cancel
   on an already-CANCELLED order returns 422 "cannot be cancelled". **Client
   mitigation:** on 422 for a mutation, re-fetch `GET /orders/{id}` and treat
   "already in target state" as success. If you'd rather the backend treat a
   same-state transition as an idempotent no-op (200 echo), say so — it's a small
   state-machine change but I didn't want to alter money-path semantics unasked.
4. **Order-topic payload: full order or delta?** → **Full `OrderDto`** on every
   message. You only need the reconnect re-fetch to catch messages missed while
   disconnected — no per-message re-fetch.
5. **`GET /orders/{orderId}/tracking`** → **already exists** (not future), but the
   path is **`/track`** not `/tracking`: `GET /api/v1/orders/{orderId}/track` →
   `OrderTrackingDto` with courier name/phone/lat/lng + ETA + restaurant/delivery
   coords. Courier fields populate once a courier is assigned.

## 🟡 Mismatches to correct (app-side)

- **Auth `/refresh` does NOT return a rotated pair** — it returns the **same**
  refresh token (only the access token changes) and never revokes it. Keep and
  reuse the original refresh token; don't expect a new one. (Reuse is tolerated,
  so your refresh-once-retry logic is fine.)
- **`GET /orders/restaurant/{id}/active` is NOT paged** — it returns a raw
  `OrderDto[]` (no `content`/`totalElements`); `page`/`size` are ignored. Parse
  as an array. (`/orders/restaurant/{id}` *is* paged.) Note it includes DELIVERED
  in "active".
- **Device tokens:** the path is **`POST /api/v1/device-tokens`** (register) and
  **`DELETE /api/v1/device-tokens`** (unregister) — **not**
  `POST /api/v1/notifications/device-token` (that 404s).
- **Payouts endpoint** `GET /api/v1/analytics/financial/restaurants/{id}/payouts`
  is **admin/finance-only (403 for RESTAURANT_OWNER)** and its field names differ
  (`totalGrossSales`, `netPayoutAmount`, `dailyTrend`…). Use
  **`GET /api/v1/restaurants/{id}/financial-report`** instead — it permits
  RESTAURANT_OWNER, validates ownership, and its field names match your list
  exactly.

## ✅ Confirmed working as you assume

Restaurant: all 6 endpoints (`/my`, `PUT /{id}` partial, `PATCH /{id}/location`,
`PATCH /{id}/toggle-open`, `POST /{id}/logo`, `POST /{id}/cover-image`, multipart
field `file`); **all 28 RestaurantDto fields** (incl. `logoUrl`/`coverImageUrl`
which are always serialized even when null). Orders: paged list, `PATCH
/{id}/status` `{status, estimatedPrepTimeMinutes?}`, `POST /{id}/cancel`
`{reason, requestRefund}`, all OrderItemDto fields incl. nested `modifiers`.
WebSocket: `/topic/restaurants/{id}/orders` (new order + every status change,
full OrderDto) and `/topic/restaurants/{id}/kitchen` are both published.
Reviews: endpoint + full ReviewDto. Ratings: `GET /analytics/cx/ratings/restaurant/{id}`
(distribution + ratingCount). Financial: `GET /restaurants/{id}/financial-report`
matches every field. Notifications: `GET /me?role=RESTAURANT…`,
`GET /user/{id}/unread-count?role=RESTAURANT`, `PATCH /{id}/read`. Restos:
`preview-menu` / `import-menu` paths, bodies, and `MenuImportResult` shape.
Canonical notification topic `/topic/users/{userId}/notifications` (drop
`/user/queue/notifications`).

## Awaited feature-flagged endpoints — all confirmed absent (keep them hidden)

None of these exist yet; your feature flags are correctly off. Build cost per
item if/when you want them:

- **Review reply** (`POST /restaurants/{id}/reviews/{reviewId}/reply`) — add a
  `reply` column to Review + endpoint.
- **Vendor→courier rating** — new endpoint + DTO (`stars, criteria[], note, orderId`)
  + persistence (the existing `/couriers/me/orders/{id}/rating` is the *courier→order*
  direction, different).
- **Notification preferences** — new entity + CRUD + enforcement in the dispatch
  path (today `category` is only a read-time filter, not persisted/honored).
- **Financial `refunds`/`cancellations`** — add fields to the report DTO + source them.
- **Sold-items breakdown** — new OrderItem aggregation (field or endpoint).
- **Staff accounts CRUD** — new `RestaurantStaff` entity + endpoints; also wire
  `hasRestaurantAccess` (currently a TODO returning false for non-owners).
