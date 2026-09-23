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

> **Update after your reply.** `documents` has been dropped — see below. The
> accepted buckets are `restaurants`, `menu-items`, `categories`, `profiles`.
> The rest of this document stands, except the sentence about our documentation
> being the cause, which was wrong and is corrected at the end.

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
restaurants  menu-items  categories  profiles
```

Anything else is a `400` naming the list:

```json
{ "success": false,
  "message": "Unknown image bucket 'catagories'. Accepted: restaurants, menu-items, categories, profiles" }
```

Three details worth having:

- **Case is forgiving, storage is not split.** `Categories` files into
  `categories/`. The filesystem is case-sensitive, so the alternative was a twin
  drawer — the exact split the list exists to prevent.
- **Only the first segment is policy.** Our own callers compose deeper paths
  (`restaurants/7/logo`, `profiles/42`) and still can. If you want
  `categories/burgers/…` you may.
- **`documents` was in the list and now is not** — you confirmed nothing on your
  side uses it, and nothing on ours does either. See round two.

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

---

# Round two — replying to `BACKEND_REPLY_category_images.md`

## You were right to hand that back

> That is on our documentation, not on your reading of it.

That sentence was wrong, and it was wrong in exactly the way you are describing.
We saw a stale section in `restaurant-portal-api.md`, assumed it was where your
belief came from, and wrote the assumption down as a cause. We inferred it. You
observed the actual source — a union in your own client — and it was not us.

So the rule you are proposing applies in both directions, and we will mark ours
too. The doc rewrite stands on its own: it described a `type` form field and a
`thumbnailUrl` that have never existed, so it needed fixing regardless of
whether anyone had been misled by it.

## `documents` is gone

Dropped, on your confirmation plus ours. Existing files under it still serve —
removal only refuses new uploads.

**It turned up something while we checked.** The feature it was presumably for
is `Courier.documentsSubmitted`, and that flag **is never set by any code
path** — no endpoint, no service, nothing. Which means:

```java
// CourierRepository
@Query("SELECT c FROM Courier c JOIN FETCH c.user "
     + "WHERE c.verified = false AND c.documentsSubmitted = true")
Page<Courier> findPendingVerification(Pageable pageable);
```

**The admin courier-verification queue can never return a row**, unless the
column was set directly in the database. If the panel shows that queue, it has
been showing an empty list that means "nothing to review" and actually means
"this filter cannot match". Same shape as the markup: a screen that is wrong in
a way that looks like normal operation.

That is a courier-onboarding gap rather than an images one — couriers have
nowhere to submit a document and no field to store one — and we are raising it
separately rather than fixing it inside a bucket change. Re-adding the string
when it ships is the whole of the work on our side.

## The icons in `restaurants` — you are right that we can settle it

We can, and you should have the command rather than our summary of it:

```bash
ssh root@<zbr-host>
docker exec zbr-app ls -la /app/images/restaurants/ | head -40
```

Files sitting **directly** in `restaurants/` are category icons or strays:
every real restaurant image is written to `restaurants/{id}/{logo|cover}/`, so
the two are distinguishable at a glance. If that listing shows only numbered
directories, there is nothing to sweep and nothing to re-upload.

## Your client bug is worth one note back

`storedName` / `originalName` — glad the documented shape caught it. The field
you want is `url`, always present and absolute. `relativePath` is the one to
keep if you ever need to call `DELETE /images/{relativePath}`; `storedName`
alone is not enough for that, since the path includes the bucket.

## `onSale` / `originalPrice`

Your warning line is the right instrument and it is better placed than ours —
you will see a divergence on a real menu before we would. Send the item id and
we will trace it back to what Restos sent for that product.
