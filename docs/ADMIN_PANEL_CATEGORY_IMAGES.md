# Category images — reply to `BACKEND_REQUEST_categories_bucket.md`

**Done: `categories` is a bucket.** Option 1, as you preferred. Switch the one
string.

```
POST /api/v1/images/upload/categories
Authorization: Bearer <token>
Content-Type: multipart/form-data;  field: file

→ { "success": true, "data": {
      "url": "https://zbrr.uz/api/v1/images/categories/3f2a….png",
      "relativePath": "categories/3f2a….png",
      "storedName": "3f2a….png", "originalName": "burgers.png",
      "size": 18422, "contentType": "image/png" } }
```

Then `PATCH /api/v1/admin/restaurant-categories/{id}` with
`{"imageUrl": "<that url>"}`. Roles: `ADMIN`, `PLATFORM`, `RESTAURANT_OWNER`,
`RESTAURANT_STAFF` — the upload endpoint's existing set, unchanged. PNG, JPEG,
GIF or WebP, 5 MB ceiling.

## One correction, and it is the interesting part

> It currently enumerates `restaurants | menu-items | profiles | documents`, and
> rejects anything else — which is correct, and why we did not simply send
> `categories` and hope.

**It enumerated nothing.** The bucket was a `@PathVariable` used directly as a
directory name, checked only for path traversal. Every name was accepted. Had
you sent `categories` and hoped, it would have worked — and so would
`catagories`, which is the problem: a typo created a second drawer, silently,
that no audit would ever look in. The enumeration you described as existing is
the one we have now added, because your reason for wanting it was right.

Worth naming plainly: you designed around a restriction that was not there, and
the workaround you adopted was the more expensive of the two options. That is on
our documentation, not on your reading of it.

Accepted buckets are now exactly:

```
restaurants  menu-items  categories  profiles  documents
```

Anything else is a `400` naming the list:

```json
{ "success": false,
  "message": "Unknown image bucket 'catagories'. Accepted: restaurants, menu-items, categories, profiles, documents" }
```

Three details worth having:

- **Case is forgiving, storage is not split.** `Categories` files into
  `categories/`. The filesystem is case-sensitive, so the alternative was a twin
  drawer — the exact split the list exists to prevent.
- **Only the first segment is policy.** Our own callers compose deeper paths
  (`restaurants/7/logo`, `profiles/42`) and still can. If you want
  `categories/burgers/…` you may.
- **`documents` is in the list although nothing in the backend writes to it.**
  You listed it as accepted and it was, so removing it is a separate decision
  from adding `categories`. Tell us if you use it and for what.

## The icons already in `restaurants`

They still resolve — reads are unaffected, and nothing was moved or deleted. Two
options, your call:

1. Re-upload the nine PNGs to `categories` and `PATCH` the new URLs. Clean, and
   the old files become orphans we can sweep.
2. Leave them. They work. The drawer stays mixed until someone cares.

We would rather (1) while it is nine files and nobody has deep-linked them.

## Your unrelated question: `totalPrice` is built from `effectivePrice`

From the code, not from memory:

```java
// ItemVariant
public BigDecimal calculateTotalPrice() {
    return menuItem.getEffectivePrice().add(priceDelta);
}
```

So `totalPrice` and the order both charge `effectivePrice + priceDelta`, and
your derived number is the same number. An item at `price 30000`, on sale at
`effectivePrice 24000`, with a size at `priceDelta 8000`, reports
`totalPrice: 32000` and charges 32000. **No client following the "use
`totalPrice` for display" advice is wrong**, on-sale items included.

This was answered to the customer app team in `MENU_OPTIONS_REPLY.md` and
evidently did not reach you. Our fault for replying to one audience.

Two things neither of you can see from outside, both still true:

- **No live item is on sale.** `onSale` is false on all 131 items across the
  five menus, so this path has never actually run in production.
- **`originalPrice` is what drives `onSale`, and the Restos import maps the
  partner's *cost* price into it.** A venue sending a cost price above their
  selling price would render as discounted with nothing discounted. No live item
  does today. When the first real sale appears we will look at it together with
  the first real size.
