# Consumer App API Documentation

API endpoints for the **Consumer App** (Mobile + Web) frontend application.

**Target Users:** CONSUMER role

**Base URL:** `http://localhost:8080/api/v1`

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Profile Management](#2-profile-management)
3. [Browse Restaurants](#3-browse-restaurants)
4. [Menu & Products](#4-menu--products)
5. [Orders](#5-orders)
6. [Payments](#6-payments)
7. [Order Tracking](#7-order-tracking)
8. [Reviews & Ratings](#8-reviews--ratings)
9. [Referral Program](#9-referral-program)
10. [Notifications](#10-notifications)
11. [WebSocket (Real-time)](#11-websocket-real-time)

---

## 1. Authentication

### Email/Password Registration
```
POST /auth/register
```

**Request:**
```json
{
  "email": "john@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe",
  "phone": "+998901234567",
  "role": "CONSUMER"
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
      "id": 10,
      "email": "john@example.com",
      "fullName": "John Doe",
      "roles": ["CONSUMER"]
    }
  }
}
```

### Email/Password Login
```
POST /auth/login
```

**Request:**
```json
{
  "emailOrPhone": "john@example.com",
  "password": "SecurePass123!"
}
```

### Phone OTP Authentication

#### Request OTP
```
POST /auth/phone/request-otp
```

**Request:**
```json
{
  "phone": "+998901234567"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": {
    "expiresIn": 300,
    "retryAfter": 60
  }
}
```

#### Verify OTP
```
POST /auth/phone/verify-otp
```

**Request:**
```json
{
  "phone": "+998901234567",
  "otp": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "isNewUser": false
  }
}
```

#### Complete Phone Registration (for new users)
```
POST /auth/phone/complete-registration
```

**Request:**
```json
{
  "phone": "+998901234567",
  "otp": "123456",
  "fullName": "John Doe",
  "email": "john@example.com"
}
```

### Refresh Token
```
POST /auth/refresh
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

> **Note:** This revokes the refresh token. The access token will remain valid until it expires. For immediate session invalidation, discard the access token locally.

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

### Get Current User
```
GET /auth/me
Authorization: Bearer {token}
```

---

## 2. Profile Management

> **Required Role:** CONSUMER

### Get My Profile
```
GET /consumers/profile
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 10,
    "email": "john@example.com",
    "fullName": "John Doe",
    "phone": "+998901234567",
    "avatarUrl": "/images/avatars/user10.jpg",
    "defaultAddress": {
      "id": 1,
      "label": "Home",
      "fullAddress": "123 Main St, Apt 4B",
      "latitude": 41.2995,
      "longitude": 69.2401
    },
    "totalOrders": 25,
    "memberSince": "2023-06-15"
  }
}
```

### Update Profile
```
PUT /consumers/profile
Authorization: Bearer {token}
```

**Request:**
```json
{
  "fullName": "John D. Doe",
  "email": "johndoe@example.com"
}
```

### Saved Addresses

#### Get Addresses
```
GET /consumers/addresses
Authorization: Bearer {token}
```

#### Add Address
```
POST /consumers/addresses
Authorization: Bearer {token}
```

**Request:**
```json
{
  "label": "Work",
  "fullAddress": "456 Business Center, Floor 5",
  "latitude": 41.3112,
  "longitude": 69.2797,
  "apartmentNumber": "505",
  "entrance": "B",
  "instructions": "Call when arrived"
}
```

#### Update Address
```
PUT /consumers/addresses/{addressId}
Authorization: Bearer {token}
```

#### Delete Address
```
DELETE /consumers/addresses/{addressId}
Authorization: Bearer {token}
```

#### Set Default Address
```
PUT /consumers/addresses/{addressId}/default
Authorization: Bearer {token}
```

---

## 3. Browse Restaurants

*Public endpoints - no authentication required*

### List Restaurants
```
GET /restaurants
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (default: 0) |
| size | int | Page size (default: 20) |
| lat | decimal | User latitude (for distance sorting) |
| lng | decimal | User longitude |
| cuisineType | string | Filter by cuisine (ITALIAN, ASIAN, etc.) |
| isOpen | boolean | Filter open restaurants only |
| minRating | decimal | Minimum rating filter |
| search | string | Search by name |
| sortBy | string | DISTANCE, RATING, DELIVERY_TIME, PRICE |

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Pizza Palace",
        "slug": "pizza-palace",
        "description": "Best pizza in town since 1995",
        "logoUrl": "/images/restaurants/pizza-palace.jpg",
        "coverImageUrl": "/images/restaurants/pizza-palace-cover.jpg",
        "phone": "+998901234567",
        "fullAddress": "123 Main Street, Tashkent",
        "latitude": 41.2995,
        "longitude": 69.2401,
        "status": "ACTIVE",
        "acceptsDelivery": true,
        "acceptsTakeaway": true,
        "acceptsDineIn": false,
        "minimumOrder": 30000,
        "deliveryFee": 15000,
        "deliveryRadiusKm": 10,
        "averagePrepTimeMinutes": 25,
        "opensAt": "09:00:00",
        "closesAt": "22:00:00",
        "isOpen": true,
        "isCurrentlyOpen": true,
        "averageRating": 4.5,
        "totalRatings": 234,
        "totalOrders": 1250
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3,
    "last": false
  }
}
```

### Get Restaurant Details
```
GET /restaurants/{id}
```
or
```
GET /restaurants/slug/{slug}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "ownerId": 3,
    "name": "Pizza Palace",
    "slug": "pizza-palace",
    "description": "Best pizza in town since 1995",
    "logoUrl": "/images/restaurants/pizza-palace.jpg",
    "coverImageUrl": "/images/restaurants/pizza-palace-cover.jpg",
    "phone": "+998901234567",
    "email": "contact@pizzapalace.com",
    "fullAddress": "123 Main Street, Tashkent",
    "addressLine1": "123 Main Street",
    "city": "Tashkent",
    "state": "Tashkent",
    "postalCode": "100000",
    "country": "Uzbekistan",
    "latitude": 41.2995,
    "longitude": 69.2401,
    "status": "ACTIVE",
    "featured": true,
    "acceptsDelivery": true,
    "acceptsTakeaway": true,
    "acceptsDineIn": false,
    "minimumOrder": 30000,
    "deliveryFee": 15000,
    "deliveryRadiusKm": 10,
    "averagePrepTimeMinutes": 25,
    "opensAt": "09:00:00",
    "closesAt": "22:00:00",
    "isOpen": true,
    "isCurrentlyOpen": true,
    "averageRating": 4.5,
    "totalRatings": 234,
    "totalOrders": 1250,
    "createdAt": "2023-01-15T10:00:00Z"
  }
}
```

### Search Restaurants
```
GET /restaurants/search?q={query}
```

### Get Nearby Restaurants
```
GET /restaurants/nearby?lat={lat}&lng={lng}&radiusKm={radius}
```

---

## 4. Menu & Products

*Public endpoints - no authentication required*

### Get Full Menu
```
GET /restaurants/{restaurantId}/menu
```

**Response:**
```json
{
  "success": true,
  "data": {
    "categories": [
      {
        "id": 1,
        "name": "Pizzas",
        "items": [
          {
            "id": 101,
            "name": "Margherita Pizza",
            "description": "Classic tomato sauce, mozzarella, basil",
            "price": 45000,
            "imageUrl": "/images/menu/margherita.jpg",
            "inStock": true,
            "vegetarian": true,
            "preparationTime": 15,
            "variants": [
              {"id": 1, "name": "Small", "price": 35000},
              {"id": 2, "name": "Medium", "price": 45000},
              {"id": 3, "name": "Large", "price": 55000}
            ],
            "options": [
              {
                "id": 1,
                "name": "Extra Toppings",
                "required": false,
                "multiSelect": true,
                "choices": [
                  {"id": 1, "name": "Extra Cheese", "price": 5000},
                  {"id": 2, "name": "Mushrooms", "price": 4000}
                ]
              }
            ]
          }
        ]
      }
    ]
  }
}
```

### Get Single Menu Item
```
GET /restaurants/{restaurantId}/menu/items/{itemId}
```

---

## 5. Orders

> **Required Role:** CONSUMER

### Create Order
```
POST /orders
Authorization: Bearer {token}
```

**Request:**
```json
{
  "restaurantId": 1,
  "orderType": "DELIVERY",
  "deliveryAddress": "123 Main St, Apt 4B, Tashkent",
  "deliveryLatitude": 41.2995,
  "deliveryLongitude": 69.2401,
  "deliveryInstructions": "Ring doorbell twice",
  "items": [
    {
      "menuItemId": 101,
      "quantity": 2,
      "variantId": 3,
      "optionIds": [1, 2],
      "specialInstructions": "No onions please"
    }
  ],
  "customerName": "John Doe",
  "customerPhone": "+998901234567",
  "notes": "Please include extra napkins",
  "tipAmount": 5000,
  "discountCode": "FIRST20"
}
```

**Order Types:** `DELIVERY`, `TAKEAWAY`, `DINE_IN`

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 456,
    "externalOrderNo": "ORD-2024-0456",
    "consumerId": 10,
    "consumerName": "John Doe",
    "restaurantId": 1,
    "restaurantName": "Pizza Palace",
    "courierId": null,
    "courierName": null,
    "orderType": "DELIVERY",
    "status": "CREATED",
    "paymentStatus": "PENDING",
    "items": [
      {
        "id": 1001,
        "menuItemId": 101,
        "menuItemName": "Margherita Pizza",
        "quantity": 2,
        "unitPrice": 45000,
        "totalPrice": 90000,
        "specialInstructions": "No onions please"
      }
    ],
    "subtotal": 110000,
    "tax": 11000,
    "deliveryFee": 15000,
    "discount": 22000,
    "tipAmount": 5000,
    "total": 119000,
    "deliveryAddress": "123 Main St, Apt 4B, Tashkent",
    "deliveryInstructions": "Ring doorbell twice",
    "customerName": "John Doe",
    "customerPhone": "+998901234567",
    "notes": "Please include extra napkins",
    "estimatedPrepTimeMinutes": 25,
    "estimatedDeliveryTime": "2024-01-15T13:30:00Z",
    "createdAt": "2024-01-15T12:30:00Z",
    "acceptedAt": null,
    "readyAt": null,
    "deliveredAt": null,
    "cancellationReason": null
  }
}
```

### Get My Orders
```
GET /orders/my
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number |
| size | int | Page size |
| status | string | Filter by status |

### Get Order Details
```
GET /orders/{orderId}
Authorization: Bearer {token}
```

### Cancel Order
```
PUT /orders/{orderId}/cancel
Authorization: Bearer {token}
```

**Request:**
```json
{
  "reason": "Changed my mind"
}
```

*Note: Can only cancel orders in CREATED or ACCEPTED status*

### Reorder (Create from previous order)
```
POST /orders/{orderId}/reorder
Authorization: Bearer {token}
```

---

## 6. Payments

> **Required Role:** CONSUMER

### Initiate Payment
```
POST /orders/{orderId}/pay
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "paymentIntentId": "pi_3abc4def5ghi",
    "clientSecret": "pi_3abc4def5ghi_secret_xyz",
    "amount": 103000,
    "currency": "UZS"
  }
}
```

### Confirm Payment
```
POST /orders/{orderId}/pay/confirm
Authorization: Bearer {token}
```

**Request:**
```json
{
  "paymentIntentId": "pi_3abc4def5ghi",
  "paymentMethodId": "pm_xxx"
}
```

### Get Payment Status
```
GET /orders/{orderId}/payment
Authorization: Bearer {token}
```

### Apply Promo Code
```
POST /orders/validate-promo
Authorization: Bearer {token}
```

**Request:**
```json
{
  "code": "FIRST20",
  "restaurantId": 1,
  "subtotal": 110000
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "valid": true,
    "discountType": "PERCENTAGE",
    "discountValue": 20,
    "discountAmount": 22000,
    "message": "20% off your first order!"
  }
}
```

---

## 7. Order Tracking

> **Required Role:** CONSUMER

### Get Live Order Status
```
GET /orders/{orderId}/track
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 456,
    "externalOrderNo": "ORD-2024-0456",
    "status": "IN_TRANSIT",
    "restaurantId": 1,
    "restaurantName": "Pizza Palace",
    "courierId": 5,
    "courierName": "Alex Courier",
    "deliveryAddress": "123 Main St, Apt 4B, Tashkent",
    "estimatedDeliveryTime": "2024-01-15T13:10:00Z",
    "createdAt": "2024-01-15T12:30:00Z",
    "acceptedAt": "2024-01-15T12:32:00Z",
    "readyAt": "2024-01-15T12:50:00Z"
  }
}
```

> **Note:** For real-time courier location tracking, subscribe to the WebSocket topic `/topic/couriers/{courierId}/location`.

---

## 8. Reviews & Ratings

> **Required Role:** CONSUMER

### Submit Review
```
POST /orders/{orderId}/review
Authorization: Bearer {token}
```

**Request:**
```json
{
  "restaurantRating": 5,
  "courierRating": 4,
  "foodRating": 5,
  "comment": "Great food, fast delivery!",
  "tags": ["FAST_DELIVERY", "TASTY_FOOD"]
}
```

### Get Restaurant Reviews
```
GET /restaurants/{restaurantId}/reviews
```

*Public endpoint*

---

## 9. Referral Program

> **Required Role:** CONSUMER

### Get My Referral Info
```
GET /referrals/my
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "referralCode": "JOHN2024",
    "referralLink": "https://app.fooddelivery.com/r/JOHN2024",
    "totalReferrals": 5,
    "earnedCredits": 50000,
    "pendingCredits": 10000
  }
}
```

### Apply Referral Code
```
POST /referrals/apply
Authorization: Bearer {token}
```

**Request:**
```json
{
  "code": "FRIEND2024"
}
```

---

## 10. Notifications

### Get My Notifications
```
GET /notifications/me
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| role | string | auto-detected | Filter by role (`CONSUMER`). Auto-detected from JWT if not provided. |
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
      "userId": 10,
      "role": "CONSUMER",
      "title": "Order Delivered",
      "message": "Your order #ORD-2024-0456 has been delivered",
      "category": "ORDER",
      "read": false,
      "readAt": null,
      "dismissed": false,
      "createdAt": "2024-01-15T13:10:00Z"
    }
  ],
  "page": 0,
  "pageSize": 20,
  "totalElements": 15,
  "totalPages": 1
}
```

### Get Unread Count
```
GET /notifications/unread/count
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| role | string | auto-detected | Filter by role (`CONSUMER`). Auto-detected from JWT if not provided. |

