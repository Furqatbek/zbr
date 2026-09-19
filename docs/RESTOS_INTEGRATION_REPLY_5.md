# Reply to Restos — round five

**Draft for review.** One paragraph is marked DECISION NEEDED and has to be
settled before sending. It is a disclosure rather than a negotiation, so the
reply cannot go without it — but what it commits us to is yours to decide.

Supersedes the round-four draft, which was never sent.

---

Your schema arrived as a passing test rather than a document, and it caught two
things on our side that a document would not have. Both are fixed. One of them
was costing your venues money, and we owe you a plain account of it.

## Your schema is implemented

We send your body now, not ours: `restaurantId`, nested `customer` and
`delivery`, `items[].specialInstructions`, `expectedTotal`. Ids go as numbers
since yours are numeric — thank you for saying the coercion was there, and for
not making it a condition.

We kept the fields you ignore (`name`, `unitPrice`, `lineTotal`, `subtotal`,
`deliveryFee`). They cost nothing and they make a ticket legible to a human
reading it.

**One thing to confirm on `expectedTotal`.** We have implemented your mapping as
written — it carries our `total`. But ours includes delivery, a tip and a tax
line, none of which appear in the menu you published, so if your check is against
what your own prices add up to it will never match. Say the word and we will send
the food-only figure instead; it is one line.

## `paymentMode` — sent, and a warning attached

Added, and always sent. You were right to refuse to guess it.

**Today every order will say `PREPAID`.** There was nothing on our order to
answer your question with: we track how far a payment has got, not what kind to
expect, and at the moment we push an order there may be no payment record at all.
So we have added the field, defaulted it to `PREPAID`, and our apps are being
changed to set it explicitly.

Until they do, treat `PREPAID` from us as "probably, not certainly". We would
rather tell you that than have a venue hand over food on our word and collect
nothing. We will confirm when the apps send it for real.

## Variants — the answer is worse than "flattened"

You asked how our import flattened them. **It didn't flatten them, it dropped
them.** Our importer read a `hasVariants` flag and never looked at the array. So
every dish you sell by size has been in our catalogue as a single item at the
base price, with no size for a customer to choose.

Which means your `422 VARIANT_REQUIRED` was going to fire on every one of those
orders — and we are glad it would have. Your reasoning is exactly right: a Large
billed as a Regular, cooked Large, with nothing on the ticket to show it, is a
worse outcome than a refused order, and our `expectedTotal` would have agreed
with itself all the way down.

Now: variants import with your ids kept, your price converted to the delta our
model stores, and a size you stop selling is deactivated rather than deleted.
Orders carry your `variantId`. And we refuse a basket missing a required size at
checkout, the same way we refuse an unknown dish — so you should not see that
`422` from us either.

## We were importing your counter prices

This is the one we owe you an account of.

Your note that the partner menu endpoint carries channel prices rather than
counter prices sent us to look. **Every import we have ever run read
`/api/v1/customer/public/...`** — the endpoints we were using before this
contract existed. So every Restos dish on our platform has been priced at your
counter price, without the markup your venues set to cover our commission, and
your venues have been absorbing the difference on every order since.

We have moved the import to the partner endpoint. It warns loudly when it falls
back to the public one, because "which prices are these" turned out to be a
question nobody could answer from the outside, which is how this survived as long
as it did.

Two consequences worth naming:

- **Existing items still carry counter prices** until a sync runs with the
  partner credential in place. The first thing we do with your staging
  credential is prove that end to end.
- **Variants only reach us through that endpoint**, so the two fixes land
  together rather than separately.

## Status webhooks — built, and sending your list

`COURIER_ASSIGNED`, `PICKED_UP`, `IN_TRANSIT`, `DELIVERED`, `COMPLETED`,
`CANCELLED`, with the reason attached and `occurredAt`, to
`/api/v1/partner/orders/{ref}/status?restaurantId={venue}`.

We do not send the kitchen states back to the kitchen, and we do not send
`REFUNDED` — your reasoning about it marking a delivered order cancelled is
right, and it is the same reason we do not accept it from you. We also do not
report on an order that never reached your till: you would refuse it, and that
refusal would bury the real failure under a second one.

Retries behave as yours do, and a `409` from you is treated as a disagreement
for a person rather than something to send again.

## Pricing

> **DECISION NEEDED — the disclosure is not optional; the commitment is.**
> Resolve internally before sending. The paragraph below is true whatever is
> decided; the final sentence is what needs an answer.

You asked for the rule in writing rather than a description of behaviour. That
was right to insist on, because writing it down surfaced something we should
tell you plainly.

**The line price is exactly as you send it.** A price you publish is written to
the field the customer is billed from, and there is no code path that marks it
up. Unconditional, and it is what we have built on.

**But our order total is not the sum of those lines.** We add an 8% tax line at
checkout, above the food and below delivery. A customer ordering a 30 000 dish
sees 30 000 for the dish and pays 32 400 for the food component.

By the test you set — whether your customer pays more than you published — that
is the thing you were asking about, and you should hear it from us rather than
find it in a reconciliation. It is under review here. We will come back with
either a commitment that the published price is the total price for the food, or
the precise rule and what the 8% is. You will have the one sentence your
restaurants will ask you to repeat; we would rather it were a sentence that
survives being repeated.

## The two you closed

**`ARCHIVED`** — correction accepted, and thank you for correcting the premise
rather than answering the question. Our importer does branch on it, and you are
right that it is reading something you do not send. We have left the branch in
place because it is the correct handling for a partner who does mark products
retired, and documented that it is dormant for you. It cannot retire a menu of
yours, because it never fires.

**Withdrawal** — settled. Absence is safe on our side now, and we are glad you
want to keep sending `available: false` as well. Your reasoning is better than
ours was: the sync is the thing that eventually removes it, the message is what
stops it being ordered in the hours before that runs.

## Staging

Ready. Send the venue id and we will create the partner record, a key stamped
`staging`, and a grant on that one venue with both capabilities. `X-Partner-Key`
on both sides, which we had both landed on independently.

Order push stays switched off on that grant until we have your outbound details
and one order has gone end to end. Reading and writing your menu is one decision;
sending a restaurant's live orders to your kitchen is another.

## The call

**We agree with your division, and we would not have proposed it as cleanly.**
You talk to the customer, we talk to the venue. We hold the payment and we are
the app on their phone; a restaurant should never be explaining our refund
policy. And it follows that the decline reason has to travel and be shown
verbatim, which we already do — "we have run out of lamb" is a sentence a
customer accepts, and you are right that "the restaurant cancelled your order" is
one they argue with.

Your first row is the one we had not thought through. A refund fails, we log it
for manual settlement, the venue has already given the food away, and nobody
tells them. You are right that it should reach them — not as something to act on,
but so the week's number is explainable. That is a small piece of work on our
side and we would rather build it than argue about it.

Your shortlist is ours:

1. **Failed refunds.** Who tells the venue, and do they bear it?
2. **Mid-cook declines.** Both sides allow them; someone pays for the food, and
   today that is silently the restaurant.
3. **Chargebacks and disputes**, which neither document mentions and both of us
   will meet.

Agreed that none of the three blocks the integration, and agreed that they are
the ones your restaurants will ask about first.
