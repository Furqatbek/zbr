# Landing page API — `app.zbrr.uz/r/{restaurant}`

For the team building the QR landing page. One endpoint, no authentication.

---

## The call

```http
GET https://zbrr.uz/api/v1/public/r/{slugOrId}
Accept-Language: uz | ru | en          # optional, defaults to uz
```

Optional `?lat=41.3113&lng=61.0867` adds distance and an arrival estimate. Only
send them if you have the visitor's location; nothing breaks without them.

`{slugOrId}` is whatever is printed on the poster. **Use the slug** —
`/r/qahvoon` survives the venue being renamed in our database and reads as
something on a sticker. A numeric id also works, so a code can go to print
before anyone agrees a slug.

**No token, no key, no CORS problem** — `https://app.zbrr.uz` is on the allow
list. Any other origin will be blocked by the browser, so test from the real
host (or from `curl`, which ignores CORS).

## The response

Everything is wrapped in the platform envelope. **Read `data`**, not the root:

```json
{
  "success": true,
  "message": null,
  "timestamp": "2026-09-27T14:02:11.221",
  "data": {
    "restaurant": { ... },
    "menu": [ ... ]
  }
}
```

### `data.restaurant`

The fields you are likely to render, with their real names:

| Field | Type | Notes |
|---|---|---|
| `id`, `name`, `slug` | | |
| `description` | string | the venue's own blurb |
| `logoUrl`, `coverImageUrl` | string | absolute URLs, use as-is |
| `fullAddress` | string | ready to display; `addressLine1`/`city` are the parts |
| `phone` | string | |
| `averageRating` | number | e.g. `4.6`. **`totalRatings`** is the count |
| `minimumOrder`, `deliveryFee` | number | so'm — see **Money** below |
| `averagePrepTimeMinutes` | number | kitchen time only, no delivery |
| `isCurrentlyOpen` | boolean | whether it is taking orders now — **the only open flag you get** |
| `opensAt`, `closesAt` | `"09:00:00"` | local wall clock |
| `acceptsDelivery`, `acceptsTakeaway`, `acceptsDineIn` | boolean | |
| `category` | object or absent | cuisine: `{ id, slug, name, imageUrl }` |
| `distanceKm` | number | **only when you send lat/lng.** Kilometres |
| `etaMinutesMin`, `etaMinutesMax` | number | a range, same condition |
| `latitude`, `longitude` | number | may be absent — two venues have none |

### `data.menu`

An array of categories, each with its items:

```json
[ { "id": 11, "name": "Kofe", "description": null, "imageUrl": null,
    "sortOrder": 1,
    "items": [
      { "id": 4417, "name": "Americano", "description": "...",
        "price": 15000.00, "effectivePrice": 15000.00,
        "onSale": false, "discountPercentage": null,
        "imageUrl": "https://zbrr.uz/api/v1/images/menu-items/abc.png",
        "orderable": true, "inStock": true,
        "prepTimeMinutes": 5, "vegetarian": false, "spicy": false,
        "variants": [], "options": [] } ] } ]
```

**Price: show `effectivePrice`, not `price`.** `price` is the menu price;
`effectivePrice` is what the customer is actually charged. They are equal unless
the item is on sale, and then `price` is the higher, struck-through one.
`onSale` and `discountPercentage` tell you whether to render that.

**`orderable`, not `inStock`,** if you grey anything out. `inStock` is the
venue's switch for the dish; `orderable` is that *and* at least one size being
available. Today no live item has sizes, so they agree — they will not always.

`variants` (sizes) and `options` (add-ons) are `[]` on every live item right
now. You can ignore them for a menu preview; they are documented in
`CUSTOMER_APP_OPTIONS_AND_ADDONS.md` if you ever render prices per size.

**`menu` can be `[]`.** A venue with no menu loaded yet is a real state, not an
error. Design for it.

## Conventions that will bite you

**Money is a JSON number with decimals** — `15000.00`, not `15000` and not
`"15000"`. Decimal so'm, no minor units, no tiyin. Format with a thousands
separator and no decimals: `15 000 so'm`.

**`isOpen` is documented in our OpenAPI schema and never sent.** The mapper
ignores it, so it is always absent. If you find it in a schema browser, it is
`isCurrentlyOpen` you want — a venue is open when the owner has switched it on
*and* the clock is inside opening hours.

**A null field is absent from the JSON entirely.** Not `null`, not `""` — the
key is missing. This applies to every optional field above, so use optional
chaining throughout rather than checking for `null`.

**Timestamps have no timezone marker.** `createdAt` looks like
`2026-09-27T14:02:11.221` with no `Z` and no offset. The values *are* UTC — our
servers run in UTC — but nothing in the string says so, so parse them as UTC
explicitly or you will be five hours out.

## Errors

| Situation | Response |
|---|---|
| No such slug or id | `404`, `{ "success": false, "message": "Restaurant not found with slug: 'x'" }` |
| Anything else | `4xx`/`5xx` with the same envelope; `message` is displayable |

A 404 on a printed poster is the case worth designing for — a venue could be
removed while stickers are still on bags. Show something better than a blank
page: the app's store links and a "find restaurants near you" route.

## Caching

The response carries `Cache-Control: max-age=60, public`. A poster gets scanned
in bursts — a table of six, a queue at a till — so let the browser and any CDN
honour it. The menu behind it changes a few times a day at most.

## What the backend does not give you

Worth knowing before you build around it:

- **No deferred deep link.** After someone installs the app from your page, the
  operating system does **not** carry the restaurant through — they land on the
  app's home screen, not this menu. That is a platform limitation, not something
  we can fix server-side. The printed promo code next to the QR is what ties the
  customer back to this venue, so give it room in the design.
- **The app does not open automatically** if it is already installed. That needs
  Universal Links / App Links, which we have not set up yet. Today your page is
  what the customer sees either way.
- **We do not count scans.** Your page is the only thing that sees them, so page
  analytics are the source of truth for "how many people scanned". The backend
  counts *conversions* — orders placed with that venue's promo code.

## Asking for more

If the design needs something not in the response — opening hours for the week,
a popular-items list, a review snippet — ask rather than deriving it. Most of it
exists and is one field away.
