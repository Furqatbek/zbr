# QR campaign — what the backend now provides

Two pieces shipped together: promo codes that actually reduce what a customer
pays, and one public endpoint the landing page calls.

---

## 1. Promo codes were accepted and ignored

Before this change, a code did nothing to an invoice:

- `POST /orders/validate-promo` answered *"valid, you save 5 000"*.
- The app sent `discountCode` with the order.
- `OrderService` never read it. `Order.discount` was never set.
- **The customer paid full price.**

`discountCode` has been on `CreateOrderRequest` since promo codes existed, and
`Order.discount` has been subtracted in `calculateTotals()` the whole time with
nothing to subtract. A campaign printed on a thousand takeaway bags, pointing at
a code that silently does not apply, would have been worse than no campaign.

**Now:** a code sent with an order is validated, applied, and recorded.

```http
POST /api/v1/orders
{ "restaurantId": 3, "orderType": "DELIVERY", "items": [...],
  "discountCode": "QAHVOON" }
```

The response carries `discount` and the new `promoCode` field, and `total` is
lower by that amount. A code that cannot be applied fails the order with a
message written for the customer — *"You have already used this promo code"*,
*"This promo code has expired"*, *"This promo code is not valid for this
restaurant"* — rather than quietly charging them.

**Two rules that could not be enforced before:**

- **Per customer.** `promo_codes.user_usage_limit` has existed since day one and
  was unenforceable: `usage_count` is one global number with no record of *who*
  spent a use. "First order free" without this is free forever, for everyone.
  `promo_code_usages` now records each use; the limit defaults to 1.
- **The last use goes to one order.** The global count is taken with a single
  conditional `UPDATE`, so two simultaneous checkouts cannot both spend it.
  Read-then-increment gives a campaign budget away twice under load.

**Two seeded codes were retired.** `WELCOME10` and `SAVE5` came from the project
template denominated in dollars — `SAVE5` takes 5.00 off an order over 25.00, and
`WELCOME10` caps at 20.00. In so'm those are nonsense, and until now it did not
matter because no code was ever applied. From this deploy on they would be, so
V54 deactivates them.

### What is still missing for "first delivery free"

**A discount applies to the subtotal, not to the delivery fee.** `DiscountType`
is `PERCENTAGE` or `FIXED`, both computed on the food. There is no way to
express "the delivery is on us", and a `FIXED` code cannot stand in for it
because the fee is distance-based and therefore different for every customer.

So the campaign as written needs one more change: a `FREE_DELIVERY` discount
type that zeroes `deliveryFee` instead of reducing the subtotal. It is a small
change and it was outside what we agreed to ship first, so it is named here
rather than smuggled in. **Until it exists, a "first delivery free" code cannot
be created** — the closest available is a fixed so'm discount, which is a
different promise.

### Creating a code per restaurant

There is no admin API for promo codes yet — another gap worth naming. For now
they are inserted directly:

```sql
INSERT INTO promo_codes
  (code, description, discount_type, discount_value, restaurant_id,
   usage_limit, user_usage_limit, starts_at, expires_at, is_active)
VALUES
  ('QAHVOON', 'QR poster — Qahvoon', 'FIXED', 10000.00, 3,
   500, 1, NOW(), NOW() + INTERVAL '3 months', TRUE);
```

`restaurant_id` is what makes the code answer "which venue brought this
customer": every use lands in `promo_code_usages` with the order and the
customer, so

```sql
SELECT p.code, COUNT(*) AS orders, COUNT(DISTINCT u.user_id) AS customers,
       SUM(u.discount_amount) AS given_away
FROM promo_code_usages u JOIN promo_codes p ON p.id = u.promo_code_id
GROUP BY p.code ORDER BY 2 DESC;
```

answers the question the campaign is for.

## 2. The landing endpoint

```http
GET /api/v1/public/r/{slugOrId}?lat=41.3113&lng=61.0867
Accept-Language: uz | ru | en
```

Public, no token. One call returns the restaurant and its whole menu:

```json
{ "success": true, "data": {
    "restaurant": { "id": 3, "name": "Qahvoon", "slug": "qahvoon",
                    "logoUrl": "...", "coverImageUrl": "...", "address": "...",
                    "rating": 4.6, "deliveryFee": 5000, "minimumOrder": 10000,
                    "isOpen": true, "distanceKm": 2.4, "etaMinutes": 28 },
    "menu": [ { "id": 11, "name": "Coffee", "items": [ { "id": 4417, "name": "Americano",
                "price": 15000, "effectivePrice": 15000, "orderable": true,
                "imageUrl": "...", "variants": [], "options": [] } ] } ] } }
```

One request rather than two, because the page is on another host and making a
call, reading an id out of it and making a second one means the customer watches
a spinner twice.

**`{slugOrId}` accepts either.** `qahvoon` is what you want on a poster — it
survives the venue being renamed and reads as something on a sticker — but a
numeric id works, so a code can be printed before anyone agrees a slug. A slug
that happens to be all digits also resolves.

`lat`/`lng` are optional and add `distanceKm` and the arrival estimate, through
the same enrichment the app's own cards use, so the web page and the app do not
quote different times for the same venue.

Cached for 60 seconds, publicly — a poster gets scanned in bursts.

### Two things to check before the posters are printed

**Slugs.** The URL only reads well if every restaurant has one. Codes are
generated at creation, but venues seeded before that may be null:

```sql
SELECT id, name, slug FROM restaurants ORDER BY id;
```

Anything null there needs a slug set before its QR code is designed.

**CORS.** `app.zbrr.uz` is a browser on a different origin calling `zbrr.uz`. It
is in the default allow-list now, but **if `CORS_ORIGINS` is set explicitly in
`.env` it overrides that default** and the landing page will render nothing with
an error in a console nobody is watching:

```bash
grep CORS_ORIGINS .env    # must include https://app.zbrr.uz, or be unset
```

## 3. What the backend does not do

Worth being plain, because the plan assumes some of it:

- **No deferred deep link.** After a fresh install the operating system does not
  carry the restaurant through, so the customer lands on the app's home screen,
  not that menu. Only a vendor SDK (Branch, AppsFlyer, Adjust) does this
  reliably. The printed promo code is what ties the customer back to the venue
  in the meantime — which is exactly why the code has to work, and now does.
- **No Universal Links / App Links yet.** Opening the app directly when it is
  already installed needs `apple-app-site-association` and `assetlinks.json`
  served from the domain, plus the app teams registering it. Ask and we will
  serve both files; the app side is theirs.
- **No scan tracking.** The landing page is on Vercel, so scans are counted
  there. The backend counts *conversions* — promo uses — which is the number
  that matters for rewarding a restaurant.
- **No admin API for promo codes.** SQL for now, as above.
