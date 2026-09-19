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

# 3. Grant a venue
curl -X PUT https://zbrr.uz/api/v1/admin/partners/1/venues \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"restaurantId":100,"externalVenueId":"55",
       "capabilities":["MENU_WRITE","ORDER_STATUS_WRITE"]}'
```

The partner code must match the `external_source` stamped on imported menu items
— `RESTOS` for Restos. That is what ties a partner's key to the items they may
change, so a partner can only ever touch what they themselves imported.

---

## Not built yet

Named so nobody builds against something that is not there:

- **Pushing orders to you** is still ours to do. This API is you calling us.
- **Order-status webhooks out to you** are not built; you poll or we agree a
  callback later.
- **Reading a menu back** through this API is not implemented — we pull yours.
