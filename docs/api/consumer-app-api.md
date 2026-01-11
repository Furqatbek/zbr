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

### Logout
```
POST /auth/logout
Authorization: Bearer {token}
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
        "slug": "pizza-palace",
        "name": "Pizza Palace",
        "imageUrl": "/images/restaurants/pizza-palace.jpg",
        "cuisineTypes": ["ITALIAN", "FAST_FOOD"],
        "rating": 4.5,
        "reviewCount": 234,
        "deliveryFee": 15000,
        "minOrderAmount": 30000,
        "avgDeliveryTime": 30,
        "distance": 2.5,
        "isOpen": true,
        "isFavorite": false
      }
    ],
    "totalElements": 45,
    "totalPages": 3
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
    "slug": "pizza-palace",
    "name": "Pizza Palace",
    "description": "Best pizza in town since 1995",
    "imageUrl": "/images/restaurants/pizza-palace.jpg",
    "coverImageUrl": "/images/restaurants/pizza-palace-cover.jpg",
    "phone": "+998901234567",
    "fullAddress": "123 Main Street, Tashkent",
    "latitude": 41.2995,
    "longitude": 69.2401,
    "cuisineTypes": ["ITALIAN", "FAST_FOOD"],
    "rating": 4.5,
    "reviewCount": 234,
    "deliveryFee": 15000,
    "minOrderAmount": 30000,
    "avgPrepTime": 25,
    "isOpen": true,
    "operatingHours": {...}
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
  "deliveryAddress": {
    "fullAddress": "123 Main St, Apt 4B",
    "latitude": 41.2995,
    "longitude": 69.2401,
    "apartmentNumber": "4B",
    "instructions": "Ring doorbell twice"
  },
  "items": [
    {
      "menuItemId": 101,
      "quantity": 2,
      "variantId": 3,
      "selectedOptions": [
        {"optionId": 1, "choiceIds": [1, 2]}
      ],
      "specialInstructions": "No onions please"
    }
  ],
  "paymentMethod": "CARD",
  "promoCode": "FIRST20"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 456,
    "externalOrderNo": "ORD-2024-0456",
    "status": "CREATED",
    "restaurant": {
      "id": 1,
      "name": "Pizza Palace"
    },
    "items": [...],
    "subtotal": 110000,
    "deliveryFee": 15000,
    "discount": 22000,
    "total": 103000,
    "payment": {
      "status": "PENDING",
      "clientSecret": "pi_xxx_secret_xxx"
    },
    "estimatedDeliveryTime": "2024-01-15T13:30:00Z"
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
    "orderId": 456,
    "status": "IN_TRANSIT",
    "statusHistory": [
      {"status": "CREATED", "timestamp": "2024-01-15T12:30:00Z"},
      {"status": "ACCEPTED", "timestamp": "2024-01-15T12:32:00Z"},
      {"status": "PREPARING", "timestamp": "2024-01-15T12:35:00Z"},
      {"status": "READY", "timestamp": "2024-01-15T12:50:00Z"},
      {"status": "PICKED_UP", "timestamp": "2024-01-15T12:55:00Z"},
      {"status": "IN_TRANSIT", "timestamp": "2024-01-15T12:56:00Z"}
    ],
    "courier": {
      "id": 5,
      "name": "Alex Courier",
      "phone": "+998907654321",
      "rating": 4.8,
      "vehicleType": "MOTORCYCLE",
      "currentLocation": {
        "latitude": 41.3001,
        "longitude": 69.2450
      }
    },
    "estimatedDeliveryTime": "2024-01-15T13:10:00Z",
    "restaurant": {
      "latitude": 41.2995,
      "longitude": 69.2401
    },
    "deliveryAddress": {
      "latitude": 41.3112,
      "longitude": 69.2797
    }
  }
}
```

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

### Get Notifications
```
GET /notifications
Authorization: Bearer {token}
```

### Get Unread Count
```
GET /notifications/unread/count
Authorization: Bearer {token}
```

### Mark as Read
```
PUT /notifications/{id}/read
Authorization: Bearer {token}
```

### Mark All as Read
```
PUT /notifications/read-all
Authorization: Bearer {token}
```

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
CREATED ──► ACCEPTED ──► PREPARING ──► READY ──► PICKED_UP ──► IN_TRANSIT ──► DELIVERED ──► COMPLETED
    │                                                                              │
    │                                                                              └──► Rate & Review
    │
    └──► CANCELLED (by consumer or restaurant)
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
