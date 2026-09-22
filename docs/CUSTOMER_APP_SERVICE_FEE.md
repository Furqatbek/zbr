# For the customer app team: the "Tax" line is not a tax

**What we need:** rename the line the customer sees, and read a new field.
Small change, and the reason matters more than the diff.

---

## What we found

The 8% line the app shows as **Tax** was never a tax.

It arrived in the platform's very first commit as a hard-coded constant, was
never touched again, and nothing in the backend ever read it — no liability was
computed from it, it appeared in no report, and it was never remitted to any
authority. It also matches no Uzbek tax: VAT here is **12% and included in the
displayed shelf price**, whereas this is 8% added at checkout, on the food only.

It is a US sales-tax default that came in with a code template. Our own API docs
still carry the fossil: an example order reading `subtotal: 25.00, tax: 2.25,
deliveryFee: 3.99` for a restaurant called *"Pizza Palace"* — dollars, on a
platform that has only ever traded in so'm.

It is a **platform service fee**. The backend has been renamed accordingly.

## Why the label matters

A restaurant's name is on that receipt, directly above the line. If it says
*tax* and no tax is remitted, the restaurant is the one standing next to the
claim — and a customer who does the arithmetic asks **them** before they ask us.

This came up because our POS partner raised it. They were right to.

## What to change

### 1. Read `serviceFee`

Order responses now carry both:

```json
{
  "subtotal": 60000,
  "serviceFee": 4800,
  "tax": 4800,
  "deliveryFee": 15000,
  "total": 79800
}
```

`tax` is a **deprecated alias holding the same value**, kept only so nothing
breaks today. It will be removed.

```ts
const serviceFee = order.serviceFee ?? order.tax ?? 0;
```

Read the new name first. If you read only `tax`, the day the alias goes the
value becomes `undefined` — and since it is only displayed, nobody would notice
until a total stopped adding up.

### 2. Rename what the customer reads

| Locale | Now | Change to |
|---|---|---|
| Russian | Налог / Налоги | **Сервисный сбор** |
| Uzbek (Latin) | Soliq | **Xizmat haqi** |
| Uzbek (Cyrillic) | Солиқ | **Хизмат ҳақи** |
| English | Tax | **Service fee** |

Please grep for the actual strings rather than trusting this table — we could
not see your repo when writing it, and the Uzbek word for "approximate" is
*taxminiy*, which makes a case-insensitive search for "tax" noisy.

### 3. Handle a zero fee

The rate is now **configurable** and may change, including to zero.

- **Do not hard-code 8%** anywhere. Use the amount the API sends.
- **Hide the line when the amount is 0.** "Service fee: 0 so'm" is noise on a
  receipt, and a customer reading a zero charge wonders what it is.

## What not to do

- **Do not remove the line.** It is a real charge the customer pays and it must
  stay itemised. Folding it silently into the total is worse than the wrong
  label — it would be the same money with no name at all.
- **Do not move it.** Its position in the breakdown is unchanged: after the
  subtotal, before the delivery fee.
- **Do not change any arithmetic.** `total` already includes it and always did.
  The amount is identical; only the name changed.

## Anything else showing this value

Worth a look while you are in there: order history, receipts, email or push
summaries, and anything exported or shared. The word is likely in more than the
checkout screen.

## Update: the fee is now off by default

Since 2026-09-22 the rate defaults to **0**, so `serviceFee` (and its `tax`
alias) come back as `0.00` on new orders unless a deployment sets a rate. The
charge was never chosen — it came in with a template — so it is off until
someone decides otherwise.

**Two things this changes for the app:**

1. **Hide a zero line.** A "Service fee 0 so'm" row is noise. Show the line only
   when the amount is greater than zero.
2. **Do not remove the line.** The rate is configuration and can come back
   without an app release, so the field still has to be read and displayed when
   it is non-zero.

Past orders are untouched: the amount is stamped onto each order when it is
placed, so an order charged 8% still shows 8%.

## Questions we can answer

- **Did the amount change?** Yes, as of 2026-09-22 — the default rate is now 0,
  so new orders carry no service fee. The rename itself changed nothing.
- **Do past orders change?** No. Historical orders keep the amount they were
  charged; the backend column was renamed, not recalculated.
- **Is it going away?** Not decided. That is a commercial question, which is why
  the rate is configurable and why you should not hard-code it.

Ping the backend team if anything here does not match what the API actually
sends — that would be our bug, not yours.
