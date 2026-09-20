# Partner API

For an integrated POS calling the platform. This is the contract Restos asked
for; it is deliberately small and addressed by **your** identifiers, so you never
have to store ours.

Base URL: `https://zbrr.uz` (staging: `https://staging.zbrr.uz`).

---

## Authentication

Send your key in the **`X-Partner-Key`** header on every request.

```
X-Partner-Key: zbrp_7fKq2mXvRtYb9cDe_hJ4nPw8sVzA...
```

Not `Authorization: Bearer`. That header already carries user sessions here, and
a partner key sent in it would be parsed as a session token, fail, and leave you
with a misleading error. Keeping the two in separate headers means neither can
be mistaken for the other — which matters, because a user session is one person
while a partner key carries authority over many restaurants.

**A key is environment-scoped.** A key issued for staging is refused in
production and the reverse. This is on purpose: the one integration mistake
whose cost lands outside our two companies is a test order printing in a real
kitchen.

**Keys are per partner; grants are per venue.** Rotating a key does not disturb
your venue access. A key that leaks is revoked on its own, and access to each
venue carries two independent capabilities:

| Capability | Lets you |
|---|---|
| `MENU_WRITE` | Change price and availability in that venue |
| `ORDER_STATUS_WRITE` | Report an order's progress |

A freshly created grant carries neither. Mapping your venue to ours and
authorising writes into its kitchen are separate decisions.

---

## Menu

### Update one item

```http
PATCH /api/v1/partner/venues/{yourVenueId}/menu/items/{yourProductId}
X-Partner-Key: zbrp_...
Content-Type: application/json

{ "price": 32000, "available": true }
```

Both fields are optional and **omitting one leaves it unchanged** — this is a
partial update, not a replacement. Sending neither is rejected.

- **`price`** is charged to the customer exactly as sent. We add nothing to it.
- **`available`** means sold out, not delisted. To withdraw an item, drop it from
  your menu snapshot; the nightly sync deactivates what is no longer there.

### Update many items

```http
POST /api/v1/partner/venues/{yourVenueId}/menu/items
X-Partner-Key: zbrp_...
Content-Type: application/json

[
  { "externalItemId": "4417", "price": 32000 },
  { "externalItemId": "4418", "available": false }
]
```

Up to 1000 items per call. Use this for a menu-wide change — a markup moving
across four hundred dishes is one call, not four hundred.

**Partial success is reported, not rolled back:**

```json
{
  "success": true,
  "data": { "updated": 397, "unknownItemIds": ["9001", "9002"], "rejected": [] }
}
```

A change across four hundred dishes must not be lost because three of them were
deleted here last week. You get those three back by id and can reconcile them;
the rest are already live.

You can only reach items that came from you. An item a restaurant typed into our
own panel is not yours to reprice, and it comes back as unknown.

---

## Orders

### Report a status

```http
POST /api/v1/partner/orders/{ourOrderReference}/status
X-Partner-Key: zbrp_...
Content-Type: application/json

{ "status": "ACCEPTED", "reason": null }
```

Orders are addressed by **our** reference, `FD-YYYYMMDD-XXXXXX` — the order was
created here and pushed to you, so it is ours to name. Menus go the other way
and use your ids. Whoever owns the thing names it.

| `status` | Meaning |
|---|---|
| `ACCEPTED` | The venue will cook it |
| `PREPARING` | Cooking has started. Optional |
| `READY` | Made, waiting for collection |
| `DECLINED` | The venue will not take it |

`reason` is optional, up to 500 characters, and is **shown to the customer** when
you decline. "We have run out of lamb" is a different conversation from silence.

A decline cancels the order and refunds the customer automatically. A venue may
decline at any point, including mid-cook — the customer's cancellation cutoff
does not bind you.

**Safe to retry.** Reporting a status the order already has returns `200` and
changes nothing, rather than failing on a "cannot go from ACCEPTED to ACCEPTED"
rule. Your retry after a timeout must never be the thing that breaks an order.

A status the order cannot reach from where it is returns **`409`** naming both,
so a genuine disagreement is visible rather than silently applied:

```json
{
  "success": false,
  "message": "Order FD-20260919-A7K2M9 is DELIVERED here and cannot move to ACCEPTED (you reported ACCEPTED)"
}
```

---

## Limits and errors

**60 requests/minute** per partner for menu updates, **120/minute** for order
status, bursting to double. Counted per partner across every key and venue, so
rotating a key does not hand out a fresh allowance. Over the limit answers `429`
— back off, do not hammer.

| Status | Meaning |
|---|---|
| `401` | Key missing, malformed, revoked, or from the wrong environment |
| `403` | Your grant on that venue lacks the capability |
| `404` | No venue is mapped to that id, or no such order |
| `409` | We disagree about where the order is |
| `422` | The request was understood but cannot be applied |
| `429` | Rate limited |

`404` is deliberately the answer both for a venue that does not exist and for
one that exists but is not yours. Telling the two apart would let anyone map our
venue ids.

Every response uses the same envelope:

```json
{ "success": true, "message": "...", "data": { } }
```

---

## Getting set up

We issue, you receive:

1. **A partner record**, once.
2. **A key per environment.** The secret is shown once at creation and is stored
   here only as a digest — we cannot recover it, so if it is lost we issue a new
   one and revoke the old.
3. **A venue grant per restaurant**, mapping your venue id to ours and naming
   the capabilities.

Start on staging with a key stamped `staging`. When it works, ask for a
production key — it is a separate credential, and the staging one will not work
there.

## Administration (our side)

