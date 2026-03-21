# Order Issue Resolution API Documentation

Comprehensive order issue resolution system including automated compensation, courier reassignment, escalation, self-service support, and delivery proof management.

## Base URLs

- Customer Credits: `/api/v1/customer/credits`
- Self-Service: `/api/v1/customer/self-service`
- Delivery Proof (Customer): `/api/v1/customer/orders/{orderId}/proof`
- Delivery Proof (Courier): `/api/v1/courier/deliveries/{orderId}/proof`
- Compensation (Admin): `/api/v1/admin/compensation`
- Escalation (Admin): `/api/v1/admin/escalation`
- Reassignment (Admin): `/api/v1/admin/courier-reassignment`

---

## Table of Contents

1. [Customer Credits](#1-customer-credits)
2. [Self-Service Resolutions](#2-self-service-resolutions)
3. [Delivery Proof](#3-delivery-proof)
4. [Admin Compensation Management](#4-admin-compensation-management)
5. [Admin Escalation Management](#5-admin-escalation-management)
6. [Admin Courier Reassignment](#6-admin-courier-reassignment)
7. [Enumerations](#enumerations)

---

## 1. Customer Credits

Endpoints for customers to view their credit balance and compensation history.

**Required Role:** `CONSUMER`

### Get Credit Balance

**GET** `/api/v1/customer/credits/balance`

Get the current credit balance for the authenticated customer.

**Response (200):**
```json
{
  "success": true,
  "data": 25.50
}
```

### Get Credit Details

**GET** `/api/v1/customer/credits`

Get detailed credit information including total earned and used.

**Response (200):**
```json
{
  "success": true,
  "data": {
    "userId": 100,
    "userEmail": "customer@example.com",
    "balance": 25.50,
    "totalEarned": 75.00,
    "totalUsed": 49.50,
    "lastCreditAt": "2024-01-15T10:30:00Z"
  }
}
```

### Get Compensation History

**GET** `/api/v1/customer/credits/history`

Get paginated compensation history for the authenticated customer.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-indexed) |
| size | int | 20 | Page size |
| sort | string | createdAt,desc | Sort field and direction |

**Response (200):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "userId": 100,
        "userEmail": "customer@example.com",
        "orderId": 500,
        "orderNumber": "ORD-2024-500",
        "triggerType": "LATE_DELIVERY",
        "compensationType": "CREDIT",
        "amount": 5.00,
        "description": "Late delivery: 25 minutes",
        "ruleId": 1,
        "ruleName": "Late Delivery - 15+ min",
        "status": "COMPLETED",
        "createdAt": "2024-01-15T10:30:00Z"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

---

## 2. Self-Service Resolutions

Endpoints for customers to resolve common order issues without contacting support.

**Required Role:** `CONSUMER`

### Check Eligibility

**GET** `/api/v1/customer/self-service/orders/{orderId}/eligibility`

Check what self-service options are available for an order.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| orderId | Long | Order ID |

**Response (200):**
```json
{
  "success": true,
  "data": {
    "autoRefund": true,
    "missingItems": true,
    "cancel": false
  }
}
```

| Eligibility | Description |
|-------------|-------------|
| autoRefund | Order total is below auto-refund threshold and not already refunded |
| missingItems | Order is delivered and missing items not already reported |
| cancel | Order can be cancelled (not yet in preparation) |

### Request Auto-Refund

**POST** `/api/v1/customer/self-service/orders/{orderId}/auto-refund`

Request automatic refund for small orders (below configurable threshold, default $10).

**Request Body:**
```json
{
  "reason": "Order arrived cold and inedible"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| reason | string | Yes | Reason for refund request |

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderId": 500,
    "resolutionType": "AUTO_REFUND_SMALL",
    "amount": "8.99",
    "status": "COMPLETED",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

**Error Responses:**
- `400`: Order total exceeds auto-refund limit
- `400`: Daily self-service limit reached

### Report Missing Items

**POST** `/api/v1/customer/self-service/orders/{orderId}/missing-items`

Report missing items from a delivered order and receive refund for those items.

**Request Body:**
```json
{
  "missingItemIds": [101, 102]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| missingItemIds | List<Long> | Yes | IDs of missing order items |

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "orderId": 500,
    "resolutionType": "MISSING_ITEMS",
    "amount": "12.50",
    "status": "COMPLETED",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

### Cancel Order (Pre-Preparation)

**POST** `/api/v1/customer/self-service/orders/{orderId}/cancel`

Cancel an order before the restaurant starts preparing it.

**Request Body:**
```json
{
  "reason": "Changed my mind"
}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 3,
    "orderId": 500,
    "resolutionType": "CANCEL_PRE_PREP",
    "amount": "25.99",
    "status": "COMPLETED",
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

**Error Responses:**
- `400`: Order is already being prepared - please contact support

### Get Resolution History

**GET** `/api/v1/customer/self-service/history`

Get all self-service resolution history for the authenticated customer.

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "orderId": 500,
      "resolutionType": "AUTO_REFUND_SMALL",
      "amount": "8.99",
      "status": "COMPLETED",
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ]
}
```

---

## 3. Delivery Proof

Endpoints for managing proof of delivery photos and verification.

### Submit Delivery Proof (Courier)

**POST** `/api/v1/courier/deliveries/{orderId}/proof`

**Required Role:** `COURIER`

Submit proof of delivery including photo and location.

**Request Body:**
```json
{
  "photoUrl": "https://storage.example.com/proofs/123.jpg",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "recipientName": "John Doe",
  "notes": "Left at front door per customer request"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| photoUrl | string | No | URL of proof photo |
| latitude | BigDecimal | Yes | Delivery location latitude |
| longitude | BigDecimal | Yes | Delivery location longitude |
| recipientName | string | No | Name of person who received order |
| notes | string | No | Delivery notes |

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderId": 500,
    "courierId": 50,
    "photoUrl": "https://storage.example.com/proofs/123.jpg",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "recipientName": "John Doe",
    "notes": "Left at front door per customer request",
    "verified": false,
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

### Get Delivery Proof (Courier)

**GET** `/api/v1/courier/deliveries/{orderId}/proof`

**Required Role:** `COURIER`

### Get Delivery Proof (Customer)

**GET** `/api/v1/customer/orders/{orderId}/proof`

**Required Role:** `CONSUMER`

Get proof of delivery for an order.

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderId": 500,
    "courierId": 50,
    "photoUrl": "https://storage.example.com/proofs/123.jpg",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "recipientName": "John Doe",
    "notes": "Left at front door per customer request",
    "verified": true,
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

### Verify Delivery Proof (Admin)

**POST** `/api/v1/admin/delivery-proofs/{proofId}/verify`

**Required Role:** `ADMIN`

Manually verify a delivery proof.

### Get Unverified Proofs (Admin)

**GET** `/api/v1/admin/delivery-proofs/unverified`

**Required Role:** `ADMIN`

Get all unverified delivery proofs for review.

---

## 4. Admin Compensation Management

Endpoints for administrators to manage compensation rules and issue manual compensations.

**Required Role:** `ADMIN`

### Get All Compensation Rules

**GET** `/api/v1/admin/compensation/rules`

Get all compensation rules including inactive ones.

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Late Delivery - 15+ min",
      "description": "Credit for deliveries 15-30 minutes late",
      "triggerType": "LATE_DELIVERY",
      "compensationType": "CREDIT",
      "conditions": {
        "delayMinutesMin": 15,
        "delayMinutesMax": 30
      },
      "creditPercentage": 10.00,
      "creditFixedAmount": null,
      "maxCompensation": 10.00,
      "priority": 1,
      "active": true
    }
  ]
}
```

### Get Active Rules

**GET** `/api/v1/admin/compensation/rules/active`

Get only active compensation rules.

### Toggle Rule Status

**PUT** `/api/v1/admin/compensation/rules/{ruleId}/toggle`

Enable or disable a compensation rule.

### Issue Manual Compensation

**POST** `/api/v1/admin/compensation/manual`

Issue a manual compensation to a customer.

**Request Body:**
```json
{
  "userId": 100,
  "orderId": 500,
  "type": "CREDIT",
  "amount": 15.00,
  "description": "Customer goodwill credit for service issues"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| userId | Long | Yes | Customer user ID |
| orderId | Long | No | Related order ID (optional) |
| type | CompensationType | Yes | CREDIT or REFUND |
| amount | BigDecimal | Yes | Compensation amount |
| description | string | Yes | Reason for compensation |

### Get User Compensations

**GET** `/api/v1/admin/compensation/users/{userId}`

Get all compensations for a specific user.

### Get Order Compensations

**GET** `/api/v1/admin/compensation/orders/{orderId}`

Get all compensations for a specific order.

---

## 5. Admin Escalation Management

Endpoints for managing support ticket escalations.

**Required Role:** `ADMIN` or `SUPPORT_MANAGER`

### Get Escalated Tickets

**GET** `/api/v1/admin/escalation/tickets`

Get all escalated support tickets.

### Get Ticket Escalation History

**GET** `/api/v1/admin/escalation/tickets/{ticketId}/history`

Get escalation history for a specific ticket.

### Manually Escalate Ticket

**POST** `/api/v1/admin/escalation/tickets/{ticketId}/escalate`

Manually escalate a support ticket.

**Request Body:**
```json
{
  "escalationLevel": 2,
  "reason": "Customer is a VIP - needs priority handling"
}
```

### Get Escalation Rules

**GET** `/api/v1/admin/escalation/rules`

Get all escalation rules.

### Toggle Escalation Rule

**PUT** `/api/v1/admin/escalation/rules/{ruleId}/toggle`

Enable or disable an escalation rule.

---

## 6. Admin Courier Reassignment

Endpoints for managing courier reassignments.

**Required Role:** `ADMIN` or `FLEET_MANAGER`

### Reassign Courier

**POST** `/api/v1/admin/courier-reassignment/orders/{orderId}/reassign`

Reassign an order to a different courier.

**Request Body:**
```json
{
  "newCourierId": 51,
  "reason": "COURIER_UNAVAILABLE",
  "notes": "Original courier had vehicle breakdown"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| newCourierId | Long | Yes | ID of new courier |
| reason | ReassignmentReason | Yes | Reason for reassignment |
| notes | string | No | Additional notes |

### Get Reassignment History

**GET** `/api/v1/admin/courier-reassignment/orders/{orderId}/history`

Get reassignment history for an order.

### Get Pending Reassignments

**GET** `/api/v1/admin/courier-reassignment/pending`

Get orders that need courier reassignment.

---

## Enumerations

### CompensationType
| Value | Description |
|-------|-------------|
| CREDIT | Store credit added to customer account |
| REFUND | Refund to original payment method |
| BOTH | Both credit and refund |

### CompensationTriggerType
| Value | Description |
|-------|-------------|
| LATE_DELIVERY | Order delivered late |
| MISSING_ITEMS | Items missing from order |
| WRONG_ITEMS | Incorrect items delivered |
| QUALITY_ISSUE | Food quality problems |
| CANCELLED_BY_RESTAURANT | Restaurant cancelled order |
| MANUAL | Manual compensation by admin |

### CompensationStatus
| Value | Description |
|-------|-------------|
| PENDING | Compensation being processed |
| COMPLETED | Successfully applied |
| FAILED | Failed to apply |

### SelfServiceResolutionType
| Value | Description |
|-------|-------------|
| AUTO_REFUND_SMALL | Automatic refund for small orders |
| MISSING_ITEMS | Missing items refund |
| CANCEL_PRE_PREP | Pre-preparation cancellation |

### ReassignmentReason
| Value | Description |
|-------|-------------|
| COURIER_UNAVAILABLE | Courier became unavailable |
| CUSTOMER_REQUEST | Customer requested different courier |
| COURIER_TOO_FAR | Courier is too far from pickup |
| PERFORMANCE_ISSUE | Courier performance concern |
| VEHICLE_ISSUE | Courier vehicle problem |
| MANUAL | Manual reassignment by admin |

### EscalationTriggerType
| Value | Description |
|-------|-------------|
| SLA_BREACH | SLA time exceeded |
| CUSTOMER_REQUEST | Customer requested escalation |
| REPEAT_ISSUE | Recurring problem detected |
| HIGH_VALUE_ORDER | High-value order requires attention |
| VIP_CUSTOMER | VIP customer needs priority |
| MANUAL | Manual escalation |

---

## Configuration

The following configuration properties control the order issue resolution system:

```yaml
app:
  selfservice:
    max-auto-refund: 10.00       # Maximum order total for auto-refund
    max-daily-resolutions: 3     # Maximum self-service resolutions per day per user

  compensation:
    late-delivery-threshold: 15  # Minutes late before compensation triggers
    max-auto-compensation: 50.00 # Maximum automatic compensation amount

  escalation:
    sla-check-interval: 60000    # SLA check interval in milliseconds
```
