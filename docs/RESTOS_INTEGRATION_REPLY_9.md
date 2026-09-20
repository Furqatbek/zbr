# Reply to Restos — round nine

**Draft for review.** One question of theirs is answered with a number we now
record; nothing needs a decision before sending.

---

Both of your notes found something. The size one is the more serious and it was
ours to fix.

## Sizes are addressable

You were right that the hole was between two correct pieces of code, and right
that ours was the half to change. `externalVariantId` is now a field on a menu
update:

```json
PATCH /api/v1/partner/venues/{venue}/menu/items/4417
{ "externalVariantId": "11", "price": 40000 }
```

Its own field rather than an overload of `externalItemId`, because your product
ids and your variant ids are separate namespaces and a collision would reprice
the wrong thing — quietly, and on a dish nobody was looking at.

Two details worth having before you write the adapter line:

**The price is absolute.** We store a size internally as a difference from the
product's price, and we do that conversion. Send what the size costs, as you
publish it.

**Sizes may be mixed into a bulk call.** We apply product changes before size
changes regardless of the order you send them in, so a size is never computed
against a base that is about to move. That is not something you should have to
know, which is why we did it rather than documenting it as a rule.

An unknown size comes back as `4417/99` rather than `4417` — the product is
present and pointing at it would send you looking in the wrong place.

Thank you for making it loud rather than closing it on your side. Writing a
Large's price over the item's would have repriced every Regular on the menu,
and that is a worse day than a stale size.

## `withdrawn` — agreed, and not yet

You are right that we had the mechanism wrong in our write-up: a withdrawal *is*
pushed, as `available: false`, and collapsing the three causes into one flag is
the right call because a customer cannot act on the difference. Corrected.

And we agree about the ordering: if a schema change is being spent on our
behalf, spend it on sizes. The gap survives — a dish pulled at 11am reads *sold
out* until the nightly sync — and it is worth less than the one we just closed.

## The owed ticket now carries a number

Your question is the right one and we did not have an answer: we recorded
**that** a venue was owed for a ticket and not **what it came to**. So our list
and yours could have been compared by row count and by nothing else.

**It is the goods, at the price you published, and nothing else.** No delivery
fee — nobody drove it, so nobody earned it. Not our service fee. No tip.

Which is your definition exactly, and worth stating the way you did: it is
`expectedTotal` **minus** the delivery fee. One word apart in description,
different on every delivery order.

Where an order cannot be read we leave the amount empty rather than zero. A
ticket that looks free gets reconciled; one that is visibly incomplete gets
asked about.

**Yes to the afternoon, ticket by ticket, before the first month closes.** Your
three ways to disagree are the three we would look for, and the second — the
same ticket at different values — is the one this change was needed for.

## Your side of the screen

Your point about who a venue should hear it from is better than ours and we had
not thought it through: a restaurant told by us what we owe them has no way to
check. That is the position this whole correspondence has been getting both
companies out of, and we had rebuilt it without noticing.

## Sequencing — we take yours

Both corrections accepted.

**The boundary and the generalisation are separable**, and only the first earns
being done early. We had argued for both at the second partner; you are right
that a template built from one API is that API with extra indirection in it.
Stages 2 and 3 move to the third partner in our copy.

**And the escape hatch goes into the design now**, not when the awkward partner
arrives. A template that grows a flag per awkward partner ends up less readable
than the classes it replaced — which is the argument for keeping the ability to
write one.

You also ranked *"nothing watches the pipes"* above where we put it, and you are
right that we contradicted ourselves: third by effort, first by sequencing. It
is the only item on either list that changes what happens on the day the first
real order fails. It stays first.
