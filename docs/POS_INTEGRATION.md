# POS integration: how it works, and how it scales

Restos is the restaurant's system. ZBR is the customer's. Neither replaces the
other, and the restaurant keeps working in the till it already uses.

---

## Part 1 — What the integration is

Four one-way pipes, each authenticated with an API key the receiving side
issued, sent in `X-Partner-Key`.

| # | Direction | What moves | How |
|---|---|---|---|
| 1 | POS → ZBR | The menu | We pull `GET /partner/menu/{venue}` and build our catalogue from it |
| 2 | POS → ZBR | Price and availability, as they change | They push to `PATCH /api/v1/partner/venues/{venue}/menu/items/{id}`, or in bulk — for a product or for one size |
| 3 | ZBR → POS | A customer's order | We `POST /partner/orders`; it becomes a real order and the kitchen ticket prints |
| 4 | Both ways | Order status | They send kitchen states (accepted, preparing, ready, declined); we send courier states (assigned, picked up, in transit, delivered, completed, cancelled) — and kitchen states too, when a restaurant set them on our tablet rather than the till |

**Pipe 2 carries price and availability**, for a product or for one of its
sizes. A withdrawal is pushed too, as `available: false` — the POS collapses
"ingredient short", "manager flipped the switch" and "item withdrawn" into that
one flag on purpose, because a customer cannot act on the difference. What the
flag cannot say is *permanently*, which is the known gap below.

### The rules that make it work

**The POS owns the price, we own the customer.** They publish a price with the
venue's channel markup already in it. We charge that number unchanged and add
our service fee as a separate line on the customer's bill. The venue is paid on
the published price. We never mark it up.

**Every order is checked against the POS menu — twice.** They refuse an unknown
dish, a missing size, or a total that disagrees with theirs. We run the same
checks at checkout, so the customer hears about it *before* paying rather than
after. Their refusal is the backstop; ours is the one a customer should ever
meet.

**The cancellation cutoff is `PREPARING`.** A customer cancels free until the
kitchen starts. After that the POS refuses, and both sides record the order as a
ticket the venue is owed for.

**Either system can accept an order, and they stay in step.** A restaurant may
work from the till or from our vendor app. Whichever sets a state, the other
hears about it — neither side echoes back a change the other reported, so there
is no loop.

**Access is per venue, and order push is off by default.** Reading a menu and
sending live orders into a kitchen are two separate decisions. A menu that is
wrong is fixable in an afternoon; an order sent to the wrong kitchen is
somebody's dinner.

### Where it stands

All four pipes are built and tested. **None are live.** Restos have no
credentials from us yet because their staging environment is not deployed, and
order push stays off on both sides until our `paymentMode` field stops being a
constant.

---

## Part 2 — What does not scale today

The partner *model* is already generic: partners, keys, per-venue grants and
capabilities are all data, and a second POS needs no new tables. Three things
underneath it are not.

**The menu import is Restos-only.** `RestosMenuImportService` carries
`EXTERNAL_SOURCE = "RESTOS"` as a compile-time constant, so the whole import
pipeline — upsert, variant handling, deletion, the safety limits — works for
exactly one partner. Everything in it is partner-agnostic in substance and
partner-specific by that one line.

**The wire formats are hard-coded.** `RestosMenuClient` knows their paths,
`OutboundOrder` is shaped to their order schema, and
`PartnerOrderPushClient` builds `/api/v1/partner/orders` by string
concatenation. A second POS with a different URL shape or body means a second
client and a second DTO, not configuration.

**Nothing watches the pipes.** A partner whose orders stopped flowing is
discovered by a restaurant phoning to ask where their tickets went. The data
exists — `partner_order_pushes` records every attempt, its status and its error
— but nothing reads it.

Two smaller ones:

- **Onboarding is a shell script and three `curl` calls.** Fine for the first
  partner, wrong for the tenth, and a step someone will skip.
- **Known gap: a withdrawal shows as "sold out" until the next sync.** A dish
  taken off permanently at 11am reads *sold out* until tonight, because
  `available: false` is the only word the message has. Harmless once; irritating
  across a hundred venues. Not worth a `withdrawn` flag yet — Restos would
  rather we spent a schema change on sizes, and we agree.

---

## Part 3 — The scalable shape

One idea: **put an adapter boundary where the POS-specific knowledge lives, and
leave everything else alone.** The platform already treats partners as data; the
work is making the wire format data too.

### A `PosAdapter` per partner

```
interface PosAdapter {
    String partnerCode();                       // stamps external_source
    List<Category> fetchMenu(Venue venue);      // their menu, our shape
    PushResult pushOrder(Venue venue, Order o); // our order, their body
    PushResult reportStatus(Venue v, ...);      // our states, their vocabulary
}
```

Everything that is genuinely ours stays where it is and stops being duplicated:
the upsert, the variant mapping, the deletion safety limits, the idempotency
record, the retryable-versus-permanent split, the checkout guard. Those took the
longest to get right and none of them are Restos-specific.

**A second POS then costs one class and a row**, not a fork of the import
pipeline. Restos becomes the first implementation rather than the only shape.

The one change with teeth: `RestosMenuImportService` loses its constant and
takes the partner code from the adapter. Mechanical, but it touches the sync's
safety rules, so it wants the existing tests green rather than a rewrite.

### A partner health view

One screen, reading rows we already write:

- last successful menu sync per venue, and what it changed
- orders pushed, delivered, failed, rejected — last hour and last day
- anything sitting in `FAILED` beyond a few minutes
- tickets recorded as **owed** to a venue, which is money nobody has settled

The first three turn "a restaurant phoned" into an alert. The fourth is the one
that pays for the screen: those rows accumulate quietly and are currently
visible only to somebody running SQL.

### Onboarding as a screen

The admin endpoints exist; they have no UI. A form that registers a partner,
issues a key, maps venues and shows the three switches would remove the
`curl` step and — more usefully — make the *order-push switch* something a
person chooses deliberately on a screen that explains what it does.

### A withdrawal signal on pipe 2

Ask partners for an explicit `withdrawn` flag, or accept `available: false` plus
absence from the next pull as we do now. Worth raising with Restos rather than
building alone: they chose the current behaviour deliberately and may prefer it.

### Sequencing

| | Why |
|---|---|
| 1. Partner health view | Reads existing data, no schema change, and it is the only item here that changes what happens on the day the first real order fails |
| 2. Adapter *boundary* | Do it at the second partner, while there is one implementation. Retrofitting after two means migrating both |
| 3. Generalising the boundary into a profile | Wait for the **third** partner. Two data points are not a pattern, and a template built from one API is that API with extra indirection |
| 4. Onboarding screen | Partner three or venue twenty |

**Do 1 before Restos goes live.**

Two refinements from Restos, both of which we take over our own reasoning. The
*boundary* and the *generalisation* are separable and worth separating — ours
argued for doing both early, and only the first of them earns that. And keep the
ability to write a class for an awkward partner: some will sign requests, or
page oddly, or want the menu as one document, and a template that grows a flag
per awkward partner ends up less readable than the classes it replaced.
