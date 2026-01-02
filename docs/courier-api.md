# Courier API Documentation

Courier management and delivery operations for the Food Delivery Platform.

## Base URL

```
/api/v1/couriers
```

---

## Table of Contents

1. [Registration](#1-registration)
2. [Profile Management](#2-profile-management)
3. [Status & Location](#3-status--location)
4. [Order Operations](#4-order-operations)
5. [Admin Operations](#5-admin-operations)
6. [Enumerations](#enumerations)

---

## 1. Registration

### Register as Courier

**POST** `/register`

Register the current authenticated user as a courier.

**Request Body:**
```json
{
  "vehicleType": "MOTORCYCLE",
  "vehicleNumber": "01A123BC",
  "licenseNumber": "DL12345678",
  "preferredRadiusKm": 10
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| vehicleType | VehicleType | Yes | Type of vehicle |
| vehicleNumber | string | No | Vehicle registration number |
| licenseNumber | string | No | Driver's license number |
| preferredRadiusKm | int | No | Preferred delivery radius in km |

**Response (201):**
```json
{
  "success": true,
  "message": "Courier registration submitted",
  "data": {
    "id": 1,
    "userId": 100,
    "vehicleType": "MOTORCYCLE",
    "vehicleNumber": "01A123BC",
    "licenseNumber": "DL12345678",
    "status": "PENDING_APPROVAL",
    "isVerified": false,
    "rating": 0.0,
    "totalDeliveries": 0,
    "currentLatitude": null,
    "currentLongitude": null,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

**Note:** After registration, courier status is `PENDING_APPROVAL` until verified by admin.

---

## 2. Profile Management

### Get My Courier Profile

**GET** `/me`

Get the courier profile of the currently authenticated user.

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 100,
    "vehicleType": "MOTORCYCLE",
    "vehicleNumber": "01A123BC",
    "status": "AVAILABLE",
    "isVerified": true,
    "rating": 4.8,
    "totalDeliveries": 150,
    "currentLatitude": 41.2995,
    "currentLongitude": 69.2401,
    "currentOrderCount": 1,
    "maxConcurrentOrders": 3,
    "preferredRadiusKm": 10,
    "lastLocationUpdate": "2024-01-15T10:30:00",
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

**Roles:** COURIER

---

## 3. Status & Location

### Update Status

**PATCH** `/me/status`

Update courier availability status (go online/offline).

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | CourierStatus | Yes | New status |

**Example:**
```
PATCH /couriers/me/status?status=AVAILABLE
```

**Response (200):**
```json
{
  "success": true,
  "message": "Status updated",
  "data": {
    "id": 1,
    "status": "AVAILABLE",
    ...
  }
}
```

**Roles:** COURIER

---

### Update Location

**POST** `/me/location`

Update courier's current GPS location.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| lat | BigDecimal | Yes | Latitude |
| lng | BigDecimal | Yes | Longitude |

**Example:**
```
POST /couriers/me/location?lat=41.2995&lng=69.2401
```

**Response (200):**
```json
{
  "success": true,
  "message": "Location updated"
}
```

**Best Practices:**
- Call every 10-30 seconds while courier is online
- Use high-accuracy GPS mode on mobile devices
- Include heading/bearing if available for better ETA

**Roles:** COURIER

---

## 4. Order Operations

### Accept Order

**POST** `/{courierId}/accept/{orderId}`

Accept an order assignment.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| courierId | Long | Courier ID |
| orderId | Long | Order ID |

**Response (200):**
```json
{
  "success": true,
  "message": "Order accepted",
  "data": {
    "id": 1,
    "status": "BUSY",
    "currentOrderCount": 2,
    ...
  }
}
```

**Roles:** COURIER

---

### Complete Delivery

**POST** `/{courierId}/complete/{orderId}`

Mark a delivery as completed.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| courierId | Long | Courier ID |
| orderId | Long | Order ID |

**Response (200):**
```json
{
  "success": true,
  "message": "Delivery completed",
  "data": {
    "id": 1,
    "status": "AVAILABLE",
    "currentOrderCount": 0,
    "totalDeliveries": 151,
    ...
  }
}
```

**Roles:** COURIER

---

## 5. Admin Operations

### Find Available Couriers

**GET** `/available`

Find available couriers near a location for order assignment.

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| lat | BigDecimal | Yes | - | Latitude of pickup location |
| lng | BigDecimal | Yes | - | Longitude of pickup location |
| radius | double | No | 5 | Search radius in kilometers |

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 100,
      "vehicleType": "MOTORCYCLE",
      "status": "AVAILABLE",
      "rating": 4.8,
      "currentLatitude": 41.3000,
      "currentLongitude": 69.2410,
      "distanceKm": 0.5
    },
    {
      "id": 2,
      "userId": 101,
      "vehicleType": "BICYCLE",
      "status": "AVAILABLE",
      "rating": 4.5,
      "currentLatitude": 41.2980,
      "currentLongitude": 69.2380,
      "distanceKm": 0.8
    }
  ]
}
```

**Roles:** PLATFORM, ADMIN, RESTAURANT_OWNER

---

### Get All Couriers

**GET** `/`

Get all couriers with pagination.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size |

**Response (200):**
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

**Roles:** PLATFORM, ADMIN

---

### Verify Courier

**POST** `/{courierId}/verify`

Verify a courier after document review.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| courierId | Long | Courier ID to verify |

**Response (200):**
```json
{
  "success": true,
  "message": "Courier verified",
  "data": {
    "id": 1,
    "status": "OFFLINE",
    "isVerified": true,
    ...
  }
}
```

**Roles:** PLATFORM, ADMIN

---

## Enumerations

### CourierStatus

| Value | Description |
|-------|-------------|
| `OFFLINE` | Not accepting orders |
| `AVAILABLE` | Ready to accept orders |
| `BUSY` | At maximum concurrent orders |
| `ON_BREAK` | Temporarily unavailable |
| `SUSPENDED` | Account suspended by admin |
| `PENDING_APPROVAL` | Awaiting verification |

### VehicleType

| Value | Description | Typical Range |
|-------|-------------|---------------|
| `WALKING` | On foot | 1-2 km |
| `BICYCLE` | Regular bicycle | 3-5 km |
| `E_BIKE` | Electric bike/scooter | 5-8 km |
| `MOTORCYCLE` | Motorcycle | 10-15 km |
| `CAR` | Car | 15-25 km |
| `VAN` | Van (large orders) | 20-30 km |

---

## Courier Lifecycle

```
1. User registers as courier
   └── Status: PENDING_APPROVAL

2. Admin verifies documents
   └── Status: OFFLINE (verified)

3. Courier goes online
   └── Status: AVAILABLE

4. Courier accepts order
   └── Status: BUSY (if at max orders)

5. Courier completes delivery
   └── Status: AVAILABLE (if no pending orders)

6. Courier goes offline
   └── Status: OFFLINE
```

---

## Location Tracking

### Real-time Updates

Couriers should update their location regularly:

```
While online:
  - Update every 10 seconds when moving
  - Update every 30 seconds when stationary
  - Update immediately on significant movement (>50m)
```

### Location Accuracy

| GPS Accuracy | Recommendation |
|--------------|----------------|
| < 10m | Excellent - use as-is |
| 10-50m | Good - acceptable |
| 50-100m | Fair - wait for better fix |
| > 100m | Poor - don't update |

---

## Delivery Workflow

```
Order Assigned
    │
    ▼
Accept Order ──────────► Order Rejected
    │                      (by courier)
    ▼
Pick Up at Restaurant
    │
    ▼
In Transit to Customer
    │
    ▼
Complete Delivery ─────► Customer Not Available
    │                      (retry or cancel)
    ▼
Ready for Next Order
```

---

## Error Responses

### 404 Courier Not Found

```json
{
  "success": false,
  "message": "Courier not found"
}
```

### 400 Invalid Status Transition

```json
{
  "success": false,
  "message": "Cannot change status from SUSPENDED to AVAILABLE"
}
```

### 422 Order Already Accepted

```json
{
  "success": false,
  "message": "Order already assigned to another courier"
}
```
