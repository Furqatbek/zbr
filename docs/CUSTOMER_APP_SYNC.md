# For the customer app team: what went live, and what to change

**Everything below is deployed on `https://zbrr.uz` now** — verified against the
live API on 2026-09-23, not written from the branch. Base URL is the bare apex,
never `www.`

Four of these change what a customer sees. One of them changes what they pay.

---

## 1. Prices dropped. Nothing in the app caused it

Two markups were being added on the backend and both are gone:

- **10% on every menu item.** A venue typed 35 000, the customer was charged
  38 500, and the venue's own screen kept showing 35 000. It applied to items
  created in the vendor app and to items imported from a POS.
- **8% on the order**, shown to customers as *Tax*. It was never a tax — a US
  sales-tax default that arrived with the project scaffolding.

Both are now zero, and the 15 items already carrying the markup were corrected
in the database. **Two menus are about 9% cheaper than yesterday**; the venues
have been told.

What this means for you:

- `price` and `effectivePrice` are now **equal on every live item**. Keep
  rendering `effectivePrice` — that is still the charged price, and a POS
  partner may legitimately set it away from `price` later.
- `serviceFee` (and its deprecated `tax` alias) now come back as **`0.00`** on
  new orders. **Hide the line when the amount is zero.** Do not delete the
  field — the rate is configuration and can come back without an app release.
- Old orders keep what they were charged. History is not rewritten, so an order
  from last week still shows its 8%.

## 2. Distance and arrival time

Send the customer's location as `lat`/`lng` on any restaurant read and you get:

```json
{ "id": 3, "name": "Qahvoon", "averagePrepTimeMinutes": 10,
  "distanceKm": 2.4, "etaMinutesMin": 30, "etaMinutesMax": 50 }
```

All three are **null** when the request carries no location — and also when the
restaurant has no coordinates on file. **Two of the five live restaurants have
none today** (Jangirov's and Burgeria), so this is not a rare edge case: show
nothing rather than a zero.

`etaMinutesMin/Max` is kitchen time plus courier time, already rounded to five
minutes. Show the range — "30–50 min" — not a single number.

**Keep using `/restaurants/active` for the home list.** `/restaurants` now
honours `lat`/`lng` too (it silently ignored them before, which is why no
customer had ever seen a distance), but it returns every restaurant *including
PENDING ones that have not been approved*. `/active` is active-and-open only.

## 3. Cuisine categories

```http
GET /api/v1/restaurants/categories      (public, no token)
Accept-Language: ru
```

Live and returning `{"success": true, "data": []}` — **empty until we file each
restaurant under a cuisine**, which is on us. Nine categories exist; none has a
restaurant yet, and the endpoint deliberately hides categories with no open
restaurant so a chip can never filter to an empty list.

So today: the chip rail stays hidden, and `category` is `null` on every
restaurant. Both are the states you already handle. Nothing to wait for — ship
it, and the rail appears by itself when we assign them.

`?categoryId=` works on `/active`, `/search` and `/nearby`. Filtering happens in
the query, so a filtered list is the whole list, not the first page of one.

`Accept-Language: uz | ru | en` resolves the category name server-side. Region
tags and weighted lists are understood. No header means Uzbek.

## 4. Featured

`GET /api/v1/restaurants/active?featured=true` — exact rather than
page-limited, and combinable with `categoryId`. `GET /restaurants/featured` is
unchanged and still works. One restaurant is currently featured.

## 5. The app version check is answering

```
GET /api/v1/app/version?platform=android
{"success":true,"data":{"latestVersion":"1.0.1","minimumVersion":"1.0.0",
 "storeUrl":"https://play.google.com/store/apps/details?id=app.zbr.customer"}}
```

Seeded so nobody is prompted. `platform` is required. **iOS has no `storeUrl`
yet** — the field is omitted and your built-in link is used; send us the App
Store URL and it is one config change.

---

## What to change, in order

1. **Hide a zero `serviceFee`/`tax` line.** Highest priority — customers will
   otherwise see "Service fee: 0 so'm" at checkout from today.
2. **Send `lat`/`lng`** on `/restaurants/active`, `/search`, `/nearby` and the
   restaurant detail read, and render `distanceKm` + the ETA range when they
   come back non-null.
3. **Handle `category: null` and an empty categories list** — no chip, no rail.
4. **Send `Accept-Language`** on every request (you already do once i18n has
   initialised).
5. **Stop deriving arrival time on the device** if anything still adds a
   constant to `averagePrepTimeMinutes`.

## What to verify once you have updated

```bash
curl -s "https://zbrr.uz/api/v1/restaurants/active?lat=41.31&lng=61.09&size=3" | jq '.data.content[] | {name, distanceKm, etaMinutesMin, etaMinutesMax, category}'
curl -s "https://zbrr.uz/api/v1/restaurants/categories" -H 'Accept-Language: ru' | jq
curl -s "https://zbrr.uz/api/v1/restaurants/1/menu" | jq '[.data[].items[] | {name, price, effectivePrice}] | .[0:3]'
```

The last one should show `price == effectivePrice` on every item. If it ever
does not, tell us — that is the markup coming back and we want to know the same
day.

## Still on us, not on you

- **Coordinates for Jangirov's and Burgeria.** Until then they have no distance
  and no ETA, while the other three do.
- **Filing the five restaurants under cuisines**, and nine category images
  (256×256 PNG).
- **No delivery-radius filtering on the browse lists.** A customer far from a
  venue currently sees a real but useless estimate rather than "too far" — all
  five venues are in one town today, so it has not bitten anyone yet.
