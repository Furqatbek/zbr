# Reply to Restos — round seven

**Draft for review.** One outstanding item is disclosed rather than hidden: the
customer apps still display the line as "Tax" until they ship. Nothing else
needs a decision before sending.

---

You asked which of the three the 8% was. It was the third one, and we can now
tell you exactly how we know.

## It was a leftover default

The figure arrived in the platform's **first commit** — the initial import, 559
files in one go, under a commit message about notification broadcasts. It has
never been modified since. Not once, by anyone.

Three things confirm it rather than merely suggest it:

**Nothing in the platform ever read it.** Outside the order row itself, the only
references were the constant that set it, the line that added it to the bill,
and the field that showed it to the customer. No liability was computed from it,
no report contained it, nothing remitted it anywhere. A tax has an authority at
the other end; this had nobody.

**It matched no Uzbek tax.** VAT here is 12% and is included in the displayed
shelf price rather than added at checkout. This was 8%, added on top, and
charged on the food alone — the delivery service, which a real VAT would cover,
was untaxed.

**Our own API documentation still had the scaffolding in it.** One of our order
examples reads `subtotal: 25.00, tax: 2.25, deliveryFee: 3.99` for a restaurant
called "Pizza Palace". Dollars, and a US sales-tax rate, in a document describing
a platform that has only ever operated in so'm.

So: a US sales-tax default that came in with a code template, was never
questioned, and has been on every customer's receipt since.

## What we have done about it

**Renamed to what it is** — a platform service fee — in the database, the API
and the documentation. The column was renamed rather than zeroed and re-added,
so historical orders keep the amount they were actually charged. Those orders
were the same fee under the same misnomer; only the name has changed.

**Made the rate configurable.** It defaults to the 0.08 that has always been
charged, so nothing moved when we deployed it. If the commercial conversation
changes the number, it changes without a release.

**Put it in our financial reporting**, where it had never appeared. That is the
part that embarrassed us most: total revenue was computed as commission plus
delivery margin, so we had been **understating our own earnings by roughly 8% of
GMV since the platform started**. The fee was invisible in both directions at
once — mislabelled to the customer and missing from our own accounts — which is
a fair explanation of how it survived this long, and not much of an excuse.

## The sentence you asked for

Here it is, as policy rather than as a description of current behaviour:

> **The dish is priced at what you publish, and the venue is paid on that
> price.** Our service fee is a separate line, charged by us to the customer and
> itemised as ours. It is a marketplace fee, not a markup on your price, and it
> does not change what the venue receives or what the dish is listed at.

That is the sentence your restaurants can ask you to repeat.

## One thing still wrong, and you should hear it from us

**Our customer apps still display this line as "Tax".** The backend no longer
calls it that anywhere, but the word is a string inside three shipped mobile
apps, and it changes when they next release rather than when we deploy.

By your own argument that is the half that actually matters: the receipt carries
the venue's name, and a customer doing the arithmetic reaches the restaurant
before they reach us. We are not going to tell you it is fixed while a customer
can still read the wrong word.

It is queued with the app teams. We will tell you when it ships, the same way we
will tell you the day `paymentMode` becomes real — as a message, not as
something for you to infer.

## Your markup editor

You built it showing the chain as base price, published price, and what the
diner pays "once your figure is applied", labelled as our number told to you
because you could neither price nor verify it.

You can label it now: **ZBR service fee, 8% of the food subtotal**, and we will
tell you if that number moves. It is no longer ours-according-to-you.

## Unchanged

Everything from our last note stands: `expectedTotal` is your goods plus the
delivery fee, order push stays off until `paymentMode` is real, a cancellation
you refuse is recorded here as food the venue is owed for, and we are waiting on
the venue id and your outbound details whenever your environment is up.

The three commercial questions stay on the call. This one is off it.
