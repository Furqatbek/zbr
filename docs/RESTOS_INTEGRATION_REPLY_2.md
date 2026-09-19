# Reply to Restos — round two

**Draft for review.** One item is marked DECISION NEEDED and must be confirmed
before sending. Everything else is verified against shipped code as of
2026-09-19.

---

Thank you — that is a more complete answer than we gave you, and you were right
about where the work sits. Two of the things you raised are now fixed on our
side, and the rest of this is about the gap that remains: the partner API you
have nothing to call.

## Two things we changed because of your reply

**The cancellation cutoff is implemented, not just agreed.** A customer can now
cancel up to `ACCEPTED` and no further; from `PREPARING` the request is refused
with a `422` and a message telling them to contact support. It binds the
customer and not the business — a kitchen fire or a spoiled delivery is still
cancellable mid-cook by the venue or by us — and the test is the caller's
relationship to that order rather than the roles on their account, so someone
holding a staff role cannot cancel their own half-cooked dinner for a full
refund.

You were right to push on it. As you read it, a customer could cancel with a
full refund right up to the moment the courier lifted the bag and nothing told
the restaurant it was happening. That was true, it was ours to fix, and your
venues would have found it before we did. It is the same cutoff you enforce, so
there is no special case on either side.

**Our import now has a deletion path**, which changes one of your design
decisions — see below.

## The five things you need from us

**1. A partner API to call.** It does not exist yet. That is the honest answer
and the only one that matters, since everything else on your list waits on it.
We are not going to quote you a date in a document; the shape is settled and
[NAME] will confirm timing separately.

For what it is worth, the order we intend to build it in follows what unblocks
you fastest:

- Partner principal and key scheme, with per-venue grants — nothing else can be
  authenticated without it.
- Item price and availability as a partial update, addressed by **your** ids.
  This is the one that stops an imported menu going stale, so it comes first
  among the endpoints.
- Order-status webhook receiver, taking your states.
- Partner rate limits and a bulk path, per what we both proposed.

**2. Credentials, per environment.** They will come with the key scheme in (1).
Your assumption is right: the key we hold today is read-only against your public
endpoints.

**3. Our idempotency semantics.** Today `POST /orders` takes an
`Idempotency-Key` header; a replay returns the original order rather than
creating a second, backed by a unique index for two simultaneous requests
carrying the same key. For the partner API it will be scoped per partner and
venue, as yours is, and we will build against the scoped version from the start
rather than migrating later.

We will also adopt your `duplicate: true` with `200` rather than `201`. Being
able to tell a replay from a fresh create is worth more than the symmetry of
always returning the same code, and our own clients can use it too.

**4. Price.**

> **DECISION NEEDED — confirm before sending.** The technical answer below is
> verified and favourable. Committing to it as policy is a commercial choice,
> because it means no markup on Restos venues. Confirm or supply the rule.

Technically this is already settled and your design settles it twice over. Our
importer reads `priceWithMargin` when it is present and charges exactly that —
it becomes the unit price on the order line. Since you send the same number
under both keys, there is no path by which we add anything on top. The customer
pays the number you publish.

The one thing worth you knowing: items a restaurant creates directly in our own
panel, rather than importing from you, do carry a platform margin. That is our
catalogue, not yours, and it has no effect on anything you send — but if a venue
runs a mixed menu, two dishes on the same screen may be priced under different
rules. Which brings us to your `UNKNOWN_ITEMS` question.

**5. Which of your states we care about.** Three: **accepted**, **declined**,
and **ready**. Those are the ones that change what our customer sees or what our
courier does. We do not need your internal kitchen or station transitions, and
we would rather not receive them — a state we ignore is a state we might start
depending on by accident.

## Two corrections

**Withdrawal as `available: false` is solving a problem we no longer have.** You
built that because we told you our import had no deletion path. It does now: a
sync treats your menu endpoint's response as authoritative and deactivates what
is no longer in it, softly, so order history survives and an item you restore
comes back on the next sync.

We would rather a withdrawn dish simply stopped appearing in the payload. As it
stands, a dish taken off the menu permanently sits on ours forever showing "sold
out" — nothing breaks, but the menu silts up with dishes that are never coming
back, and the customer is told to check again later for something that will
never return.

We are not asking you to change this unilaterally. If `available: false` is
load-bearing elsewhere, keep it and we will live with it. But if withdrawal can
be silence, our side is now built for it.

Three things about our deletion pass are worth you knowing either way, because
they affect what you can expect to happen:

- If any part of a sync fails, it updates what it got and deactivates **nothing**
  — we cannot tell an item you deleted from one your response happened not to
  include.
- A sync that would retire an implausible share of a venue's menu at once
  reports what it would have done and changes nothing, on the assumption that a
  menu collapsing from two hundred dishes to three is an outage rather than a
  decision.
- A product you mark `ARCHIVED` is treated as retired rather than ignored. Tell
  us if that is not what you intend.

Your confirmation that the menu endpoint returns a complete, unpaginated
snapshot is exactly what makes all of this safe. Thank you for stating it
plainly.

**`UNKNOWN_ITEMS` is a real volume problem, not a hypothetical.** You offered to
find a better answer if items created in our panel turn out to matter. They do:
our vendor app lets a restaurant add and edit menu items directly, and nothing
stops a Restos venue from using it. So a mixed catalogue is not an edge case —
it is what happens the first time a venue adds a lunch special without going
back to the till.

We agree with your reasoning: an order the kitchen cannot read is worse than a
refused one, and free-text lines are not the answer. We would like to take you
up on finding a third option. The two that seem worth discussing:

- The order is **split** — your items go to the kitchen, ours do not — which is
  honest but means a customer receives part of their order from a process the
  venue cannot see.
- We **refuse the basket at checkout** rather than at push time, so the customer
  is told before they pay rather than after. This needs us to know, at menu
  time, which items you would accept — which your menu already tells us.

We think the second is better and cheaper, and it is mostly work on our side.

## Where we agree, briefly

Your auth shape, the rate limits, the bulk path, the nightly full re-sync, not
building against our staging OpenAPI, and keying your retries on
`FD-YYYYMMDD-XXXXXX` — all agreed, nothing to add.

On ids: agreed, and we will hold to it. The partner API will be addressed by
your identifiers throughout, and we will not ask you to store ours.

## The call

Agreed, and agreed that it should be short — and you are right that the two
questions we proposed are already answered. We pull for the initial import, you
push changes after that, and the auth is the key-plus-venue-grant shape we both
arrived at.

Your third question is the one worth the hour, and we would add a fourth:

1. A refund that fails on our side after a cancellation.
2. An order you refuse after our customer has paid.
3. A venue that declines after accepting.
4. **Who talks to the customer in each of those**, and what they are told. All
   three are situations where the customer has paid and something has gone
   wrong, and the worst version is both of us assuming the other is handling it.

We should come to that call with a position on each rather than discover them
live. We will send ours beforehand.
