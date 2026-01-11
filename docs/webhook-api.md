# Webhook API Documentation

External webhook endpoints for the Food Delivery Platform. Handles payment provider callbacks and event notifications.

## Base URL

```
/api/v1/webhooks
```

---

## Table of Contents

1. [Overview](#1-overview)
2. [Payment Webhooks](#2-payment-webhooks)
3. [Refund Webhooks](#3-refund-webhooks)
4. [Health Check](#4-health-check)
5. [Webhook Events](#5-webhook-events)
6. [Security](#6-security)
7. [Configuration](#7-configuration)
8. [Integration Flow](#8-integration-flow)

---

## 1. Overview

The Webhook module handles callbacks from external payment providers (Stripe-like integration). These endpoints are **publicly accessible** (no authentication required) but should be secured via signature verification.

### Architecture

```
┌─────────────────────┐
│  Payment Provider   │
│     (Stripe)        │
└──────────┬──────────┘
           │
           │ HTTPS POST
           │ Stripe-Signature header
           ▼
┌─────────────────────┐
│  WebhookController  │
│  /api/v1/webhooks   │
└──────────┬──────────┘
           │
           │ Event Processing
           ▼
┌─────────────────────┐
│   PaymentService    │
│                     │
│ • confirmPayment()  │
│ • failPayment()     │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Order Updated     │
│   Payment Saved     │
└─────────────────────┘
```

---

## 2. Payment Webhooks

### Handle Payment Webhook

**POST** `/api/v1/webhooks/payments`

Receives payment event notifications from the payment provider.

**Headers:**
| Header | Required | Description |
|--------|----------|-------------|
| Stripe-Signature | No* | Webhook signature for verification |

*Signature verification is recommended for production.

**Request Body:**
```json
{
  "id": "evt_1abc2def3ghi4jkl",
  "type": "payment_intent.succeeded",
  "api_version": "2023-10-16",
  "created": 1704067200,
  "data": {
    "payment_intent_id": "pi_3abc4def5ghi6jkl",
    "payment_id": "ch_1mno2pqr3stu4vwx",
    "amount": 2999,
    "amount_decimal": 29.99,
    "currency": "usd",
    "status": "succeeded",
    "payment_method_type": "card",
    "customer_id": "cus_abc123",
    "metadata": "{\"orderId\": 456}"
  },
  "rawData": "{...original payload...}"
}
```

**Response (200 - Success):**
```json
{
  "success": true,
  "message": "Webhook processed"
}
```

**Response (200 - Error):**
```json
{
  "success": false,
  "message": "Webhook processing failed: Payment not found"
}
```

**Note:** Returns 200 even on errors to prevent webhook retries for known issues.

---

## 3. Refund Webhooks

### Handle Refund Webhook

**POST** `/api/v1/webhooks/refund`

Receives refund event notifications from the payment provider.

**Request Body:**
```json
{
  "id": "evt_refund_abc123",
  "type": "refund.succeeded",
  "api_version": "2023-10-16",
  "created": 1704067200,
  "data": {
    "payment_id": "ch_1mno2pqr3stu4vwx",
    "amount": 2999,
    "currency": "usd",
    "status": "succeeded"
  }
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Refund webhook processed"
}
```

---

## 4. Health Check

### Webhook Health Check

**GET** `/api/v1/webhooks/health`

Verify the webhook endpoint is reachable. Useful for payment provider configuration verification.

**Response (200):**
```json
{
  "success": true,
  "message": "Success",
  "data": "OK"
}
```

---

## 5. Webhook Events

### Supported Event Types

| Event Type | Description | Action |
|------------|-------------|--------|
| `payment_intent.succeeded` | Payment completed successfully | Confirm payment, update order |
| `payment_intent.payment_failed` | Payment failed | Mark payment as failed |
| `charge.refunded` | Charge was refunded | Handle refund confirmation |
| `refund.succeeded` | Refund processed successfully | Confirm refund |

### Event Processing Flow

```
payment_intent.succeeded
        │
        ▼
┌───────────────────┐
│ PaymentService.   │
│ confirmPayment()  │
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│ Payment Status:   │
│ PENDING → CONFIRMED│
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│ Order Status:     │
│ Updates allowed   │
└───────────────────┘


payment_intent.payment_failed
        │
        ▼
┌───────────────────┐
│ PaymentService.   │
│ failPayment()     │
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│ Payment Status:   │
│ PENDING → FAILED  │
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│ Error stored:     │
│ code + message    │
└───────────────────┘
```

---

## 6. Security

### Endpoint Access

Webhook endpoints are **publicly accessible** (excluded from JWT authentication):

```java
// SecurityConfig.java
"/api/v1/webhooks/**" // Public access
```

### Signature Verification (Recommended)

In production, verify webhook signatures to ensure requests come from legitimate sources:

```java
// Stripe signature verification example
Webhook.constructEvent(payload, signature, webhookSecret);
```

### Best Practices

1. **Always verify signatures** in production
2. **Return 200 status** even on processing errors to prevent infinite retries
3. **Idempotency**: Handle duplicate webhook deliveries gracefully
4. **Logging**: Log all webhook events for debugging
5. **Timeout handling**: Process webhooks quickly (< 30 seconds)

---

## 7. Configuration

### Application Properties

```yaml
app:
  payment:
    provider: stripe
    webhook-secret: ${PAYMENT_WEBHOOK_SECRET:whsec_test_secret}
    api-key: ${PAYMENT_API_KEY:sk_test_key}
```

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `PAYMENT_WEBHOOK_SECRET` | Yes (prod) | Stripe webhook signing secret |
| `PAYMENT_API_KEY` | Yes | Stripe API key |

### Stripe Dashboard Setup

1. Go to Stripe Dashboard → Developers → Webhooks
2. Add endpoint: `https://yourdomain.com/api/v1/webhooks/payments`
3. Select events:
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`
   - `charge.refunded`
4. Copy signing secret to `PAYMENT_WEBHOOK_SECRET`

---

## 8. Integration Flow

### Complete Payment Flow with Webhooks

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PAYMENT + WEBHOOK FLOW                               │
└─────────────────────────────────────────────────────────────────────────────┘

 CONSUMER                 PLATFORM                 STRIPE               BACKEND
    │                        │                        │                     │
    │ 1. Create Order        │                        │                     │
    │ ──────────────────────▶│                        │                     │
    │                        │                        │                     │
    │ 2. Order Created       │                        │                     │
    │    (PAYMENT: PENDING)  │                        │                     │
    │ ◀──────────────────────│                        │                     │
    │                        │                        │                     │
    │ 3. Initiate Payment    │                        │                     │
    │ ──────────────────────▶│ 4. Create PaymentIntent│                     │
    │                        │ ──────────────────────▶│                     │
    │                        │                        │                     │
    │                        │ 5. Client Secret       │                     │
    │ ◀──────────────────────│◀──────────────────────│                     │
    │                        │                        │                     │
    │ 6. Enter Card Details  │                        │                     │
    │ ──────────────────────────────────────────────▶│                     │
    │                        │                        │                     │
    │ 7. Payment Processed   │                        │                     │
    │ ◀──────────────────────────────────────────────│                     │
    │                        │                        │                     │
    │                        │                        │ 8. Webhook Event    │
    │                        │                        │ ───────────────────▶│
    │                        │                        │                     │
    │                        │                        │         ┌───────────┴───────────┐
    │                        │                        │         │ 9. Verify Signature   │
    │                        │                        │         │ 10. confirmPayment()  │
    │                        │                        │         │ 11. Update Order      │
    │                        │                        │         └───────────┬───────────┘
    │                        │                        │                     │
    │                        │                        │ 12. HTTP 200 OK     │
    │                        │                        │ ◀──────────────────│
    │                        │                        │                     │
    ▼                        ▼                        ▼                     ▼
```

### Error Handling

| Scenario | Response | Retry? |
|----------|----------|--------|
| Valid event processed | 200 OK | No |
| Invalid signature | 400 Bad Request | Yes |
| Processing error (logged) | 200 OK | No |
| Unknown event type | 200 OK (ignored) | No |
| Server error | 500 Error | Yes |

---

## Data Transfer Objects

### PaymentWebhookPayload

| Field | Type | Description |
|-------|------|-------------|
| id | string | Webhook event ID |
| type | string | Event type (e.g., `payment_intent.succeeded`) |
| api_version | string | API version |
| created | long | Unix timestamp of event creation |
| data | PaymentData | Payment event data |
| rawData | string | Raw payload for signature verification |

### PaymentData

| Field | Type | Description |
|-------|------|-------------|
| payment_intent_id | string | Payment intent ID |
| payment_id | string | Payment/charge ID |
| amount | long | Amount in cents |
| amount_decimal | BigDecimal | Amount as decimal |
| currency | string | Currency code |
| status | string | Payment status |
| payment_method_type | string | Payment method (card, etc.) |
| customer_id | string | Customer ID |
| error_code | string | Error code (for failures) |
| error_message | string | Error message (for failures) |
| metadata | string | Additional metadata |

---

## Testing Webhooks

### Using Stripe CLI

```bash
# Install Stripe CLI
brew install stripe/stripe-cli/stripe

# Login
stripe login

# Forward webhooks to local server
stripe listen --forward-to localhost:8080/api/v1/webhooks/payments

# Trigger test events
stripe trigger payment_intent.succeeded
stripe trigger payment_intent.payment_failed
```

### Manual Testing

```bash
# Test health endpoint
curl http://localhost:8080/api/v1/webhooks/health

# Test payment webhook (without signature verification)
curl -X POST http://localhost:8080/api/v1/webhooks/payments \
  -H "Content-Type: application/json" \
  -d '{
    "id": "evt_test_123",
    "type": "payment_intent.succeeded",
    "data": {
      "payment_intent_id": "pi_test_456",
      "payment_id": "ch_test_789",
      "amount": 2999,
      "currency": "usd",
      "status": "succeeded"
    }
  }'
```

---

## Monitoring

### Key Metrics

- Webhook events received (by type)
- Processing success/failure rate
- Average processing time
- Signature verification failures

### Log Events

| Event | Level | Description |
|-------|-------|-------------|
| Webhook received | INFO | Event type and ID logged |
| Payment succeeded | INFO | PaymentIntent ID logged |
| Payment failed | WARN | PaymentIntent and error logged |
| Processing error | ERROR | Full error with stack trace |
| Unhandled event | DEBUG | Unknown event type received |
