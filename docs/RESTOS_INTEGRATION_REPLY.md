# Reply to Restos — integration questions

**Draft for review. Two items are marked DECISION NEEDED and must be filled in
before this is sent.** Everything else is verified against the running code as
of 2026-09-19.

---

Thanks — these are the right questions, and several of them land on gaps we'd
rather name now than have you discover against a sandbox.

## Where the integration stands today

What exists is **one-directional and menu-only**: we pull your menu over your
public customer endpoints (categories, products, full menu, cached menu,
categories-with-kitchen-info, all-products) and upsert it into our catalogue.
That import is triggered by the restaurant owner or our staff, on demand.

What does not exist yet is a **partner API on our side**. Our current API is a
first-party one, built for our customer, courier and vendor apps, and
authenticated as a logged-in *user*. There is no partner principal, no API-key
scheme and no partner-scoped endpoint in it. That is the gap your questions 1–4
are really about, and it is the thing to agree before either side writes code.

---

## 1. API documentation

`https://www.zbrr.uz/api/v1/` returning 401 is expected rather than a
permissions problem — there is no endpoint at that path, and our authentication
entry point answers 401 for anything unmatched.

Our OpenAPI spec is deliberately disabled in production. It is enabled in
staging (`https://staging.zbrr.uz/swagger-ui.html`, raw spec at `/api-docs`),
and we can open access to that.

We'd suggest not building against it, though. It documents the API our own apps
use, addressed by our internal identifiers and scoped to user roles — it is not
a partner contract, and treating it as one will send you down a path we'd both
have to unwind. We would rather agree the partner contract described below and
publish that.

## 2. Auth scheme and sandbox credentials

Today: **JWT bearer only, issued when a human logs in** (phone OTP, or email and
password). There is no API key, no OAuth client-credentials flow and no request
signing.

This means there is no sandbox credential we can issue you. The only way Restos
could call us today is by holding a restaurant owner's login and impersonating
them, which neither of us should accept.

Our proposal, for your comment:

- **API key per partner**, plus a per-venue link so a key can only act on the
  venues it is entitled to. Sent as `Authorization: Bearer <key>`.
- Keys issued per environment, revocable independently, never shared between
  staging and production.
- If you would rather have OAuth client credentials or signed requests, say so
  now — it is a reasonable ask and much cheaper to decide before we build than
  after.

## 3. Item updates and order-status reporting

Taking the three parts separately, because the answers differ.

**Availability — supported today, and it is a genuine partial update.**

```
PATCH /api/v1/restaurants/{restaurantId}/menu/items/{itemId}/stock?inStock=false
```

**Price — not supported as a partial update.** The item update endpoint takes a
complete item representation and validates it as such, so a price-only body is
rejected. A price-and-availability partial endpoint is one of the things we'd
add for the partner API.

**Order status back to you — does not exist.** We publish every status change
internally (message bus, WebSocket, push to the vendor app), so the hook point
is there, but nothing currently leaves the platform. This would be a webhook we
POST to an endpoint you give us.

**On identifiers**, which matters more than it looks: the endpoints above are
addressed by *our* internal item id, not your product id. We do store your ids —
every imported item keeps `external_id` and `external_source = RESTOS`, under a
unique index on `(category, source, external_id)` — so resolving your product id
to ours is already possible. The partner API should be addressed by **your**
ids throughout, so you never have to store ours.

**To answer the question as asked:** today it is neither partial updates nor
full replacement. It is a pull — we fetch your menu when a restaurant asks us
to. Moving to you pushing incremental updates is the change under discussion.

## 4. Rate limits and latency

We currently rate-limit authentication endpoints only (3–20 requests per minute
per IP). The menu and order endpoints have no application-level limit, so there
is no published figure we can honestly quote you.

We would rather set a partner limit deliberately than let you discover ours
under load. Proposed starting point, to be revised once we see real traffic:

- **60 requests/minute per partner key** for item price/availability updates,
  burst to 120.
- **A bulk endpoint** for larger changes, so a menu-wide price change is one
  call rather than four hundred.
- `429` with `Retry-After` when exceeded, never a silent drop.

On latency we won't quote numbers we haven't measured under partner load. For
symmetry with our side: our client calling you uses a 5s connect and 10s read
timeout, and we'd suggest you assume the same of us.

## 5. Menu reconciliation

**Yes, and we agree with the reasoning.** A periodic full re-sync alongside
incremental updates is the right design, and it is close to free on our side:
our import is already an upsert keyed on `(restaurant, source, your product id)`
behind a unique index, so re-running it is idempotent by construction. We record
`last_menu_sync_at` per restaurant.

One gap we should name rather than let you assume otherwise: **our import
currently only creates and updates — it has no deletion path.** An item you
delete in Restos stays live and orderable on our side indefinitely. That is
exactly the drift a full re-sync is meant to catch, and today it would not catch
it. We are fixing that as part of this work; it needs us to treat "present in
Restos" as authoritative and deactivate what is missing, which in turn needs the
re-sync to be a guaranteed-complete snapshot rather than a partial page. Worth
confirming your full-menu endpoint gives us that.

Cadence: we'd suggest a nightly full re-sync plus incremental updates as they
happen. Happy to go more frequent if your side is comfortable with it.