| | |
|---|---|
| `POST /api/v1/admin/partners` | Register a partner |
| `POST /api/v1/admin/partners/{id}/keys` | Issue a key — **the secret is returned once** |
| `GET /api/v1/admin/partners/{id}/keys` | List keys (metadata only) |
| `DELETE /api/v1/admin/partners/keys/{keyId}` | Revoke a key, effective on the next request |
| `PUT /api/v1/admin/partners/{id}/venues` | Grant or update access to a venue |
| `GET /api/v1/admin/partners/{id}/venues` | List grants |
| `DELETE /api/v1/admin/partners/{id}/venues/{restaurantId}` | Revoke access to a venue |

Admin or platform session required. Deliberately under `/admin/` rather than
`/partner/`: a partner must never be able to widen its own access, and putting
these behind the partner key filter is how that would eventually happen.

Example — onboarding Restos:

```bash
# 1. Register
curl -X POST https://zbrr.uz/api/v1/admin/partners \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"code":"RESTOS","name":"Restos"}'

# 2. Issue a key — copy the secret NOW, it is not shown again
curl -X POST https://zbrr.uz/api/v1/admin/partners/1/keys \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"label":"Restos production, issued to their integration team"}'

# 3. Grant a venue. pushOrders decides whether its orders print on their till;
#    omitting it leaves that setting alone rather than switching a kitchen back.
curl -X PUT https://zbrr.uz/api/v1/admin/partners/1/venues \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"restaurantId":100,"externalVenueId":"55",
       "capabilities":["MENU_WRITE","ORDER_STATUS_WRITE"],"pushOrders":true}'

# 4. Where WE send orders, using the credential they issued us
curl -X PUT https://zbrr.uz/api/v1/admin/partners/1/outbound \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"baseUrl":"https://pos.restos.uz","apiKey":"<their key>",
       "authHeader":"X-Partner-Key"}'
```

Order push needs both step 3's `pushOrders` and step 4. A venue switched on
against a partner with no outbound URL sends nothing and logs an error — the
alternative is orders quietly not printing.

The partner code must match the `external_source` stamped on imported menu items
— `RESTOS` for Restos. That is what ties a partner's key to the items they may
change, so a partner can only ever touch what they themselves imported.

---

---

## Orders we send you

The other direction. When a venue is switched on for it, a new order is POSTed
to **your** `/api/v1/partner/orders` so the ticket prints in the kitchen.

This is asynchronous and off the customer's checkout path: if your system is
slow or down, the order is still taken and we keep retrying. Food arriving late
is recoverable; an order that was never accepted is not.

**Idempotency.** `externalOrderId` is our order reference, `FD-YYYYMMDD-XXXXXX`.
It is assigned once and never changes, so every retry carries the same value —
we rely on your `(partner, venue, id)` uniqueness, and hold our own record of
each push so a redelivered message cannot produce a second ticket from our side
either. A double print is a double-cooked order, so both guards are deliberate.

We treat your `201` and your `200` + `duplicate: true` as the same success.

**Payload** (field names are our proposal — tell us if yours differ):

```json
{
  "restaurantId": 55,
  "externalOrderId": "FD-20260919-A7K2M9",
  "orderType": "DELIVERY",
  "paymentMode": "PREPAID",
  "customer": { "name": "Anvar", "phone": "998901234567" },
  "delivery": { "address": "Mustaqillik 15, kv 42" },
  "items": [
    { "productId": 4417, "variantId": 11, "quantity": 2,
      "specialInstructions": "no onions",
      "name": "Plov", "unitPrice": 30000, "lineTotal": 60000 }
  ],
  "expectedTotal": 75000,
  "subtotal": 60000, "deliveryFee": 15000, "serviceFee": 4800
}
```

`productId` is **your** product id, taken from what your menu import stamped on
the item. Nulls are omitted.

**`variantId` is yours, not ours**, and is sent whenever the customer chose a
size. A dish you sell by size cannot be ordered here without one — we refuse
that basket at checkout rather than let you answer `422 VARIANT_REQUIRED` after
the customer has paid.

**`paymentMode`** is `PREPAID` or `CASH`, always sent.

**`expectedTotal` is the food at your prices plus the delivery fee** — the
amount that reconciles between the two companies for this ticket. Not what the
customer pays (that carries our service fee and any tip, which you cannot see) and
not the food alone (the venue is owed the delivery fee too). It is deliberately
the only figure both sides can compute from the same menu, which is what makes
a check on it mean anything. A discount we fund is not deducted: our promotion
does not reduce what the venue is owed for food they cooked.

**On `422 UNKNOWN_ITEMS`.** We now check at checkout instead: a basket
containing an item you do not have is refused before the customer pays, naming
the dish so they can remove it. You should therefore rarely see this — and if
you do, it means our catalogues have drifted and we want to know.

A rejection is never retried. It is recorded against the order and logged
loudly, because it means a ticket that will never print.

---

## Not built yet

Named so nobody builds against something that is not there:

- **Order-status webhooks out to you** are not built; you poll or we agree a
  callback later.
- **Reading a menu back** through this API is not implemented — we pull yours.

---

## Rotating the credential you issued us

Issue a new key and send it; we replace ours and stop presenting the old one.
There is no multi-step handover on our side — the change takes effect on the
next call.

If you would rather accept both for a window while the change propagates, say
so. We can hold only one at a time, so an overlap has to come from your side.

Rotating the key **we** issued **you** is the other direction and already
supported: we issue a second key, you switch, we revoke the first. Both are live
in between, so there is no moment where a call of yours is refused.
