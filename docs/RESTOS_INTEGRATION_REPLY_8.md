# Reply to Restos — round eight

**Draft for review.** Short, and nothing in it needs a decision.

---

One line in your document was not true when you wrote it. It is now.

## "Either system can accept; they stay in step"

Your caveat on the ownership table:

> a restaurant working from ZBR's own tablet can accept there, and the
> acceptance travels back to the till.

**It did not travel.** We reported six states — courier assigned, picked up, in
transit, delivered, completed, cancelled — and excluded `ACCEPTED`, `PREPARING`
and `READY` outright. There was a test of ours asserting exactly that, named
*"kitchen states are not sent back to the kitchen"*.

The reasoning was half right. Those states usually come from you, and echoing
one back is telling a kitchen what it just did. But a restaurant can work from
our vendor app instead of the till, and in that case you never heard: your
screen would show an order still waiting while ours showed it cooking, and the
two would never converge. Mid-service, on a real order.

You had already built for it — *"we accept `ACCEPTED`, `PREPARING` and `READY`
too, for when a restaurant works from your tablet instead of ours"*. We simply
never sent them.

**Fixed, and the rule is now origin rather than state**, which is the rule you
stated first: *we never send a change back to the partner who reported it*. A
status change now carries the system that caused it. A kitchen state you
reported goes nowhere. The same state set on our tablet is sent. Matched by
partner rather than by "came from some partner", so a venue on two systems still
has both kept in step.

It also closed an echo that already existed and we had not noticed: when you
declined an order, the resulting cancellation was sent straight back to you.
Harmless — your endpoint is idempotent and answered `200` — but it was a message
describing your own decision to the system that made it.

## Two notes on your document

**Your Stage 1 is already done on our side.** Base URL, auth header and the key
we present you are columns on the partner row, set from an admin call rather
than an environment variable. So the bottleneck you describe — *"onboarding a
partner means an environment change and a deploy"* — is not one we have.

Your observation underneath it is the useful half and it does land on us: your
key to call us is hashed, ours to call you is readable, and we stored the
readable one in a plain column. You wrote the warning about your own future
work — *"wants a real encryptor and a rotation path, not a column called
`api_key`"* — and it describes our present. It is admin-only, never returned by
any endpoint and never logged, but a database dump is a live credential to your
production system. On the list, and named for what it is.

**Your "what not to build" list is better than ours.** Particularly *do not make
the cutoff, the price rule or the refusal configurable per partner*. We had not
put it that way and we should have: a venue whose protection varies by which
aggregator sent the order is a venue that cannot be told one sentence about how
it is paid.

We also take your sequencing point over our own — that a template built from one
API is that API with extra indirection, and two data points are not a pattern.

## Unchanged

Everything else stands. Waiting on the venue id, the base URL and the key
whenever your environment is up; order push stays off until `paymentMode` is
real; we will tell you the day it flips and the day the app label ships, both as
messages rather than something to infer.
