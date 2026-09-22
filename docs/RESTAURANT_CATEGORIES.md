# Restaurant categories — backend answers

Reply to `BACKEND_RESTAURANT_CATEGORIES.md`. All four items are built; two came
with a decision, and one came with a bug on our side.

**Why your probe found three of them missing: none of this is deployed yet.**
It is committed and tested, not released — so `/restaurants/categories` is
matched by `/{id}` and answers `400`, `category` is absent rather than null, and
both filters are unknown parameters, which Spring ignores. That last part is
worth keeping in mind generally: an unknown query parameter is never an error
here, so a filter that has not shipped looks exactly like a filter that does
nothing. Your decision to re-filter on the client is the right defence and worth
keeping even after this deploys.

---

## 1. Category on the restaurant — nested shape

```json
{
  "id": 3, "name": "Qahvoon", "averagePrepTimeMinutes": 10,
  "category": {
    "id": 3, "slug": "coffee", "name": "Kofe",
    "imageUrl": null, "sortOrder": 60
  }
}
```

On `GET /restaurants`, `/restaurants/{id}`, `/restaurants/slug/{slug}`,
`/restaurants/active`, `/restaurants/search`, `/restaurants/featured` and
`/restaurants/nearby`. One category per restaurant, as you proposed.

`slug` is extra and is the one to key on for anything that must survive a
rename — analytics, a deep link, a hard-coded chip order. Names change; slugs
do not, by design.

**`category` can be null, and you have to handle that.** A restaurant only has
one once an admin files it, and the five that exist today have none — nobody can say what cuisine a venue is without asking it, and a guess
puts a wrong chip on a real business. Render no chip when it is null.

`imageUrl` is null everywhere for now; the categories exist, the artwork does
not. Square 256×256 PNGs to us and we will set them.

## 2. `GET /restaurants/categories` and `?categoryId=`

```http
GET /api/v1/restaurants/categories
Accept-Language: ru
```

```json
{ "success": true, "data": [
  { "id": 1, "slug": "national", "name": "Национальная кухня", "imageUrl": null, "sortOrder": 10 },
  { "id": 6, "slug": "coffee",   "name": "Кофе",               "imageUrl": null, "sortOrder": 60 }
]}
```

Public, no token. **Only categories with at least one open restaurant** — your
dead-end point was right, and it means the rail changes through the day: a
cuisine whose only venue closes at 22:00 stops being offered at 22:00. Sorted by
`sortOrder`, which is ours to set.

The filter is on the three lists that take one:

```
GET /api/v1/restaurants/active?categoryId=3
GET /api/v1/restaurants/search?q=plov&categoryId=1
GET /api/v1/restaurants/nearby?lat=..&lng=..&categoryId=3
```

Applied in the query, not to the page — filtering a page would answer "the
burgers on page 1", which looks exactly like "the burgers" and is not.

## 3. Localisation — resolved server-side, as you preferred

Send `Accept-Language: uz | ru | en` and `name` comes back in that language,
already resolved. No mapping table on your side, and nothing else to change.

We take the first tag we recognise, so `ru-RU`, `uz-Latn-UZ` and
`en-US,en;q=0.9` all work — you send bare codes, but a browser or a curl will
not, and refusing those over punctuation would hand a Russian speaker Uzbek.

**With no header, or one we do not speak, the answer is Uzbek.** The platform
serves Uzbekistan, so an unlabelled request is more likely a local customer than
an English-speaking one. Tell us if you would rather it were Russian — it is one
line.

Names are stored per language (`nameUz`, `nameRu`, `nameEn`), and only Uzbek is
required. A category with one name falls back to it rather than rendering blank,
because an unlabelled chip is worse than one in the wrong language.

## 4. `featured` — half of what I told you was wrong

**Correction.** I said nothing had ever been featured and the carousel had been
empty since launch. You checked, and L'Amoura has `featured: true` in
production — so the carousel does populate, and has.

What is true is narrower: **no API could set the flag.** The create mapper
ignores it and the update request has no field for it, so L'Amoura's flag was
set outside the application, in the data. That is why there was nothing to turn
it on with, and why a second featured restaurant was impossible to add.

There is now a switch:

```http
PATCH /api/v1/restaurants/{id}/featured?featured=true     # admin/platform only
```

And the filter you asked for:

```
GET /api/v1/restaurants/active?featured=true
```

Exact rather than page-limited, and combinable with `categoryId`.
`GET /restaurants/featured` still works and is unchanged.

---

## Administration (ours)

```http
GET    /api/v1/admin/restaurant-categories            # all, including empty ones
POST   /api/v1/admin/restaurant-categories            # nameUz required
PATCH  /api/v1/admin/restaurant-categories/{id}       # partial; slug never changes
PUT    /api/v1/admin/restaurant-categories/assignments/{restaurantId}?categoryId=3
```

Omitting `categoryId` on the assignment clears it, so a restaurant filed wrongly
does not need an invented category to get out of the wrong one.

Nine categories ship seeded, in all three languages: national, fast-food,
burgers, pizza, shashlik, coffee, desserts, sushi, drinks. That is a first
draft — rename, reorder, deactivate or add as you like. **Renaming before
restaurants are assigned is free; after is the expensive conversation you
warned about, which is why the slug exists.**

## `GET /restaurants` — you were right, and it is fixed

It accepted `lat`/`lng` and dropped them. That is why no customer has seen a
distance or an arrival estimate on the home screen while the app was sending
coordinates on every request: `/active` honoured them and `/restaurants` did
not. It now takes `lat`, `lng`, `categoryId` and `Accept-Language`, so the two
stop drifting.

**One thing to know before you switch back to it, though: `/restaurants`
returns every restaurant whatever its status** — including PENDING ones that
have been created but not yet approved. `/active` returns only active and open
venues, which is what a customer list wants. Your move to `/active` in 1.0.2 is
the right one regardless of this fix; a home screen should not be able to show
an unapproved restaurant.

We would rather not silently narrow `/restaurants` to active-only, since we do
not know what else calls it. Say if you want it narrowed and we will.

## What is still on us

- **Deploying it.** Nothing above is live until the next release.
- **Assigning the five live restaurants.** Needs someone who knows what they
  sell, not a guess.
- **Category artwork.** Nine `imageUrl`s waiting for nine PNGs.
- **Deciding who is featured.** The switch exists; the editorial call does not.
