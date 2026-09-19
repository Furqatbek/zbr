# Reply to Restos — round four

**Draft for review. DO NOT SEND until the pricing question below is settled
internally — there is a discrepancy, and the honest version of this reply
depends on how it is resolved.** Everything else is verified against shipped
code as of 2026-09-19.

---

## 1. Staging credential

Ready when you are. Send the venue id and we will create the partner record, a
key stamped `staging`, and a grant carrying both capabilities on that one venue.

Two things to expect:

- **The secret is shown once.** We store only a digest and cannot recover it, so
  if it goes missing we issue a new key and revoke the old one.
- **`pushOrders` starts off**, even on the grant. Reading and writing your menu
  is one decision; sending a restaurant's live orders to your kitchen is
  another. We will switch it on for that venue once the payload schema below is
  agreed and we have your outbound details.

## 2. Pricing — and a discrepancy we owe you

You asked for the rule in writing rather than a description of current
behaviour. That was the right thing to insist on, because writing it down has
surfaced something we need to tell you about.

**The line price is exactly as you send it.** A price you publish is written to
the field the customer is billed from, and there is no code path that marks it
up. That part is unconditional and it is what we have built on.

**But our order total is not the sum of those lines.** We add an 8% tax line at
checkout, on top of the food, before delivery and tip. So a customer ordering a
30 000 dish sees 30 000 for the dish and pays 32 400 for the food component.

By your framing — "your customer pays more than we published" — that is exactly
the thing you were asking about, and we would rather you heard it from us than
found it in a reconciliation. It is under review here. We will come back with
either a commitment that the published price is the total price for the food, or
the precise rule and what the 8% is.

> **INTERNAL — resolve before sending.** The 8% is a hard-coded constant in
> OrderService, not configurable, applied to every order on the platform and
> labelled `tax` to the customer. Whether it is a real tax we remit, a margin, or
> a leftover default is a question for the business, not for engineering. The
> paragraph above is written to be true under any of those answers; replace it
> with the actual rule once known.

## 3. `expectedTotal` — thank you, and fixed

Your note is the first we knew that you validate a total against your own
computation, and it would have failed every order we sent. We have added the
field, and it is deliberately **not** our order total:

```json
{ "expectedTotal": 60000, "subtotal": 60000, "deliveryFee": 15000, "total": 75000 }
```

`expectedTotal` is the sum of the line totals at your prices — what you should
expect to charge for the food, and the only number reconstructible from the menu
you published. `total` is what the customer pays us, all in, for the ticket to
display. Confirm we have read your semantics right.

This is the second time your questions have caught something on our side before
it cost anyone an order. It is appreciated.

## 4. Order push is built

Your note says our document has it as still ours to build — that was the version
we sent you last week. It is done: orders POST to your
`/api/v1/partner/orders` when a venue is switched on, asynchronously and off our
checkout path, keyed on `FD-YYYYMMDD-XXXXXX` with our own duplicate record as a
second guard against a double ticket. A retryable failure is retried; a refusal
is recorded and escalated rather than retried into the ground.

So the critical path is not blocked on us building it. It is blocked on **one
schema confirmation**: you gave us the endpoint and its idempotency semantics
but never the body, so what we POST is our proposal. The updated document has
it in full. If your field names differ, tell us and we will match them — it is
one class and one mapper.

Once that is agreed and we have your staging outbound details, an end-to-end
test is a same-day thing rather than a project.

## 5. What we need from you

1. **The venue id** for the first venue, so we can create the grant.
2. **Confirmation of the order payload schema**, or yours in place of ours.
   This is the whole critical path.
3. **Your staging outbound details** — base URL, the key you issue us, and
   which header you want it in. `X-Partner-Key` or `Authorization: Bearer`,
   your choice; it is one configuration field here.
4. **Production equivalents**, once staging works end to end.

Two from earlier rounds, still open and still not blocking: whether a withdrawn
item can simply stop appearing in your menu snapshot, and whether `ARCHIVED`
means retired.