## 6. Order status model

```
CREATED → ACCEPTED → PREPARING → READY → COURIER_ASSIGNED
       → PICKED_UP → IN_TRANSIT → DELIVERED → COMPLETED
```

- `CANCELLED` — reachable from `CREATED`, `ACCEPTED`, `PREPARING`, `READY` and
  `COURIER_ASSIGNED`.
- `REFUNDED` — a separate state, reachable from `CANCELLED`, `DELIVERED` and
  `COMPLETED`. A refund moves the payment state; it does not un-cancel an order.

Two transitions worth flagging because they are not the naive linear path: a
courier is often assigned while the food is still cooking, so
`COURIER_ASSIGNED → PREPARING → READY` is normal, and `READY → PICKED_UP` is the
common pickup path rather than `COURIER_ASSIGNED → PICKED_UP`.

**When the restaurant declines after the customer has paid:** the order moves to
`CANCELLED` with a recorded reason, and a refund is issued automatically against
the confirmed payment.

We should be straight with you about one thing: **that refund is best-effort.**
If the payment provider call fails, we log it for manual settlement and the
order still reads `CANCELLED` — the customer is made whole by a person rather
than by the system. It is rare, it is monitored, and we are not going to
describe it as fully automatic when it isn't.

Unpaid orders are auto-cancelled after 30 minutes.

## 7. Pricing

> **DECISION NEEDED — commercial. Do not send this section as written.**
>
> The factual behaviour below is accurate. What we tell Restos about our
> *policy* — whether partner prices are marked up, by how much, and whether that
> is disclosed — has not been decided. Fill this in before sending.

Current behaviour, precisely:

- We store the price you send, and separately a display price.
- **When you send `priceWithMargin`, we use it as sent.**
- When you send only `price`, a margin is applied before display.
- The displayed price is what the customer is charged — it becomes the line
  unit price on the order.

Rounding: prices are held to two decimal places and rounded half-up on write. In
UZS that means the second decimal place is never meaningful in practice; if you
send whole-so'm prices you will get whole-so'm prices back.

**What we need to confirm to you:** whether the price a customer sees is exactly
the price you sent, and if not, the rule. We will not leave this ambiguous —
your restaurants will notice, and they should hear it from us first.

## 8. Cancellation window

> **DECISION NEEDED — commercial. Do not send this section as written.**
>
> The behaviour below is what the code does today. Whether it is the policy we
> want, and who absorbs the cost after the kitchen starts, is yours to decide.
> Restos is right to ask, and no restaurant will accept the current answer once
> they notice it.

Current behaviour, precisely:

- A customer can cancel while the order is in `CREATED`, `ACCEPTED`,
  `PREPARING`, `READY` or `COURIER_ASSIGNED` — in other words **right up until
  the courier physically takes the food**, including well after the kitchen has
  started cooking.
- The refund is **full**, regardless of how far preparation got.
- There is no cancellation fee, no partial refund and no cutoff point anywhere
  in the system.

In effect the restaurant absorbs the whole cost of a late cancellation, and
nothing in the platform tells them that is happening. A cutoff — most naturally
at `PREPARING`, after which a cancellation is either refused or partially
refunded — is the obvious fix, and we'd like your view on where the line should
sit for Restos venues.

## 9. Retry and idempotency

**Your calls to us — supported.** Order creation accepts an `Idempotency-Key`
header; a replay returns the original order rather than creating a second, and a
unique index backs that up for two simultaneous requests carrying the same key.
Cancellation is idempotent too — cancelling an already-cancelled order succeeds
rather than erroring.

One mismatch to fix: our key is currently **globally unique rather than scoped
per partner**, where yours is `(partner, venue, your order id)`. We will scope
ours the same way for the partner API — otherwise two partners can collide on a
key neither of them chose badly.

**Our calls to you — nothing to report yet**, because we make no write calls to
Restos at all today. When we do: yes, we will retry with a stable reference, and
your `(partner, venue, order id)` key is the right shape. Our order reference is
`FD-YYYYMMDD-XXXXXX` (for example `FD-20260919-A7K2M9`); it is unique, assigned
at creation and never changes, so it is the value to key on.

---

## What we need from you

1. **An order-creation endpoint**, and confirmation that an order created
   through it follows the same print and kitchen-station routing as one rung up
   on the till. We are asking for order creation rather than "print a receipt"
   deliberately — we assume printing is a consequence of the check existing, and
   your kitchen-station data in the menu API suggests that is right. Correct us
   if not.
2. **A write-scoped credential** per venue. The key we hold today is used
   against your public customer endpoints and we assume it will not authorise
   writes.
3. **Your idempotency semantics on that endpoint** — the header or field name,
   and what a replay returns.
4. **What happens to an order referencing a product you no longer have**, so we
   can decide whether to block the order or send the line as free text. This is
   a real case for us: items created directly in our panel have no Restos
   product behind them.
5. **An endpoint to receive order-status webhooks**, and which of our states you
   actually care about.

## Proposed next step

A short call to settle direction — specifically whether you push menu updates to
us or we keep pulling, and the auth scheme. Once those two are fixed the rest is
a contract we can write down and both build against. We would rather spend an
hour on that than exchange specifications.
