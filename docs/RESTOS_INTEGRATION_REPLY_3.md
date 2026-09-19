# Reply to Restos — round three

**Draft for review.** One item is marked DECISION NEEDED. Everything else is
verified against shipped code as of 2026-09-19.

Attach `PARTNER_API.md` when sending.

---

It is built. The partner API you have been waiting on is done, and so is the
order push in the other direction — your venues' orders will print on their own
tills. What is left is a short list of things only you can give us, and one
schema we guessed at.

## What you can call now

The full contract is in the attached document; the shape in four lines:

- **Auth** is `X-Partner-Key`, a key per partner per environment, plus a
  per-venue grant carrying `MENU_WRITE` and `ORDER_STATUS_WRITE` independently.
  The shape you proposed and the shape we proposed, which turned out to be the
  same shape.
- **`PATCH /api/v1/partner/venues/{yourVenueId}/menu/items/{yourProductId}`** —
  price and availability, partial, addressed by your ids throughout. This is the
  one that stops an imported menu going stale between syncs.
- **`POST .../menu/items`** — the bulk path, up to 1000 items, for a markup
  moving across a whole menu. Partial success is reported rather than rolled
  back: a change across four hundred dishes is not lost because three of them
  were deleted here last week.
- **`POST /api/v1/partner/orders/{ourReference}/status`** — `ACCEPTED`,
  `PREPARING`, `READY`, `DECLINED`. Safe to retry; a status the order cannot
  reach answers `409` naming both, as yours does.

We set the rate limits we proposed: 60/minute for menu, 120/minute for order
status, per partner across all your keys and venues, `429` with `Retry-After`
rather than a silent drop. Say if that is tight for a large venue count.

**One thing to note about keys.** They are stamped with the environment they
were issued for and refused anywhere else. You will need two, and a staging key
will simply not work against production. That is deliberate — a test order
printing in a real kitchen is the one mistake whose cost lands outside both our
companies.

## What we send you — and the one thing we guessed

Orders now POST to your `/api/v1/partner/orders` when a venue is switched on.
Asynchronous and off our checkout path, so your system being slow or down never
costs a customer their order; we retry, and we distinguish a timeout from a
refusal so a rejection is never retried into the ground.

`externalOrderId` is our reference, `FD-YYYYMMDD-XXXXXX`, assigned once and
never changed. We rely on your `(partner, venue, id)` uniqueness, and we also
hold our own record of every push so a redelivered message cannot produce a
second ticket from our side. Two guards, because a double print is a
double-cooked order. Your `201` and your `200` with `duplicate: true` are both
treated as success.

**The gap: you gave us the endpoint and its idempotency semantics, but not the
body schema.** What we send is in the attached document, and it is our proposal
rather than your contract. If your field names differ, tell us and we will
match them — it is one class and one mapper on our side. **This is the only
thing blocking a working end-to-end test.**

## `UNKNOWN_ITEMS` — we took you up on the offer

You said you would rather find a better answer than free-text lines or a bare
refusal, if items created in our panel turned out to be real volume. They are,
and we have built the third option: **we now refuse the basket at checkout**,
before the customer pays, naming the dishes so they can remove them.

You should therefore rarely see `422 UNKNOWN_ITEMS` — and when you do, it means
our catalogues have drifted and we want to hear about it rather than have it
handled quietly.

We agree with your reasoning, for the record: an order the kitchen cannot read
is worse than a refused one, and a paid customer learning their food is not
coming is worse than either.

## Pricing

> **DECISION NEEDED — confirm before sending, unless it was settled in the last
> round.** The technical answer below is verified. Committing to it as policy is
> a commercial choice.

Technically this is closed on our side. A price you send is charged verbatim —
it is written to the field the customer is actually billed from, and there is no
code path that adds to it. Since you publish the same number under both keys,
there is no way for us to apply a margin even by accident.

## What we need from you

In the order it blocks us.

1. **Confirm the order payload schema**, or send us yours. Nothing can be tested
   end to end until the body we POST is the body you parse.
2. **A staging credential**: your base URL, the key, and which header you want
   it in. We support `X-Partner-Key` or `Authorization: Bearer` — your choice,
   it is one configuration field.
3. **Your venue ids** for the restaurants we are switching on first, so we can
   map them to ours. We would like to start with **one venue**, not a rollout.
4. **A production credential**, once staging works.
5. **Two answers still outstanding from our last note**, neither blocking:
   whether a withdrawn item can simply stop appearing in your menu snapshot now
   that we handle absence safely, and whether `ARCHIVED` means retired (we treat
   it as such).

## Still not built

Named so nobody builds against it:

- **Order-status webhooks out to you.** We push orders and you report status
  back; we do not yet push our own status changes to you. If you want them, say
  which states and we will add it.
- **Reading a menu back through this API.** We pull yours; there is no reverse.

## The call

Still worth it, and still for the commercial questions rather than the technical
ones. We said we would bring a position on each — here is what the system does
today, which is not the same as what we think the answer should be:

| Case | What happens now |
|---|---|
| A refund fails after a cancellation | The order still reads cancelled and the failure is logged for manual settlement. A person makes the customer whole. |
| You refuse an order after our customer paid | The order is cancelled and refunded automatically, and the refusal reason is shown to the customer. |
| A venue declines after accepting | Same path — allowed at any point, including mid-cook. The cancellation cutoff binds our customers, not your venues. |
| A customer cancels after cooking starts | Refused with a `422`. This is live, and it is the cutoff at `PREPARING` you proposed. |

The one we would add: **who talks to the customer in each case, and what they
are told.** All four are situations where someone has paid and something has
gone wrong, and the worst version is both of us assuming the other is handling
it.
