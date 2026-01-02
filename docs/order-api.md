# Order API Documentation

Order management for the Food Delivery Platform. Provides endpoints for creating, tracking, and managing food orders including payment processing.

## Base URL

```
/api/v1/orders
```

---

## Table of Contents

1. [Create Order](#1-create-order)
2. [Retrieve Orders](#2-retrieve-orders)
3. [Order Status Management](#3-order-status-management)
4. [Payment Operations](#4-payment-operations)
5. [Enumerations](#enumerations)
6. [Order Lifecycle](#order-lifecycle)

---

## 1. Create Order

### Create Order

**POST** `/`

Create a new order for a restaurant.

**Request Body:**
```json
{
  "restaurantId": 1,
  "orderType": "DELIVERY",
  "items": [
    {
      "menuItemId": 1,
      "quantity": 2,
      "variantId": 1,
      "optionIds": [1, 2],
      "specialInstructions": "Extra cheese please"
    },
    {
      "menuItemId": 2,
      "quantity": 1
    }
  ],
  "deliveryAddress": "789 Customer Street, New York, NY 10003",
  "deliveryLatitude": 40.7282,
  "deliveryLongitude": -73.9942,
  "deliveryInstructions": "Ring doorbell twice",
  "tableId": "T5",
  "customerName": "John Doe",
  "customerPhone": "+12125551234",
  "notes": "Please include extra napkins",
  "tipAmount": 5.00,
  "discountCode": "SAVE10"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| restaurantId | Long | Yes | Target restaurant ID |
| orderType | OrderType | Yes | Type of order |
| items | List\<OrderItemRequest\> | Yes | Order items (min 1) |
| deliveryAddress | string | No | Full delivery address (max 500) |
| deliveryLatitude | BigDecimal | No | Latitude (-90 to 90) |
| deliveryLongitude | BigDecimal | No | Longitude (-180 to 180) |
| deliveryInstructions | string | No | Delivery instructions (max 500) |
| tableId | string | No | Table ID for DINE_IN orders |
| customerName | string | No | Customer name (max 200) |
| customerPhone | string | No | Phone in E.164 format |
| notes | string | No | Order notes (max 1000) |
| tipAmount | BigDecimal | No | Tip amount (non-negative) |
| discountCode | string | No | Discount/promo code |

**OrderItemRequest:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| menuItemId | Long | Yes | Menu item ID |
| quantity | Integer | Yes | Quantity (min 1) |
| variantId | Long | No | Variant ID (e.g., size) |
| optionIds | List\<Long\> | No | Add-on option IDs |
| specialInstructions | string | No | Special instructions (max 500) |

**Response (201):**
```json
{
  "success": true,
  "message": "Order created successfully",
  "data": {
    "id": 1,
    "externalOrderNo": "ORD-2024-001",
    "consumerId": 100,
    "consumerName": "John Doe",
    "restaurantId": 1,
    "restaurantName": "Pizza Palace",
    "orderType": "DELIVERY",
    "status": "CREATED",
    "paymentStatus": "PENDING",
    "items": [...],
    "subtotal": 25.00,
    "tax": 2.25,
    "deliveryFee": 3.99,
    "discount": 2.50,
    "tipAmount": 5.00,
    "total": 33.74,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

**Roles:** CONSUMER, PLATFORM, ADMIN

---

## 2. Retrieve Orders

### Get Order by ID

**GET** `/{id}`

Retrieve order details by ID.

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "externalOrderNo": "ORD-2024-001",
    "consumerId": 100,
    "consumerName": "John Doe",
    "restaurantId": 1,
    "restaurantName": "Pizza Palace",
    "courierId": 50,
    "courierName": "Mike Driver",
    "orderType": "DELIVERY",
    "status": "PREPARING",
    "paymentStatus": "CONFIRMED",
    "items": [
      {
        "id": 1,
        "menuItemId": 1,
        "itemName": "Margherita Pizza",
        "quantity": 2,
        "unitPrice": 12.00,
        "totalPrice": 24.00,
        "variantName": "Large",
        "variantPriceDelta": 2.00,
        "modifiers": [
          {"id": 1, "name": "Extra Cheese", "price": 1.50}
        ],
        "modifiersTotal": 1.50,
        "specialInstructions": "Extra cheese please"
      }
    ],
    "subtotal": 25.00,
    "tax": 2.25,
    "deliveryFee": 3.99,
    "discount": 2.50,
    "tipAmount": 5.00,
    "total": 33.74,
    "deliveryAddress": "789 Customer Street, New York, NY 10003",
    "deliveryInstructions": "Ring doorbell twice",
    "estimatedPrepTimeMinutes": 25,
    "estimatedDeliveryTime": "2024-01-15T11:15:00",
    "createdAt": "2024-01-15T10:30:00",
    "acceptedAt": "2024-01-15T10:32:00",
    "readyAt": null,
    "deliveredAt": null
  }
}
```

---

### Get Order by Number

**GET** `/number/{orderNo}`

Retrieve order details by external order number.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| orderNo | string | External order number (e.g., ORD-2024-001) |

---

### Get My Orders

**GET** `/my`

Get all orders for the currently authenticated user.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-indexed) |
| size | int | 20 | Page size |
| sort | string | createdAt,desc | Sort criteria |

**Response (200):**
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3
  }
}
```

**Roles:** Any authenticated user

---

### Get Restaurant Orders

**GET** `/restaurant/{restaurantId}`

Get all orders for a specific restaurant.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| restaurantId | Long | Restaurant ID |

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size |

**Roles:** RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN

---

### Get Active Restaurant Orders

**GET** `/restaurant/{restaurantId}/active`

Get active (non-terminal) orders for a restaurant. Returns orders that are not in COMPLETED, CANCELLED, or REFUNDED state.

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "externalOrderNo": "ORD-2024-001",
      "status": "PREPARING",
      ...
    },
    {
      "id": 2,
      "externalOrderNo": "ORD-2024-002",
      "status": "READY",
      ...
    }
  ]
}
```

**Roles:** RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN

---

## 3. Order Status Management

### Update Order Status

**PATCH** `/{id}/status`

Update order status with optional timing information.

**Request Body:**
```json
{
  "status": "ACCEPTED",
  "estimatedPrepTimeMinutes": 25,
  "notes": "Order accepted, starting preparation soon"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| status | OrderStatus | Yes | New status |
| estimatedPrepTimeMinutes | Integer | No | Estimated prep time (when accepting) |
| notes | string | No | Reason for status change |

**Response (200):**
```json
{
  "success": true,
  "message": "Order status updated",
  "data": {
    "id": 1,
    "status": "ACCEPTED",
    "estimatedPrepTimeMinutes": 25,
    "acceptedAt": "2024-01-15T10:32:00",
    ...
  }
}
```

**Roles:** RESTAURANT_OWNER, RESTAURANT_STAFF, COURIER, PLATFORM, ADMIN

---

### Cancel Order

**POST** `/{id}/cancel`

Cancel an existing order.

**Request Body:**
```json
{
  "reason": "Customer requested cancellation",
  "requestRefund": true
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| reason | string | Yes | Cancellation reason (max 500) |
| requestRefund | Boolean | No | Request refund (default: true) |

**Response (200):**
```json
{
  "success": true,
  "message": "Order cancelled",
  "data": {
    "id": 1,
    "status": "CANCELLED",
    "cancellationReason": "Customer requested cancellation",
    ...
  }
}
```

**Note:** Orders can only be cancelled in CREATED, ACCEPTED, PREPARING, or READY states.

**Roles:** Any authenticated user (order owner)

---

## 4. Payment Operations

### Create Payment

**POST** `/{orderId}/payment`

Create a payment intent for an order.

**Request Body:**
```json
{
  "paymentMethod": "card",
  "returnUrl": "https://example.com/payment/callback",
  "metadata": "optional metadata"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| paymentMethod | string | Yes | Payment method (e.g., "card") |
| returnUrl | string | No | Redirect URL for payment completion |
| metadata | string | No | Additional metadata |

**Response (201):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderId": 1,
    "provider": "stripe",
    "providerPaymentId": "pi_1234567890",
    "paymentIntentId": "pi_1234567890",
    "clientSecret": "pi_1234567890_secret_xxx",
    "paymentMethod": "card",
    "amount": 33.74,
    "currency": "USD",
    "status": "PENDING",
    "createdAt": "2024-01-15T10:31:00"
  }
}
```

---

### Get Payment

**GET** `/{orderId}/payment`

Get payment details for an order.

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "orderId": 1,
    "provider": "stripe",
    "paymentMethod": "card",
    "amount": 33.74,
    "currency": "USD",
    "status": "CONFIRMED",
    "confirmedAt": "2024-01-15T10:32:00",
    "refundAmount": null,
    "refundedAt": null
  }
}
```

---

## Enumerations

### OrderStatus

| Value | Description | Cancellable | Terminal |
|-------|-------------|-------------|----------|
| `CREATED` | Order created, awaiting payment/acceptance | Yes | No |
| `ACCEPTED` | Order accepted by restaurant | Yes | No |
| `PREPARING` | Order is being prepared | Yes | No |
| `READY` | Order ready for pickup/delivery | Yes | No |
| `PICKED_UP` | Order picked up by courier | No | No |
| `IN_TRANSIT` | Order in transit to customer | No | No |
| `DELIVERED` | Order delivered to customer | No | No |
| `COMPLETED` | Order completed and closed | No | Yes |
| `CANCELLED` | Order was cancelled | No | Yes |
| `REFUNDED` | Order was refunded | No | Yes |

### Status Transitions

```
CREATED → ACCEPTED, CANCELLED
ACCEPTED → PREPARING, CANCELLED
PREPARING → READY, CANCELLED
READY → PICKED_UP, DELIVERED, COMPLETED, CANCELLED
PICKED_UP → IN_TRANSIT, DELIVERED
IN_TRANSIT → DELIVERED
DELIVERED → COMPLETED, REFUNDED
COMPLETED → REFUNDED
CANCELLED → REFUNDED
```

### OrderType

| Value | Description |
|-------|-------------|
| `DELIVERY` | Delivery to customer address |
| `TAKEAWAY` | Customer picks up from restaurant |
| `PICKUP` | Alias for TAKEAWAY |
| `DINE_IN` | Dine-in at restaurant (hall service) |

### PaymentStatus

| Value | Description | Terminal |
|-------|-------------|----------|
| `PENDING` | Payment not initiated | No |
| `PROCESSING` | Payment in progress | No |
| `CONFIRMED` | Payment successful | No |
| `FAILED` | Payment failed | No |
| `REFUNDED` | Payment refunded | Yes |
| `CANCELLED` | Payment cancelled | Yes |

### Payment Status Transitions

```
PENDING → PROCESSING, CONFIRMED, FAILED, CANCELLED
PROCESSING → CONFIRMED, FAILED, CANCELLED
CONFIRMED → REFUNDED
FAILED → PENDING, CANCELLED
```

---

## Order Lifecycle

### Typical Delivery Order Flow

```
1. Customer creates order
   └── Status: CREATED

2. Restaurant accepts order
   └── Status: ACCEPTED
   └── Sets estimated prep time

3. Kitchen starts preparing
   └── Status: PREPARING

4. Order ready for pickup
   └── Status: READY
   └── Courier assigned

5. Courier picks up order
   └── Status: PICKED_UP

6. Courier in transit
   └── Status: IN_TRANSIT

7. Order delivered
   └── Status: DELIVERED

8. Order closed
   └── Status: COMPLETED
```

### Takeaway/Pickup Flow

```
CREATED → ACCEPTED → PREPARING → READY → COMPLETED
```

### Dine-In Flow

```
CREATED → ACCEPTED → PREPARING → READY → DELIVERED → COMPLETED
```

---

## Error Responses

### 404 Order Not Found

```json
{
  "success": false,
  "message": "Order not found"
}
```

### 400 Invalid Status Transition

```json
{
  "success": false,
  "message": "Cannot transition from DELIVERED to PREPARING"
}
```

### 400 Order Not Cancellable

```json
{
  "success": false,
  "message": "Order cannot be cancelled in current state"
}
```

### 403 Forbidden

```json
{
  "success": false,
  "message": "Not authorized to access this order"
}
```

---

## Role Requirements Summary

| Endpoint | Roles |
|----------|-------|
| POST `/` | CONSUMER, PLATFORM, ADMIN |
| GET `/{id}` | Any authenticated |
| GET `/number/{orderNo}` | Any authenticated |
| GET `/my` | Any authenticated |
| GET `/restaurant/{id}` | RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN |
| GET `/restaurant/{id}/active` | RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN |
| PATCH `/{id}/status` | RESTAURANT_OWNER, RESTAURANT_STAFF, COURIER, PLATFORM, ADMIN |
| POST `/{id}/cancel` | Any authenticated (owner) |
| POST `/{orderId}/payment` | Any authenticated |
| GET `/{orderId}/payment` | Any authenticated |
