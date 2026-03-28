# Courier App API Documentation

API endpoints for the **Courier App** (Mobile) frontend application.

**Target Users:** COURIER role

**Base URL:** `http://localhost:8080/api/v1`

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Courier Registration](#2-courier-registration)
3. [Status Management](#3-status-management)
4. [Location Updates](#4-location-updates)
5. [Order Management](#5-order-management)
6. [Earnings & History](#6-earnings--history)
7. [Notifications](#7-notifications)
8. [WebSocket (Real-time)](#8-websocket-real-time)

---

## 1. Authentication

### Login
```
POST /auth/login
```

**Request:**
```json
{
  "emailOrPhone": "courier@fooddelivery.com",
  "password": "password"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 5,
      "email": "courier@fooddelivery.com",
      "fullName": "Alex Courier",
      "roles": ["CONSUMER", "COURIER"]
    }
  }
}
```

### Phone OTP Login
```
POST /auth/phone/request-otp
POST /auth/phone/verify-otp
```
*Same as Consumer App*

### Refresh Token
```
POST /auth/refresh
Authorization: Bearer {token}
```

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

### Logout
```
POST /auth/logout
```

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Logout successful",
  "data": null
}
```

> **Note:** This revokes the refresh token. The access token will remain valid until it expires. For immediate session invalidation, the client should also discard the access token locally.

### Logout from All Devices
```
POST /users/me/logout-all
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "message": "Logged out from all devices",
  "data": null
}
```

> **Note:** This revokes all refresh tokens for the current user, effectively logging them out from all devices.

### Get Current User
```
GET /auth/me
Authorization: Bearer {token}
```

---

## 2. Courier Registration

### Register as Courier

> **Prerequisite:** User must have CONSUMER role first

```
POST /couriers/register
Authorization: Bearer {token}
```

**Request:**
```json
{
  "vehicleType": "MOTORCYCLE",
  "vehicleNumber": "01A123BC",
  "licenseNumber": "DL123456789",
  "preferredRadiusKm": 10
}
```

**Vehicle Types:** `BICYCLE`, `MOTORCYCLE`, `CAR`

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 5,
    "userName": "Alex Courier",
    "email": "courier@fooddelivery.com",
    "phone": "+998901234567",
    "status": "PENDING_APPROVAL",
    "vehicleType": "MOTORCYCLE",
    "vehicleNumber": "01A123BC",
    "verified": false,
    "currentOrderCount": 0,
    "totalDeliveries": 0,
    "averageRating": null
  }
}
```

### Get My Courier Profile

> **Required Role:** COURIER

```
GET /couriers/me
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 5,
    "userName": "Alex Courier",
    "email": "courier@fooddelivery.com",
    "phone": "+998901234567",
    "status": "AVAILABLE",
    "vehicleType": "MOTORCYCLE",
    "vehicleNumber": "01A123BC",
    "currentLat": 41.2995,
    "currentLng": 69.2401,
    "verified": true,
    "verifiedAt": "2024-01-10T10:00:00Z",
    "currentOrderCount": 1,
    "maxConcurrentOrders": 3,
    "totalDeliveries": 156,
    "averageRating": 4.8,
    "todayEarnings": 125000,
    "weeklyEarnings": 850000
  }
}
```

### Update Courier Profile
```
PUT /couriers/me
Authorization: Bearer {token}
```

**Request:**
```json
{
  "vehicleType": "CAR",
  "vehicleNumber": "01A456DE",
  "preferredRadiusKm": 15
}
```

**Response:**
```json
{
  "success": true,
  "message": "Profile updated",
  "data": {
    "id": 1,
    "userId": 5,
    "userName": "Alex Courier",
    "email": "courier@fooddelivery.com",
    "phone": "+998901234567",
    "status": "AVAILABLE",
    "vehicleType": "CAR",
    "vehicleNumber": "01A456DE",
    "currentLat": 41.2995,
    "currentLng": 69.2401,
    "preferredRadiusKm": 15,
    "verified": true,
    "verifiedAt": "2024-01-10T10:00:00Z",
    "currentOrderCount": 0,
    "maxConcurrentOrders": 3,
    "totalDeliveries": 156,
    "averageRating": 4.8
  }
}
```

---

## 3. Status Management

> **Required Role:** COURIER

### Update Status (Go Online/Offline)
```
PUT /couriers/me/status
Authorization: Bearer {token}
```

**Request:**
```json
{
  "status": "AVAILABLE"
}
```

**Status Values:**
| Status | Description |
|--------|-------------|
| `OFFLINE` | Not accepting orders |
| `AVAILABLE` | Ready to accept orders |
| `BUSY` | At max capacity |
| `ON_BREAK` | Temporary break |

**Response:**
```json
{
  "success": true,
  "message": "Status updated",
  "data": {
    "id": 1,
    "userId": 5,
    "userName": "Alex Courier",
    "email": "courier@fooddelivery.com",
    "phone": "+998901234567",
    "status": "AVAILABLE",
    "vehicleType": "MOTORCYCLE",
    "vehicleNumber": "01A123BC",
    "currentLat": 41.2995,
    "currentLng": 69.2401,
    "verified": true,
    "verifiedAt": "2024-01-10T10:00:00Z",
    "currentOrderCount": 0,
    "maxConcurrentOrders": 3,
    "totalDeliveries": 156,
    "averageRating": 4.8
  }
}
```

### Courier Status Flow

```
                    ┌──────────────┐
                    │   OFFLINE    │
                    │ (Not working)│
                    └──────┬───────┘
                           │ Go Online
                           ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   ON_BREAK   │◄──►│  AVAILABLE   │◄──►│     BUSY     │
│  (Paused)    │    │(Ready for    │    │(Max orders)  │
└──────────────┘    │   orders)    │    └──────────────┘
                    └──────────────┘
                           │ Go Offline
                           ▼
                    ┌──────────────┐
                    │   OFFLINE    │
                    └──────────────┘
```

---

## 4. Location Updates

> **Required Role:** COURIER

### Update Current Location
```
PUT /couriers/me/location
Authorization: Bearer {token}
```

**Request:**
```json
{
  "latitude": 41.3001,
  "longitude": 69.2450,
  "accuracy": 10.5,
  "heading": 180,
  "speed": 25.5
}
```

**Response:**
```json
{
  "success": true,
  "message": "Location updated",
  "data": null
}
```

### Best Practices for Location Updates

| Scenario | Update Frequency |
|----------|------------------|
| Idle (Available) | Every 30 seconds |
| Active Delivery | Every 5-10 seconds |
| Moving Fast | Every 3-5 seconds |

---

## 5. Order Management

> **Required Role:** COURIER

### Get Available Orders (Nearby)
```
GET /couriers/me/available-orders
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| lat | decimal | Current latitude |
| lng | decimal | Current longitude |
| radiusKm | decimal | Search radius (default: from profile) |

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "orderId": 456,
      "externalOrderNo": "ORD-2024-0456",
      "restaurantId": 1,
      "restaurantName": "Pizza Palace",
      "restaurantAddress": "123 Main Street",
      "restaurantLat": 41.2995,
      "restaurantLng": 69.2401,
      "deliveryAddress": "456 Elm Street, Apt 5A",
      "deliveryLat": 41.3112,
      "deliveryLng": 69.2797,
      "customerName": "John D.",
      "customerPhone": "+998907654321",
      "deliveryInstructions": "Ring doorbell twice",
      "status": "READY",
      "deliveryFee": 15000,
      "tipAmount": 3000,
      "total": 125000,
      "itemCount": 3,
      "createdAt": "2024-01-15T12:30:00Z",
      "readyAt": "2024-01-15T12:45:00Z",
      "pickedUpAt": null,
      "deliveredAt": null
    }
  ]
}
```

### Accept Order
```
POST /couriers/me/orders/{orderId}/accept
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "message": "Order accepted",
  "data": {
    "orderId": 456,
    "externalOrderNo": "ORD-2024-0456",
    "restaurantId": 1,
    "restaurantName": "Pizza Palace",
    "restaurantAddress": "123 Main Street",
    "restaurantLat": 41.2995,
    "restaurantLng": 69.2401,
    "deliveryAddress": "456 Elm Street, Apt 5A",
    "deliveryLat": 41.3112,
    "deliveryLng": 69.2797,
    "customerName": "John D.",
    "customerPhone": "+998907654321",
    "deliveryInstructions": "Ring doorbell twice",
    "status": "COURIER_ASSIGNED",
    "deliveryFee": 15000,
    "tipAmount": 3000,
    "total": 125000,
    "itemCount": 3,
    "createdAt": "2024-01-15T12:30:00Z",
    "readyAt": "2024-01-15T12:45:00Z",
    "courierAssignedAt": "2024-01-15T12:47:00Z",
    "pickedUpAt": null,
    "deliveredAt": null
  }
}
```

### Get My Active Orders
```
GET /couriers/me/orders/active
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "orderId": 456,
      "externalOrderNo": "ORD-2024-0456",
      "restaurantId": 1,
      "restaurantName": "Pizza Palace",
      "restaurantAddress": "123 Main Street",
      "restaurantLat": 41.2995,
      "restaurantLng": 69.2401,
      "deliveryAddress": "456 Elm Street, Apt 5A",
      "deliveryLat": 41.3112,
      "deliveryLng": 69.2797,
      "customerName": "John D.",
      "customerPhone": "+998907654321",
      "deliveryInstructions": "Ring doorbell twice",
      "status": "COURIER_ASSIGNED",
      "deliveryFee": 15000,
      "tipAmount": 3000,
      "total": 125000,
      "itemCount": 3,
      "createdAt": "2024-01-15T12:30:00Z",
      "readyAt": "2024-01-15T12:45:00Z",
      "courierAssignedAt": "2024-01-15T12:47:00Z",
      "pickedUpAt": null,
      "deliveredAt": null
    }
  ]
}
```

### Get Order Details
```
GET /couriers/me/orders/{orderId}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "orderId": 456,
    "externalOrderNo": "ORD-2024-0456",
    "restaurantId": 1,
    "restaurantName": "Pizza Palace",
    "restaurantAddress": "123 Main Street",
    "restaurantLat": 41.2995,
    "restaurantLng": 69.2401,
    "deliveryAddress": "456 Elm Street, Apt 5A",
    "deliveryLat": 41.3112,
    "deliveryLng": 69.2797,
    "customerName": "John D.",
    "customerPhone": "+998907654321",
    "deliveryInstructions": "Ring doorbell twice",
    "status": "PICKED_UP",
    "deliveryFee": 15000,
    "tipAmount": 3000,
    "total": 125000,
    "itemCount": 3,
    "createdAt": "2024-01-15T12:30:00Z",
    "readyAt": "2024-01-15T12:45:00Z",
    "pickedUpAt": "2024-01-15T12:50:00Z",
    "deliveredAt": null
  }
}
```

### Update Order Status

#### Picked Up from Restaurant
```
PUT /couriers/me/orders/{orderId}/pickup
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "message": "Order picked up",
  "data": {
    "orderId": 456,
    "externalOrderNo": "ORD-2024-0456",
    "restaurantId": 1,
    "restaurantName": "Pizza Palace",
    "restaurantAddress": "123 Main Street",
    "restaurantLat": 41.2995,
    "restaurantLng": 69.2401,
    "deliveryAddress": "456 Elm Street, Apt 5A",
    "deliveryLat": 41.3112,
    "deliveryLng": 69.2797,
    "customerName": "John D.",
    "customerPhone": "+998907654321",
    "deliveryInstructions": "Ring doorbell twice",
    "status": "PICKED_UP",
    "deliveryFee": 15000,
    "tipAmount": 3000,
    "total": 125000,
    "itemCount": 3,
    "createdAt": "2024-01-15T12:30:00Z",
    "readyAt": "2024-01-15T12:45:00Z",
    "pickedUpAt": "2024-01-15T12:50:00Z",
    "deliveredAt": null
  }
}
```

#### Start Transit
```
PUT /couriers/me/orders/{orderId}/transit
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "message": "In transit to customer",
  "data": {
    "orderId": 456,
    "externalOrderNo": "ORD-2024-0456",
    "restaurantId": 1,
    "restaurantName": "Pizza Palace",
    "restaurantAddress": "123 Main Street",
    "restaurantLat": 41.2995,
    "restaurantLng": 69.2401,
    "deliveryAddress": "456 Elm Street, Apt 5A",
    "deliveryLat": 41.3112,
    "deliveryLng": 69.2797,
    "customerName": "John D.",
    "customerPhone": "+998907654321",
    "deliveryInstructions": "Ring doorbell twice",
    "status": "IN_TRANSIT",
    "deliveryFee": 15000,
    "tipAmount": 3000,
    "total": 125000,
    "itemCount": 3,
    "createdAt": "2024-01-15T12:30:00Z",
    "readyAt": "2024-01-15T12:45:00Z",
    "pickedUpAt": "2024-01-15T12:50:00Z",
    "deliveredAt": null
  }
}
```

#### Complete Delivery
```
POST /couriers/me/orders/{orderId}/complete
Authorization: Bearer {token}
```

**Request (optional):**
```json
{
  "deliveryPhoto": "base64_encoded_image",
  "deliveryNotes": "Left at door as requested"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Delivery completed",
  "data": {
    "orderId": 456,
    "externalOrderNo": "ORD-2024-0456",
    "restaurantId": 1,
    "restaurantName": "Pizza Palace",
    "restaurantAddress": "123 Main Street",
    "restaurantLat": 41.2995,
    "restaurantLng": 69.2401,
    "deliveryAddress": "456 Elm Street, Apt 5A",
    "deliveryLat": 41.3112,
    "deliveryLng": 69.2797,
    "customerName": "John D.",
    "customerPhone": "+998907654321",
    "deliveryInstructions": "Ring doorbell twice",
    "status": "DELIVERED",
    "deliveryFee": 15000,
    "tipAmount": 3000,
    "total": 125000,
    "itemCount": 3,
    "createdAt": "2024-01-15T12:30:00Z",
    "readyAt": "2024-01-15T12:45:00Z",
    "pickedUpAt": "2024-01-15T12:50:00Z",
    "deliveredAt": "2024-01-15T13:05:00Z"
  }
}
```

### Report Issue with Order
```
POST /couriers/me/orders/{orderId}/issue
Authorization: Bearer {token}
```

**Request:**
```json
{
  "issueType": "CUSTOMER_UNAVAILABLE",
  "description": "Customer not answering phone",
  "photos": ["base64_image_1"]
}
```

**Issue Types:**
- `CUSTOMER_UNAVAILABLE`
- `WRONG_ADDRESS`
- `RESTAURANT_DELAY`
- `ACCIDENT`
- `VEHICLE_ISSUE`
- `OTHER`

**Response:**
```json
{
  "success": true,
  "message": "Issue reported",
  "data": null
}
```

---

## 6. Earnings & History

> **Required Role:** COURIER

### Get Earnings Summary
```
GET /couriers/me/earnings
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| period | string | TODAY, THIS_WEEK, THIS_MONTH, CUSTOM |
| startDate | date | For CUSTOM period |
| endDate | date | For CUSTOM period |

**Response:**
```json
{
  "success": true,
  "data": {
    "todayEarnings": 125000,
    "weekEarnings": 850000,
    "monthEarnings": 3200000,
    "totalEarnings": 15600000,
    "todayDeliveries": 7,
    "weekDeliveries": 45,
    "monthDeliveries": 180,
    "totalDeliveries": 850,
    "averagePerDelivery": 18353,
    "pendingPayout": 250000
  }
}
```

### Get Delivery History
```
GET /couriers/me/orders/history
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (0-indexed) |
| size | int | Page size (default: 20) |

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "orderId": 455,
        "externalOrderNo": "ORD-2024-0455",
        "restaurantId": 2,
        "restaurantName": "Burger House",
        "restaurantAddress": "789 Oak Avenue",
        "restaurantLat": 41.3050,
        "restaurantLng": 69.2500,
        "deliveryAddress": "321 Pine Street",
        "deliveryLat": 41.3200,
        "deliveryLng": 69.2650,
        "customerName": "Jane S.",
        "customerPhone": "+998909876543",
        "deliveryInstructions": null,
        "status": "DELIVERED",
        "deliveryFee": 12000,
        "tipAmount": 5000,
        "total": 85000,
        "itemCount": 2,
        "createdAt": "2024-01-14T18:30:00Z",
        "readyAt": "2024-01-14T18:45:00Z",
        "pickedUpAt": "2024-01-14T18:50:00Z",
        "deliveredAt": "2024-01-14T19:05:00Z"
      },
      {
        "orderId": 450,
        "externalOrderNo": "ORD-2024-0450",
        "restaurantId": 1,
        "restaurantName": "Pizza Palace",
        "restaurantAddress": "123 Main Street",
        "restaurantLat": 41.2995,
        "restaurantLng": 69.2401,
        "deliveryAddress": "555 Maple Drive",
        "deliveryLat": 41.2900,
        "deliveryLng": 69.2300,
        "customerName": "Mike R.",
        "customerPhone": "+998901112233",
        "deliveryInstructions": "Leave at door",
        "status": "DELIVERED",
        "deliveryFee": 18000,
        "tipAmount": 2000,
        "total": 145000,
        "itemCount": 5,
        "createdAt": "2024-01-14T12:00:00Z",
        "readyAt": "2024-01-14T12:20:00Z",
        "pickedUpAt": "2024-01-14T12:25:00Z",
        "deliveredAt": "2024-01-14T12:45:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 156,
    "totalPages": 8,
    "last": false
  }
}
```

### Get Single Delivery Details
```
GET /couriers/me/orders/{orderId}
Authorization: Bearer {token}
```

---

## 7. Notifications

### Get My Notifications
```
GET /notifications/me
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| role | string | auto-detected | Filter by role (`COURIER`). Auto-detected from JWT if not provided. |
| isRead | boolean | null | `true` = read only, `false` = unread only, null = all |
| category | string | null | Filter by category (e.g. `ORDER`, `SYSTEM`) |
| page | int | 0 | Page number (0-indexed) |
| pageSize | int | 20 | Results per page |

**Response:**
```json
{
  "notifications": [
    {
      "id": 123,
      "userId": 5,
      "role": "COURIER",
      "title": "New order nearby",
      "message": "A new order is available for pickup near your location",
      "category": "ORDER",
      "read": false,
      "readAt": null,
      "dismissed": false,
      "createdAt": "2024-01-15T12:55:00Z"
    }
  ],
  "page": 0,
  "pageSize": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

### Get Unread Count
```
GET /notifications/unread/count?role=COURIER
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| role | string | auto-detected | Filter by role (`COURIER`). Auto-detected from JWT if not provided. |

**Response:**
```json
{
  "unreadCount": 5
}
```

### Get Notification Counts
```
GET /notifications/user/{userId}/counts?role=COURIER
Authorization: Bearer {token}
```

Returns total, unread, and per-category counts.

### Mark as Read
```
PATCH /notifications/{id}/read
Authorization: Bearer {token}
```

**Response:**
```json
{
  "id": 123,
  "userId": 5,
  "role": "COURIER",
  "title": "New order nearby",
  "message": "A new order is available for pickup near your location",
  "category": "ORDER",
  "read": true,
  "readAt": "2024-01-15T13:00:00Z",
  "dismissed": false,
  "createdAt": "2024-01-15T12:55:00Z"
}
```

### Mark All as Read
```
PATCH /notifications/read-all?userId={userId}&role=COURIER
Authorization: Bearer {token}
```

**Response:**
```json
{
  "status": "success",
  "markedCount": 12
}
```

### Mark Batch as Read
```
PATCH /notifications/read-batch
Authorization: Bearer {token}
```

**Request:**
```json
[123, 124, 125]
```

**Response:**
```json
{
  "status": "success",
  "markedCount": 3
}
```

### Dismiss Notification
```
PATCH /notifications/{id}/dismiss
Authorization: Bearer {token}
```

Soft-deletes a notification. Dismissed notifications are excluded from queries by default.

### Bulk Action
```
POST /notifications/bulk-action
Authorization: Bearer {token}
```

**Request:**
```json
{
  "notificationIds": [123, 124, 125],
  "action": "MARK_READ"
}
```

**Actions:** `MARK_READ`, `DISMISS`, `DELETE`

**Response:**
```json
{
  "status": "success",
  "affectedCount": 3
}
```

### Notification Types for Courier

| Type | Description |
|------|-------------|
| `NEW_ORDER_NEARBY` | New order available in your area |
| `NEW_DELIVERY_AVAILABLE` | New delivery assignment via push notification |
| `ORDER_ASSIGNED` | Order assigned to you |
| `ORDER_CANCELLED` | Order was cancelled |
| `PAYOUT_ISSUED` | Payout sent to your account |
| `VERIFICATION_APPROVED` | Courier profile verified |
| `RATING_RECEIVED` | New rating from customer |

---

## 8. WebSocket (Real-time)

### Connection
```
ws://localhost:8080/ws
```

**STOMP CONNECT Headers:**
```
Authorization: Bearer {accessToken}
```

### Subscribe to Channels

```javascript
const stompClient = Stomp.over(new SockJS('/ws'));

stompClient.connect(
  { 'Authorization': 'Bearer ' + accessToken },
  function(frame) {
    // New available orders (broadcast to ALL online couriers)
    stompClient.subscribe('/topic/couriers/orders/available', function(message) {
      const order = JSON.parse(message.body);
      showNewOrderNotification(order);
    });

    // New orders targeted to this courier specifically
    stompClient.subscribe('/user/queue/orders/new', function(message) {
      const order = JSON.parse(message.body);
      showNewOrderNotification(order);
    });

    // Order updates for assigned orders
    stompClient.subscribe('/topic/orders/{orderId}/status', function(message) {
      const status = JSON.parse(message.body);
      updateOrderStatus(status);
    });

    // All courier role notifications (order ready, new delivery available, etc.)
    stompClient.subscribe('/topic/roles/courier/notifications', function(message) {
      const notification = JSON.parse(message.body);
      showNotification(notification);
    });

    // Personal notifications for this user
    stompClient.subscribe('/topic/users/' + userId + '/notifications', function(message) {
      const notification = JSON.parse(message.body);
      showNotification(notification);
    });
  }
);
```

### Topics for Courier

| Topic | Description |
|-------|-------------|
| `/topic/couriers/orders/available` | **New available orders broadcast to all couriers** |
| `/user/queue/orders/new` | New order targeted to this courier specifically |
| `/topic/orders/{orderId}/status` | Status updates for assigned orders |
| `/topic/roles/courier/notifications` | All courier role notifications (order ready, assignments, etc.) |
| `/topic/users/{userId}/notifications` | Personal notifications for this user |

### New Order Notification Format

```json
{
  "type": "NEW_ORDER",
  "orderId": 456,
  "orderNumber": "ORD-2024-0456",
  "restaurant": {
    "name": "Pizza Palace",
    "distance": 1.2
  },
  "deliveryDistance": 3.5,
  "estimatedEarnings": 18000,
  "expiresAt": "2024-01-15T12:35:00Z"
}
```

---

## Delivery Flow

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              COURIER DELIVERY FLOW                                       │
└─────────────────────────────────────────────────────────────────────────────────────────┘

  1. GO ONLINE                    2. RECEIVE ORDER               3. ACCEPT ORDER
  ┌──────────────┐               ┌──────────────┐               ┌──────────────┐
  │  Set Status  │──────────────▶│  New Order   │──────────────▶│   Accept     │
  │  AVAILABLE   │               │  Notification│               │   Order      │
  └──────────────┘               └──────────────┘               └──────────────┘
                                                                       │
  ┌────────────────────────────────────────────────────────────────────┘
  │  Status: COURIER_ASSIGNED
  ▼
  4. NAVIGATE TO RESTAURANT       5. PICK UP ORDER               6. START DELIVERY
  ┌──────────────┐               ┌──────────────┐               ┌──────────────┐
  │  Navigate    │──────────────▶│  Confirm     │──────────────▶│  Navigate    │
  │  to Pickup   │               │  Pickup      │               │  to Customer │
  └──────────────┘               └──────────────┘               └──────────────┘
                                  Status: PICKED_UP              Status: IN_TRANSIT
                                                                       │
  ┌────────────────────────────────────────────────────────────────────┘
  │
  ▼
  7. ARRIVE AT DESTINATION        8. COMPLETE DELIVERY
  ┌──────────────┐               ┌──────────────┐
  │  Contact     │──────────────▶│  Mark as     │
  │  Customer    │               │  Delivered   │
  └──────────────┘               └──────────────┘
                                  Status: DELIVERED
```

### Order Status Flow (Courier Perspective)

```
READY ──► COURIER_ASSIGNED ──► PICKED_UP ──► IN_TRANSIT ──► DELIVERED
           (Courier accepts)   (At restaurant)  (Driving)   (Complete)
```

| Action | Endpoint | Result Status |
|--------|----------|---------------|
| Accept Order | `POST /couriers/me/orders/{id}/accept` | `COURIER_ASSIGNED` |
| Pick Up | `PUT /couriers/me/orders/{id}/pickup` | `PICKED_UP` |
| Start Transit | `PUT /couriers/me/orders/{id}/transit` | `IN_TRANSIT` |
| Complete | `POST /couriers/me/orders/{id}/complete` | `DELIVERED` |

---

## Courier Logout Flow

When a courier wants to end their session, they should follow this sequence to ensure proper state management:

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              COURIER LOGOUT FLOW                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘

  1. CHECK ACTIVE ORDERS           2. GO OFFLINE                  3. LOGOUT
  ┌──────────────┐               ┌──────────────┐               ┌──────────────┐
  │  Complete or │──────────────▶│  Set Status  │──────────────▶│    POST      │
  │  Cancel      │               │   OFFLINE    │               │ /auth/logout │
  └──────────────┘               └──────────────┘               └──────────────┘
         │                              │                              │
         │                              │                              │
         ▼                              ▼                              ▼
  ┌──────────────┐               ┌──────────────┐               ┌──────────────┐
  │ No pending   │               │ Stop location│               │ Clear local  │
  │ deliveries   │               │ updates      │               │ tokens       │
  └──────────────┘               └──────────────┘               └──────────────┘
```

### Recommended Logout Sequence

1. **Check for Active Orders**
   ```
   GET /couriers/me/orders/active
   ```
   - If there are active orders, prompt the courier to complete or cancel them first
   - Couriers should not go offline with active deliveries

2. **Go Offline**
   ```
   PUT /couriers/me/status
   {"status": "OFFLINE"}
   ```
   - This stops new order notifications
   - Removes courier from available pool

3. **Stop Location Updates**
   - Stop the background location service
   - This saves battery and data

4. **Disconnect WebSocket**
   ```javascript
   stompClient.disconnect();
   ```

5. **Logout from Server**
   ```
   POST /auth/logout
   {"refreshToken": "eyJhbGciOiJIUzI1NiIs..."}
   ```

6. **Clear Local Storage**
   - Remove access token
   - Remove refresh token
   - Clear cached data

### Quick Logout (Emergency)

For emergency situations where immediate logout is needed:

```
POST /users/me/logout-all
Authorization: Bearer {token}
```

This will:
- Revoke all refresh tokens across all devices
- Force re-authentication on next app open

> **Warning:** Using logout-all will also log out the courier from any other devices they may be using.

---

## Navigation Integration

### Get Navigation URL

The app can generate navigation URLs for popular map apps:

**Google Maps:**
```
https://www.google.com/maps/dir/?api=1&destination={lat},{lng}
```

**Yandex Maps:**
```
https://yandex.com/maps/?rtext=~{lat},{lng}&rtt=auto
```

**Apple Maps:**
```
maps://maps.apple.com/?daddr={lat},{lng}
```

---

## Error Responses

```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

### Common Errors

| Code | Message | Description |
|------|---------|-------------|
| 400 | "Courier must be verified before going online" | Need verification |
| 400 | "Order already has a courier assigned" | Order taken |
| 400 | "Order must be ready before courier assignment" | Order not ready |
| 403 | "This order is not assigned to you" | Wrong courier |
| 409 | "Courier is not available to accept orders" | Status conflict |

---

## Test Accounts

| Email | Role | Password | Status |
|-------|------|----------|--------|
| courier@fooddelivery.com | COURIER | password | Verified |

---

## Performance Tips

### Battery Optimization

1. Use efficient location tracking (GPS vs Network)
2. Batch location updates when idle
3. Reduce update frequency when stationary

### Network Optimization

1. Cache restaurant/order data locally
2. Use WebSocket for real-time updates (less battery than polling)
3. Retry failed requests with exponential backoff

### Recommended Update Intervals

| State | Location Update | API Poll |
|-------|-----------------|----------|
| Offline | None | None |
| Available (Idle) | 30s | WebSocket only |
| Active Delivery | 5-10s | WebSocket only |
| Background | 60s | None |
