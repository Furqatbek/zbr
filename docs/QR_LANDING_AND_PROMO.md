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
- **No Universal Links / App Links yet, and they are not ours to serve.**
  Opening the app directly when it is already installed needs
  `apple-app-site-association` and `assetlinks.json` at `/.well-known/` on **the
  domain in the link** — `app.zbrr.uz`, which is Vercel. An earlier version of
  this document said the backend could serve them; that was written assuming the
  link lived on `zbrr.uz`, and iOS and Android fetch those files from the link's
  own host, so us serving them would do nothing. The landing page hosts them, the
  app teams supply what goes in them (iOS: Team ID + bundle id; Android: package
  name + release signing SHA-256). If the link ever moves to `zbrr.uz`, it
  becomes ours and we will add it.
- **No scan tracking.** The landing page is on Vercel, so scans are counted
  there. The backend counts *conversions* — promo uses — which is the number
  that matters for rewarding a restaurant.
- **No admin API for promo codes.** SQL for now, as above.

---

# Free delivery

Three ways a delivery becomes free, one mechanism underneath.

## 1. `FREE_DELIVERY` promo codes

A third `DiscountType`, alongside `PERCENTAGE` and `FIXED`:

```sql
INSERT INTO promo_codes
  (code, description, discount_type, discount_value, restaurant_id,
   usage_limit, user_usage_limit, starts_at, expires_at, is_active)
VALUES
  ('QAHVOON', 'QR poster — first delivery free', 'FREE_DELIVERY', 0, 3,
   500, 1, NOW(), NOW() + INTERVAL '3 months', TRUE);
```

`discount_value` is ignored — the discount *is* the fee, whatever it comes to
for that customer. This is exactly why a `FIXED` code could not stand in: the
fee is distance-based, so a flat amount short-changes someone far away and
overpays someone next door. Set `max_discount_amount` to say "free delivery up
to 10 000".

## 2. Every new customer, automatically

One credit is granted at registration. It is spent on the first delivery order
that has a fee — no code to remember and nothing for the customer to do.

Granted at registration rather than worked out at checkout from "has this
person ordered before", because an order placed and cancelled would otherwise
consume a benefit the customer never received.

## 3. Referrals — both sides

`completeReferral` existed with **no caller** and a body ending in
`// In production: Credit rewards to user wallets/accounts`. Every referral has
been sitting at `USED` forever and nobody has ever been rewarded for bringing
anybody.

It now pays out, and it now has a caller: the referred customer's order reaching
`DELIVERED`.

- **The referrer** gets a free delivery, one per successful referral.
- **The referred customer** already has theirs — the welcome credit every
  customer gets. Granting a second one for having arrived through a code would
  hand one person two free deliveries for one arrival.

That is the "no double" rule, and it is the answer to the obvious question: a
customer who arrives through a referral gets *one* free delivery, the same as a
customer who arrives on their own. The referrer's is the extra one, and it is
what the referral is for.

Paid on delivery rather than on order, so an order placed and cancelled cannot
mint a reward.

## How the rules are actually enforced

In the database, not in a method:

```sql
CREATE UNIQUE INDEX uq_delivery_credits_welcome
    ON delivery_credits (user_id) WHERE reason = 'WELCOME';

CREATE UNIQUE INDEX uq_delivery_credits_referral
    ON delivery_credits (source_referral_id) WHERE source_referral_id IS NOT NULL;
```

A service check is something a second code path can forget to call. A partial
unique index is not. Both grants catch the violation and carry on, so a retried
registration or a redelivered `DELIVERED` event is harmless rather than
expensive — and `completeReferral` runs twice by design, once for `DELIVERED`
and again for `COMPLETED`.

**One order cannot take two free deliveries.** If a `FREE_DELIVERY` code has
already covered the fee, no credit is spent — it stays for the customer's next
order.

**A credit is never spent on an order with no delivery fee.** Pickup, or a venue
that does not charge, keeps the credit rather than burning it on nothing.

## What it does to the money

The credit discounts what the **customer** pays. It does **not** zero the
delivery fee.

```
deliveryFee 8 000, discount 8 000, total = food + service fee
```

The courier is paid `deliveryFee + tip`, so zeroing the fee would fund the
marketing out of the courier's pocket. The order keeps its fee, carries an equal
discount, and the platform absorbs the cost. It also makes the invoice readable
after the fact.

## Nothing was granted to existing accounts

Customers registering from this deploy on get a welcome credit. The people
already registered do not — handing free deliveries to an existing list is a
decision about money, and a migration should not make it quietly. To grant it to
everyone who has registered but never ordered:

```sql
INSERT INTO delivery_credits (user_id, reason)
SELECT u.id, 'WELCOME' FROM users u
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.consumer_id = u.id)
  AND NOT EXISTS (SELECT 1 FROM delivery_credits c
                  WHERE c.user_id = u.id AND c.reason = 'WELCOME');
```

## Watching it

```sql
SELECT reason,
       COUNT(*) FILTER (WHERE used_at IS NULL) AS outstanding,
       COUNT(*) FILTER (WHERE used_at IS NOT NULL) AS spent,
       COALESCE(SUM(amount), 0) AS cost_so_far
FROM delivery_credits GROUP BY reason;
```

`outstanding` is a liability — free deliveries promised and not yet taken.

## Still open

- **The app shows nothing about it.** There is no field on any response saying
  "your delivery is free" before checkout, so today the customer discovers it
  when the total is lower than expected. Pleasant, but a benefit nobody knows
  about does not bring anyone back. The obvious addition is `freeDeliveryAvailable`
  on the customer's profile or the fee quote — tell us where you want it.
- **No expiry is set.** The column exists and nothing fills it, so a credit
  lasts forever. That is the safe default; a campaign may want 30 days.
