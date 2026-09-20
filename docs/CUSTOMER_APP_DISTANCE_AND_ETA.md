# For the customer app team: distance and arrival time

**What we need:** send the customer's location on restaurant reads, and show
the two new numbers that come back. Then stop computing arrival time on the
device.

Until now the only timing number in the app was the restaurant's average
preparation time, which answers "how long does this kitchen take" and not the
question the customer is asking, which is "when do I eat". Nothing said whether
the food was coming from the next street or from across the river.

---

## What the backend now returns

### Restaurant cards and detail

`lat` and `lng` are **optional** query parameters on every customer-facing
restaurant read. Send them and the response carries three new fields; omit them
and the response is byte-for-byte what it was before, so nothing breaks in the
app that has not shipped yet.

```
GET /api/v1/restaurants/nearby?lat=41.326&lng=69.280&radius=10
GET /api/v1/restaurants/active?lat=41.326&lng=69.280&page=0&size=20
GET /api/v1/restaurants/search?q=plov&lat=41.326&lng=69.280
GET /api/v1/restaurants/featured?lat=41.326&lng=69.280
GET /api/v1/restaurants/{id}?lat=41.326&lng=69.280
GET /api/v1/restaurants/slug/{slug}?lat=41.326&lng=69.280
```

The envelope is unchanged — `{ "success": true, "data": ... }` — and inside it
the restaurant now reads:

```json
{
  "id": 100,
  "name": "Plov Centre",
  "averagePrepTimeMinutes": 25,
  "distanceKm": 2.4,
  "etaMinutesMin": 30,
  "etaMinutesMax": 50
}
```

- **`distanceKm`** — restaurant to customer. See the caveat below; it is an
  approximation of road distance, not a measured route.
- **`etaMinutesMin` / `etaMinutesMax`** — minutes from now until the food is at
  the door. Kitchen time plus courier time, already rounded to five minutes.

All three are **null** when the request carried no location, and null when the
restaurant has no coordinates on file. Null means *show nothing there* — never
zero kilometres, never "0 min".

### Checkout

`POST /api/v1/orders/calculate-delivery-fee` — the call you already make for the
fee — now also returns the estimate, computed from the same distance the fee was
charged from, so the two can never disagree on the screen:

```json
{
  "deliveryFee": 15000, "distanceKm": 2.4,
  "prepMinutes": 25, "travelMinutes": 15,
  "etaMinutesMin": 30, "etaMinutesMax": 50
}
```

This one asks the road router rather than approximating, so it is the better
number — expect it to differ slightly from the card the customer tapped.

### An order in flight

`estimatedDeliveryTime` on the order is no longer the kitchen's guess plus a
flat fifteen minutes. It is now distance and vehicle, and it is **recomputed**
when a courier accepts, when a replacement courier accepts, and at pickup. It
moves during the order's life, and it should be re-read rather than latched at
order time.

---

## What the app should do

1. **Ask for location, and degrade well without it.** If the customer declines,
   fall back to the coordinates of their selected delivery address; if there is
   no address either, send no `lat`/`lng` and show preparation time as today.
   The request must not fail because the phone said no.

2. **Show the range, not a point.** "30–50 min" is honest; "37 min" is not, and
   invites the customer to watch a clock we cannot beat.

3. **Stop deriving arrival time on the device.** If anything in the app adds a
   constant to `averagePrepTimeMinutes` today, delete it — it will drift from
   what the order screen shows the moment a courier is assigned.

4. **Takeaway and pickup orders should not show a delivery ETA.** The courier
   leg does not exist for them; the preparation time is the whole answer.

5. **Do not draw `distanceKm` as a route.** It is not a path anyone drives; see
   below.

6. **Re-read `estimatedDeliveryTime` on the tracking screen** rather than
   counting down from a value captured at checkout.

7. **Localise the units.** "km" and "min" need uz/ru forms; the backend sends
   numbers only and has no opinion about the words.

---

## Things you need to decide (app side)

| Question | Why it is yours |
|---|---|
| Distance on the card, or only the ETA? | Both fit, but the card is small. The ETA is the useful one; the distance is the one people trust. |
| Sort or filter by distance in the UI? | The backend does not sort by distance today — see the list below. If you sort what is on the page, the customer sees "nearest on page", which is not "nearest". |
| What to show when the restaurant is outside its delivery radius? | We return the restaurant with a distance and an ETA regardless. Whether that is a greyed-out card or a hidden one is a product decision. |
| Whether the range collapses when it is short | "10–15 min" reads oddly to some; "about 15 min" may be better copy at the low end. |

---

## What we are still fixing or deciding on our side

Listed because several of them change the numbers you are about to display, and
you should know which ones are still soft.

1. **Road routing is switched off in production.** Every distance today —
   including the one the delivery fee is charged from, which predates this
   work — is a straight line multiplied by 1.3 to approximate roads. Across a
   river or a rail line, that under-states the journey badly. Standing up our
   own OSRM instance is the fix; the flag is already there
   (`DELIVERY_ROUTING_ENABLED`). Until then, treat `distanceKm` as "about this
   far", not "this far".

2. **The assumed vehicle is a guess.** Nobody is assigned when a customer is
   browsing, so every card's ETA assumes one configured vehicle — currently a
   bicycle. If the fleet is mostly on motorcycles, every card is a few minutes
   pessimistic and every checkout quote with it. We need the real mix from
   operations, and then it is one configuration line.

3. **`/nearby` is not sorted by distance.** The query filters by radius and
   returns whatever order the database gives. It has always been this way and
   nothing noticed, because nothing displayed the distance. Now that the
   distance is on the card, an unsorted "nearby" list looks broken. We intend to
   sort it; tell us if you would rather sort client-side, in which case paging
   has to be settled first.

4. **Restaurants with no coordinates get no distance.** They are returned as
   normal with nulls. We are auditing how many there are and making coordinates
   required at onboarding.

5. **Courier vehicle type may not be accurate in the data.** It defaults to
   bicycle on the courier record, and if onboarding never set it, a motorcycle
   courier is being quoted as a cyclist. Being audited.

6. **Nothing yet measures whether these estimates are any good.** We do not
   store the quoted range against the order, so we cannot compare what we
   promised with what happened. Until we do, the speeds stay guesses that nobody
   can correct with evidence. This is the one on this list we most want to fix,
   and it is invisible to the customer.

7. **The courier's trip to the restaurant is deliberately not modelled.** Before
   assignment there is no courier to measure from, and afterwards the pickup
   usually overlaps the cooking. This makes the estimate slightly pessimistic,
   which is the direction we chose: a customer told 40 minutes and served in 35
   is happy, and the reverse is a support call.

---

## The numbers behind it, if you want them

Travel time is distance ÷ the vehicle's average door-to-door speed, plus a fixed
overhead for parking and the handover:

| Vehicle | km/h | Overhead |
|---|---|---|
| Walking | 4.5 | 3 min |
| Bicycle | 12 | 3 min |
| E-bike / scooter | 18 | 3 min |
| Motorcycle | 24 | 4 min |
| Car | 20 | 6 min |
| Van | 18 | 7 min |

A car is slower than an e-bike here on purpose: it is faster on the road and
slower everywhere else — traffic it cannot filter through, and somewhere to
leave it at both ends. Every figure is configurable and every figure is a guess
about Tashkent at an ordinary hour, which is why point 6 above matters.

The quoted range is the estimate ± the larger of 15% and five minutes, rounded
outward to five minutes.
