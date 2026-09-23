# For the customer app team: sizes and add-ons

**Short answer: reading them and ordering with them works today. Creating them
from the vendor app does not** — see the last section, which is ours to fix.

So you can build the picker now; it will stay empty until restaurants have
something to put in it.

---

## What comes back on a menu item

`GET /api/v1/restaurants/{restaurantId}/menu` and
`GET /api/v1/restaurants/{restaurantId}/menu/items/{itemId}` both carry two
arrays on every item:

```json
{
  "id": 4417, "name": "Lavash", "price": 30000, "effectivePrice": 30000,
  "inStock": true,
  "variants": [
    { "id": 11, "name": "Regular", "priceDelta": 0,    "totalPrice": 30000, "inStock": true,  "sortOrder": 1, "active": true },
    { "id": 12, "name": "Large",   "priceDelta": 8000, "totalPrice": 38000, "inStock": false, "sortOrder": 2, "active": true }
  ],
  "options": [
    { "id": 51, "groupName": "Sauce",   "name": "Garlic",  "priceDelta": 0,    "isDefault": true,  "required": true,  "maxSelections": 1, "inStock": true, "sortOrder": 1, "active": true },
    { "id": 52, "groupName": "Sauce",   "name": "Spicy",   "priceDelta": 0,    "isDefault": false, "required": true,  "maxSelections": 1, "inStock": true, "sortOrder": 2, "active": true },
    { "id": 60, "groupName": "Extras",  "name": "Cheese",  "priceDelta": 5000, "isDefault": false, "required": false, "maxSelections": 3, "inStock": true, "sortOrder": 1, "active": true }
  ]
}
```

**Variants are one-of** — a size. `priceDelta` is a *difference from the item's
price*, not an absolute, and `totalPrice` is that sum already computed for you:
use it for display and you cannot get the arithmetic wrong.

**Options are add-ons, grouped by `groupName`.** Group the UI by that string.
Within a group, `required` and `maxSelections` describe the rule — `required:
true, maxSelections: 1` is a radio group, `required: false, maxSelections: 3` is
a checkbox group with a cap of three.

`isDefault` marks what to preselect. `inStock: false` on a variant or option
means that one choice is sold out while the dish itself is not — show it
disabled rather than hiding it, so the customer understands why their usual
order looks different.

Both arrays are `[]` when an item has none, never null. Today **every live item
has both empty**, because nothing has been able to create them yet.

## How to order with them

```http
POST /api/v1/orders
Authorization: Bearer <token>
Idempotency-Key: <uuid>

{
  "restaurantId": 3,
  "orderType": "DELIVERY",
  "deliveryAddress": "Mustaqillik 15, kv 42",
  "deliveryLatitude": 41.3113, "deliveryLongitude": 61.0867,
  "items": [
    {
      "menuItemId": 4417,
      "quantity": 2,
      "variantId": 12,
      "optionIds": [52, 60],
      "specialInstructions": "no onions"
    }
  ]
}
```

`variantId` is a single id or omitted. `optionIds` is a flat array across *all*
groups — the backend does not care which group each came from.

**The price the customer pays for that line:**

```
(effectivePrice + variantPriceDelta + sum of option priceDeltas) × quantity
(30000 + 8000 + 0 + 5000) × 2 = 86000
```

Compute the same way for the basket total you display, and it will match the
order the server creates. The order response echoes back `variantName` and the
chosen modifiers, so the confirmation screen can show what was picked without
re-deriving it.

## Two rules the backend does NOT enforce — so you must

The server checks that each `variantId`/`optionId` belongs to that menu item and
nothing more. It does **not** check:

1. **`required` groups.** An order with no sauce chosen is accepted. If your UI
   lets it through, the kitchen gets an ambiguous ticket.
2. **`maxSelections`.** Five extras from a group capped at three is accepted and
   charged.

It also does not check `inStock` on a variant or option — a sold-out size can
be ordered if the client sends its id.

We would rather the server enforced all three, and it should; until it does,
the app is the only thing standing between a customer and a ticket the kitchen
cannot cook. Validate before you submit.

## What is missing on our side

**There is no endpoint to add a size or an add-on to an item that already
exists.** They can only be supplied nested inside the item at creation:

```http
POST /api/v1/restaurants/{restaurantId}/menu/items
{ "name": "Lavash", "price": 30000, "categoryId": 6,
  "variants": [ { "name": "Large", "priceDelta": 8000 } ],
  "options":  [ { "groupName": "Extras", "name": "Cheese", "priceDelta": 5000 } ] }
```

`PUT /menu/items/{itemId}` accepts `variants` and `options` in the body and
**silently ignores them** — the same "accepted and ignored" shape you flagged on
`categoryId`, and worth knowing before someone tests against it and concludes
the feature works.

So today a restaurant can only get add-ons by deleting an item and recreating
it. That is the gap to close before any of this reaches a customer, and it is on
us, not on you.
