# Sizes and add-ons — answering your open question

Reply to `BACKEND_MENU_OPTIONS.md`.

---

## `totalPrice` is built from `effectivePrice`. The two agree

Confirmed in the code rather than from memory:

```java
// ItemVariant
public BigDecimal calculateTotalPrice() {
    return menuItem.getEffectivePrice().add(priceDelta);
}
```

So your table's second row cannot happen: an item at `price 30000` on sale at
`effectivePrice 24000` with a `priceDelta` of 8000 reports
`totalPrice: 32000`, which is what the order charges. Display advice and order
formula are the same arithmetic, and deriving it yourself — which you already do
— gives the same number.

Worth adding, since you asked to look at the on-sale case together: **no live
item is currently on sale.** I checked all four menus; `onSale` is false on
every one of 131 items, so this path has never actually been exercised in
production either. When the first one appears it is worth a look at the same
time as the first real size.

One related thing you could not have seen from outside. `originalPrice` is what
drives `onSale` and `discountPercentage`, and the Restos import maps the
partner's **cost price** into it. If a venue ever sends a cost price above their
selling price, an item would render as discounted when nothing was discounted.
No live item does today — I checked that too — but it is the kind of thing that
turns up on someone else's menu, so we are watching it.

## Your two client changes are right, and one of them is on us

Taking the strictest `maxSelections` and requiring a size when a dish has them
matches the server exactly. Good.

The second one deserves a note, though: **when every size of a dish is sold
out, the server now refuses the line** — it asks the customer to choose a size,
and every size it would name is unavailable. That is a dead end we created, and
it belongs to us rather than to your picker. The honest fix is for the dish
itself to be unavailable when all its sizes are, which we will do; until then,
treating "all sizes sold out" as "dish unavailable" in the UI is the right call
and we would rather you kept it.

## Accepted-and-ignored: agreed as a position, with one distinction

You are right, and it is now a rule rather than three fixes. Two changes:

**Request bodies we act on refuse unknown fields.** `SaveItemVariantRequest`,
`SaveItemOptionRequest`, `SaveRestaurantCategoryRequest` and
`CreateMenuItemRequest` now answer `400` naming the field:

```json
{ "success": false,
  "message": "Unknown field 'prise' in the request body. Check the spelling, or the endpoint documentation for what this accepts." }
```

Including a case mismatch — `maxselections` would previously have been dropped
and the cap silently left at 1.

**A body we cannot read is a 400, not a 500.** Malformed JSON, or a string where
a number belongs, fell through to the catch-all and answered *"An unexpected
error occurred"* with a 500 — telling a client its own bad request was our
fault, and burying the reason in a server log.

**The distinction we are keeping**, because it is not a compromise but the
point: **inbound partner payloads stay lenient.** Restos adds fields to their
menu JSON when they feel like it, and an import that refuses an unknown column
breaks on their release schedule rather than ours. Leniency is right for reading
someone else's feed, wrong for a body we act on.

**And one endpoint we have deliberately not tightened yet: order creation.** A
`400` at checkout because a shipped client sends one extra field is worse than
anything it would catch. Tell us `CreateOrderRequest` and `OrderItemRequest`
carry no fields beyond the documented ones and we will tighten them in the same
release.

## Query parameters: worth separating the three cases

Of your three instances, only one was this bug:

| | What actually happened |
|---|---|
| `?categoryId=` | Shipped in code, **not deployed** when you probed |
| `?featured=true` | Same |
| `PUT /items` dropping `variants` | A real accepted-and-ignored bug, now refused |

Spring ignores unknown query parameters and there is no per-endpoint switch for
it. We could add an interceptor rejecting any parameter a handler does not
declare — but during a rollout that turns "filter not deployed yet" from a wider
list into a **broken screen**, which is worse for a customer than the thing it
prevents. Your client-side re-filter is the better trade while versions can
differ, and we would rather you kept it for filters whose absence changes what
the customer sees.

What we can promise instead: a filter parameter ships honoured or not at all,
and we will tell you which release carries it rather than letting you discover
it by probing.

## Still not exercised against real data

Agreed, and unchanged: every live item returns `variants: []` and `options: []`.
Restaurants can create them now. When the first venue adds a size, we will set
one deliberately sold out and one item on sale so both edge cases get looked at
together before a customer finds them.
