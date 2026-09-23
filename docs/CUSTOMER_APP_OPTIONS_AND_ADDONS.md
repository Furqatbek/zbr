# For the customer app team: sizes and add-ons

**All of it works now.** Reading them, ordering with them, and — as of this
change — creating and editing them from the vendor app. The picker will stay
empty until restaurants add something, but nothing is blocking them any more.

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

## The rules the server now enforces

Validate in the UI for a decent experience, but these are checked on the server
too, and an order that breaks one is refused with a message written to be shown
to the customer:

| Rule | Message |
|---|---|
| A dish with sizes needs one chosen | *Choose a size for 'Lavash': Regular, Large* |
| A `required` group needs an answer | *Choose 'Sauce' for 'Lavash'* |
| `maxSelections` per group | *Choose at most 2 from 'Extras' for 'Lavash'* |
| Sold-out size or add-on | *'Large' is sold out for 'Lavash'* |
| Withdrawn size or add-on | *That size is no longer available for 'Lavash'* |
| The same add-on twice | *The same add-on was chosen twice for 'Lavash'* |

Two of those matter even with a correct UI: a menu the app fetched an hour ago
can name a size that has since sold out, and an id can always be replayed.
Surface the message rather than a generic failure — it tells the customer what
to change.

Where `required` and `maxSelections` disagree across options in one group (they
are stored per option but describe the group), the server takes the strictest
value it finds.

## Managing them (vendor app / admin panel)

```http
POST   /api/v1/restaurants/{rid}/menu/items/{itemId}/variants
PUT    /api/v1/restaurants/{rid}/menu/items/{itemId}/variants/{variantId}
DELETE /api/v1/restaurants/{rid}/menu/items/{itemId}/variants/{variantId}

POST   /api/v1/restaurants/{rid}/menu/items/{itemId}/options
PUT    /api/v1/restaurants/{rid}/menu/items/{itemId}/options/{optionId}
DELETE /api/v1/restaurants/{rid}/menu/items/{itemId}/options/{optionId}
```

Restaurant owner, staff, platform or admin.

```json
POST .../variants   { "name": "Large", "priceDelta": 8000 }
POST .../options    { "groupName": "Extras", "name": "Cheese", "priceDelta": 5000,
                      "maxSelections": 3, "required": false }
```

`PUT` is partial — send `{"inStock": false}` to mark one sold out for the
evening and nothing else changes. Defaults on create: in stock, active,
`priceDelta` 0, optional, `maxSelections` 1, and a sort order after whatever is
already there.

`DELETE` really deletes. Past orders keep the name and the price they were
charged, so nothing is lost; to hide a size you may want back, send
`{"active": false}` instead.

**`PUT /menu/items/{itemId}` now refuses a body containing `variants` or
`options`** and names these endpoints instead. It used to accept them and drop
them silently — the same "accepted and ignored" shape you flagged on
`categoryId` — which from the outside is indistinguishable from a request that
worked.
