# Reply to Restos — round eleven

**Draft for review.** One answer, given plainly because you asked for it before
staging rather than during.

---

## Yes — `available` works on a size

Send both. `externalVariantId` scopes the partial update to that row, exactly as
you read it, and each field is applied independently:

```json
PATCH /api/v1/partner/venues/{venue}/menu/items/4417
{ "externalVariantId": "11", "price": 40000, "available": false }
```

A Large selling out does not make the Regular unavailable, and there is a test
pinning that specifically rather than incidentally.

**And it cannot be silently ignored.** A size we do not hold answers `404` on
the single-item call and comes back under `unknownItemIds` as `4417/99` in the
bulk one. There is no path where we return success and drop the field — an entry
carrying neither `price` nor `available` is refused as "nothing to change"
rather than counted.

Our example showing `price` alone is what left you guessing, and that was our
documentation being thinner than our API. Fixed: it now shows both fields and
says what happens to a size we do not recognise.

You were right to ask rather than assume. A silent ignore is the one failure
mode here that looks like success from your side, and it would have been
invisible until a customer was offered a Large that sold out an hour earlier.

## The bulk path you found

That one was genuinely yours and we would not have noticed it. A venue-wide
markup moving every size as well as every item, and the bulk push carrying items
only, puts back exactly the staleness the single-item path removes — once per
markup change, on the day a venue reprices its whole menu.

Which is the worst day for it, and the failure would have read as ours: your
prices correct, our catalogue half right, and our `expectedTotal` refusing
orders while both systems believed they agreed.

Sending the item first and then the sizes, rather than relying on our
reordering, is the right instinct. We reorder anyway and will keep doing so —
two guards against a failure of this shape is not one too many.

## The owed ticket

Agreed to the definition, and your point about drift is the useful addition: a
ticket whose price moved between our cache and your menu differs by that drift
rather than by an error. Worth recognising on sight, and worth saying out loud
before the afternoon rather than discovering as an anomaly during it.

On empty-rather-than-zero — you do not have the case and we do, which is the
only reason we met it first. Writing it down because the reasoning is not
obvious until somebody says it is the right call.

## What is left

Staging, and it is yours. Everything else is closed: `withdrawn` stays where it
is, the sequencing is settled, order push stays off until `paymentMode` is real,
and you will hear that day and the app-label day as messages rather than infer
them.

Your last paragraph is the one worth keeping. Three findings in three rounds,
each one correct code on both sides of a boundary nobody owns, none of them
reachable by testing either system alone. That is an argument for the first
order going through with a person watching both ends — and for carrying on
describing our own systems to someone who will read them adversarially, which is
a cheaper review than either of us would have budgeted for.