**Response:**
```json
{
  "unreadCount": 5
}
```

### Mark as Read
```
PATCH /notifications/{id}/read
Authorization: Bearer {token}
```

**Response:**
```json
{
  "id": 123,
  "userId": 10,
  "role": "CONSUMER",
  "title": "Order Delivered",
  "message": "Your order #ORD-2024-0456 has been delivered",
  "category": "ORDER",
  "read": true,
  "readAt": "2024-01-15T13:15:00Z",
  "dismissed": false,
  "createdAt": "2024-01-15T13:10:00Z"
}
```

### Mark All as Read
```
PATCH /notifications/read-all?userId={userId}&role=CONSUMER
Authorization: Bearer {token}
```

**Response:**
```json
{
  "status": "success",
  "markedCount": 8
}
```

### Dismiss Notification
```
PATCH /notifications/{id}/dismiss
Authorization: Bearer {token}
```

Soft-deletes a notification. Dismissed notifications are excluded from queries by default.

---

## 11. WebSocket (Real-time)

### Connection
```
ws://localhost:8080/ws
```

**STOMP CONNECT Headers:**
```
Authorization: Bearer {accessToken}
```

### Subscribe to Order Updates

```javascript
// Track specific order
stompClient.subscribe('/topic/orders/{orderId}/status', (message) => {
  const status = JSON.parse(message.body);
  console.log('Order status:', status);
});

// Track courier location
stompClient.subscribe('/topic/couriers/{courierId}/location', (message) => {
  const location = JSON.parse(message.body);
  console.log('Courier location:', location);
});

// Personal notifications
stompClient.subscribe('/user/queue/notifications', (message) => {
  const notification = JSON.parse(message.body);
  console.log('Notification:', notification);
});
```

### Topics for Consumer

| Topic | Description |
|-------|-------------|
| `/topic/orders/{orderId}/status` | Order status changes |
| `/topic/couriers/{courierId}/location` | Courier real-time location |
| `/user/queue/notifications` | Personal notifications |

### Courier Location Format

```json
{
  "courierId": 5,
  "lat": 41.3001,
  "lng": 69.2450,
  "timestamp": "2024-01-15T12:58:00Z"
}
```

---

## Order Status Flow (Consumer Perspective)

```
CREATED ──► ACCEPTED ──► PREPARING ──► READY ──► COURIER_ASSIGNED ──► PICKED_UP ──► IN_TRANSIT ──► DELIVERED ──► COMPLETED
    │                                                                                                    │
    │                                                                                                    └──► Rate & Review
    │
    └──► CANCELLED (by consumer or restaurant)
```

**Status Descriptions:**

| Status | Consumer View |
|--------|---------------|
| CREATED | Order placed, waiting for restaurant |
| ACCEPTED | Restaurant confirmed your order |
| PREPARING | Kitchen is preparing your food |
| READY | Food ready, waiting for courier |
| COURIER_ASSIGNED | Courier assigned, heading to restaurant |
| PICKED_UP | Courier picked up your order |
| IN_TRANSIT | Courier on the way to you |
| DELIVERED | Order delivered |
| COMPLETED | Order finalized |
| CANCELLED | Order cancelled |

---

## Error Responses

```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

### Validation Error
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "email": "Invalid email format",
    "phone": "Phone number is required"
  }
}
```

---

## Test Accounts

| Email/Phone | Role | Password |
|-------------|------|----------|
| john.doe@example.com | CONSUMER | password |
| +998901234567 | CONSUMER | OTP: 123456 (test) |
