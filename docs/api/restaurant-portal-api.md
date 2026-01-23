# Restaurant Portal API Documentation

API endpoints for the **Restaurant Portal** frontend application.

**Target Users:** RESTAURANT_OWNER, RESTAURANT_STAFF roles

**Base URL:** `http://localhost:8080/api/v1`

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Restaurant Management](#2-restaurant-management)
3. [Menu Management](#3-menu-management)
4. [Order Management](#4-order-management)
5. [Image Upload](#5-image-upload)
6. [Notifications](#6-notifications)
7. [WebSocket (Real-time)](#7-websocket-real-time)

---

## 1. Authentication

### Login
```
POST /auth/login
```

**Request:**
```json
{
  "emailOrPhone": "owner@pizzapalace.com",
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
      "id": 3,
      "email": "owner@pizzapalace.com",
      "fullName": "Pizza Palace Owner",
      "roles": ["RESTAURANT_OWNER"]
    }
  }
}
```

### Register Restaurant Owner
```
POST /auth/register
```

**Request:**
```json
{
  "email": "newowner@restaurant.com",
  "password": "SecurePass123!",
  "fullName": "New Restaurant Owner",
  "phone": "+998901234567",
  "role": "RESTAURANT_OWNER"
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

### Get Current User
```
GET /auth/me
Authorization: Bearer {token}
```

### Change Password
```
POST /auth/change-password
Authorization: Bearer {token}
```

---

## 2. Restaurant Management

### Create Restaurant

> **Required Role:** RESTAURANT_OWNER, PLATFORM, ADMIN

```
POST /restaurants
Authorization: Bearer {token}
```

**Request:**
```json
{
  "name": "Pizza Palace",
  "description": "Best pizza in town",
  "phone": "+998901234567",
  "email": "contact@pizzapalace.com",
  "addressLine1": "123 Main Street",
  "addressLine2": "Suite 100",
  "city": "Tashkent",
  "state": "Tashkent",
  "postalCode": "100000",
  "country": "Uzbekistan",
  "latitude": 41.2995,
  "longitude": 69.2401,
  "acceptsDelivery": true,
  "acceptsTakeaway": true,
  "acceptsDineIn": false,
  "deliveryFee": 15000,
  "minimumOrder": 30000,
  "deliveryRadiusKm": 10,
  "averagePrepTimeMinutes": 25,
  "opensAt": "09:00",
  "closesAt": "22:00"
}
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
    "description": "Best pizza in town",
    "phone": "+998901234567",
    "email": "contact@pizzapalace.com",
    "fullAddress": "123 Main Street, Tashkent, Tashkent 100000, Uzbekistan",
    "addressLine1": "123 Main Street",
    "city": "Tashkent",
    "state": "Tashkent",
    "postalCode": "100000",
    "country": "Uzbekistan",
    "latitude": 41.2995,
    "longitude": 69.2401,
    "status": "PENDING",
    "acceptsDelivery": true,
    "acceptsTakeaway": true,
    "acceptsDineIn": false,
    "minimumOrder": 30000,
    "deliveryFee": 15000,
    "deliveryRadiusKm": 10,
    "averagePrepTimeMinutes": 25,
    "opensAt": "09:00:00",
    "closesAt": "22:00:00",
    "isOpen": false,
    "isCurrentlyOpen": false,
    "averageRating": null,
    "totalRatings": 0,
    "totalOrders": 0,
    "createdAt": "2024-01-15T10:00:00Z"
  }
}
```

### Get My Restaurants

```
GET /restaurants/my
Authorization: Bearer {token}
```

### Get Restaurant by ID

```
GET /restaurants/{id}
Authorization: Bearer {token}
```

### Update Restaurant

> **Required Role:** RESTAURANT_OWNER, RESTAURANT_STAFF, PLATFORM, ADMIN

```
PUT /restaurants/{id}
Authorization: Bearer {token}
```

**Request:**
```json
{
  "name": "Pizza Palace Updated",
  "description": "Updated description",
  "deliveryFee": 12000,
  "minimumOrder": 25000,
  "averagePrepTimeMinutes": 30
}
```

### Toggle Open/Closed Status

> **Required Role:** RESTAURANT_OWNER, RESTAURANT_STAFF

```
PATCH /restaurants/{id}/toggle-open?isOpen={true|false}
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| isOpen | boolean | true to open, false to close |

**Response:**
```json
{
  "success": true,
  "message": "Restaurant is now open",
  "data": {
    "id": 1,
    "name": "Pizza Palace",
    "status": "ACTIVE",
    "isOpen": true,
    "isCurrentlyOpen": true
  }
}
```

---

## 3. Menu Management

### Categories

#### Get All Categories
```
GET /restaurants/{restaurantId}/menu/categories
```
*Public endpoint - no auth required*

#### Create Category

> **Required Role:** RESTAURANT_OWNER, RESTAURANT_STAFF

```
POST /restaurants/{restaurantId}/menu/categories
Authorization: Bearer {token}
```

**Request:**
```json
{
  "name": "Pizzas",
  "description": "Our delicious pizzas",
  "imageUrl": "/images/categories/pizzas.jpg",
  "sortOrder": 1
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "restaurantId": 1,
    "name": "Pizzas",
    "description": "Our delicious pizzas",
    "imageUrl": "/images/categories/pizzas.jpg",
    "sortOrder": 1,
    "active": true
  }
}
```

#### Update Category
```
PUT /restaurants/{restaurantId}/menu/categories/{categoryId}
Authorization: Bearer {token}
```

#### Delete Category
```
DELETE /restaurants/{restaurantId}/menu/categories/{categoryId}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "message": "Category deleted successfully",
  "data": null
}
```

### Menu Items

#### Get All Menu Items
```
GET /restaurants/{restaurantId}/menu/items
```
*Public endpoint - no auth required*

#### Get Menu Item by ID
```
GET /restaurants/{restaurantId}/menu/items/{itemId}
```

#### Create Menu Item

> **Required Role:** RESTAURANT_OWNER, RESTAURANT_STAFF

```
POST /restaurants/{restaurantId}/menu/items
Authorization: Bearer {token}
```

**Request:**
```json
{
  "categoryId": 1,
  "name": "Margherita Pizza",
  "description": "Classic pizza with tomato sauce, mozzarella, and basil",
  "price": 45000,
  "originalPrice": 50000,
  "imageUrl": "/images/margherita.jpg",
  "prepTimeMinutes": 15,
  "calories": 850,
  "vegetarian": true,
  "vegan": false,
  "glutenFree": false,
  "spicy": false,
  "allergens": "Contains dairy, gluten",
  "featured": true,
  "sortOrder": 1,
  "variants": [
    {"name": "Small", "priceDelta": -10000, "sortOrder": 1},
    {"name": "Medium", "priceDelta": 0, "sortOrder": 2},
    {"name": "Large", "priceDelta": 10000, "sortOrder": 3}
  ],
  "options": [
    {
      "groupName": "Extra Toppings",
      "name": "Extra Cheese",
      "priceDelta": 5000,
      "isDefault": false,
      "maxSelections": 1,
      "required": false,
      "sortOrder": 1
    },
    {
      "groupName": "Extra Toppings",
      "name": "Mushrooms",
      "priceDelta": 4000,
      "isDefault": false,
      "maxSelections": 1,
      "required": false,
      "sortOrder": 2
    }
  ]
}
```

**Notes:**
- `price`: Base price of the item (required)
- `originalPrice`: For showing discounts (optional)
- `variants.priceDelta`: Price difference from base price (e.g., -10000 for Small = 35000 total)
- `options.priceDelta`: Additional cost for this option

#### Update Menu Item
```
PUT /restaurants/{restaurantId}/menu/items/{itemId}
Authorization: Bearer {token}
```

#### Delete Menu Item
```
DELETE /restaurants/{restaurantId}/menu/items/{itemId}
Authorization: Bearer {token}
```

#### Update Item Stock Status
```
PATCH /restaurants/{restaurantId}/menu/items/{itemId}/stock?inStock={true|false}
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| inStock | boolean | true if in stock, false if out of stock |

**Response:**
```json
{
  "success": true,
  "message": "Item is now out of stock",
  "data": {
    "id": 15,
    "categoryId": 1,
    "categoryName": "Pizzas",
    "name": "Margherita Pizza",
    "description": "Classic pizza with tomato sauce, mozzarella, and basil",
    "price": 45000,
    "inStock": false,
    "active": true
  }
}
```

#### Upload Item Image
```
POST /restaurants/{restaurantId}/menu/items/{itemId}/image
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Form Data:**
| Field | Type | Description |
|-------|------|-------------|
| file | file | Image file (JPEG, PNG, WebP) |

**Response:**
```json
{
  "success": true,
  "message": "Image uploaded successfully",
  "data": {
    "id": 15,
    "name": "Margherita Pizza",
    "imageUrl": "/images/menu/margherita-abc123.jpg"
  }
}
```

#### Delete Item Image
```
DELETE /restaurants/{restaurantId}/menu/items/{itemId}/image
Authorization: Bearer {token}
```

---

## 4. Order Management

### Get Restaurant Orders

> **Required Role:** RESTAURANT_OWNER, RESTAURANT_STAFF

```
GET /orders/restaurant/{restaurantId}
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (default: 0) |
| size | int | Page size (default: 20) |
| status | string | Filter by status |
| dateFrom | date | Start date |
| dateTo | date | End date |

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "externalOrderNo": "ORD-2024-0001",
        "status": "CREATED",
        "orderType": "DELIVERY",
        "consumer": {
          "id": 4,
          "name": "John Doe",
          "phone": "+998901234567"
        },
        "items": [...],
        "subtotal": 95000,
        "deliveryFee": 15000,
        "total": 110000,
        "createdAt": "2024-01-15T12:30:00Z"
      }
    ],
    "totalElements": 150,
    "totalPages": 8
  }
}
```

### Get Order Details
```
GET /orders/{orderId}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "externalOrderNo": "ORD-2024-0001",
    "consumerId": 4,
    "consumerName": "John Doe",
    "restaurantId": 1,
    "restaurantName": "Pizza Palace",
    "orderType": "DELIVERY",
    "status": "CREATED",
    "paymentStatus": "PAID",
    "items": [
      {
        "id": 101,
        "menuItemId": 15,
        "menuItemName": "Margherita Pizza",
        "quantity": 2,
        "unitPrice": 45000,
        "totalPrice": 90000,
        "specialInstructions": "No onions"
      }
    ],
    "subtotal": 90000,
    "tax": 9000,
    "deliveryFee": 15000,
    "discount": 0,
    "tipAmount": 5000,
    "total": 119000,
    "deliveryAddress": "456 Elm Street, Apt 5A",
    "deliveryInstructions": "Ring doorbell twice",
    "customerName": "John Doe",
    "customerPhone": "+998907654321",
    "estimatedPrepTimeMinutes": 25,
    "createdAt": "2024-01-15T12:30:00Z",
    "acceptedAt": null,
    "readyAt": null
  }
}
```

### Update Order Status

> **Required Role:** RESTAURANT_OWNER, RESTAURANT_STAFF

Use this single endpoint to update order status (accept, prepare, ready, etc.)

```
PATCH /orders/{orderId}/status
Authorization: Bearer {token}
Content-Type: application/json
```

**Request:**
```json
{
  "status": "ACCEPTED",
  "estimatedPrepTimeMinutes": 20,
  "notes": "Order accepted"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Order status updated",
  "data": {
    "id": 1,
    "externalOrderNo": "ORD-2024-0001",
    "status": "ACCEPTED",
    "acceptedAt": "2024-01-15T12:32:00Z",
    "estimatedPrepTimeMinutes": 20
  }
}
```

**Status Transitions for Restaurant:**

| Action | Set Status To | Notes Field |
|--------|---------------|-------------|
| Accept order | `ACCEPTED` | Optional estimated prep time |
| Start preparing | `PREPARING` | Optional notes |
| Mark as ready | `READY` | Order ready for pickup |
| Reject order | `CANCELLED` | Must include reason in notes |

**Example - Accept Order:**
```json
{
  "status": "ACCEPTED",
  "estimatedPrepTimeMinutes": 25
}
```

**Example - Start Preparing:**
```json
{
  "status": "PREPARING",
  "notes": "Cooking started"
}
```

**Example - Mark Ready:**
```json
{
  "status": "READY"
}
```

**Example - Reject Order:**
```json
{
  "status": "CANCELLED",
  "notes": "Item out of stock"
}
```

### Cancel Order

```
POST /orders/{orderId}/cancel
Authorization: Bearer {token}
```

**Request:**
```json
{
  "reason": "Customer requested cancellation"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Order cancelled",
  "data": {
    "id": 1,
    "status": "CANCELLED",
    "cancellationReason": "Customer requested cancellation"
  }
}
```

---

## 5. Image Upload

> **Required Role:** RESTAURANT_OWNER, RESTAURANT_STAFF

### Upload Image
```
POST /images/upload
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Form Data:**
| Field | Type | Description |
|-------|------|-------------|
| file | file | Image file (JPEG, PNG, WebP) |
| type | string | MENU_ITEM, RESTAURANT, CATEGORY |

**Response:**
```json
{
  "success": true,
  "data": {
    "url": "/images/menu/abc123.jpg",
    "thumbnailUrl": "/images/menu/abc123_thumb.jpg"
  }
}
```

### Delete Image
```
DELETE /images/{imageId}
Authorization: Bearer {token}
```

---

## 6. Notifications

### Get My Notifications
```
GET /notifications
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number |
| size | int | Page size |
| unreadOnly | boolean | Filter unread only |

### Get Unread Count
```
GET /notifications/unread/count
Authorization: Bearer {token}
```

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
  "userId": 3,
  "role": "RESTAURANT_OWNER",
  "title": "New Order Received",
  "message": "Order #ORD-2024-0001 has been placed",
  "category": "ORDER",
  "read": true,
  "readAt": "2024-01-15T12:35:00Z",
  "createdAt": "2024-01-15T12:30:00Z"
}
```

### Mark All as Read
```
PUT /notifications/read-all
Authorization: Bearer {token}
```

### Dismiss Notification
```
DELETE /notifications/{id}
Authorization: Bearer {token}
```

---

## 7. WebSocket (Real-time)

### Connection
```
ws://localhost:8080/ws
```

**STOMP CONNECT Headers:**
```
Authorization: Bearer {accessToken}
```

### Subscribe to Kitchen Orders

```javascript
// Subscribe to new orders for your restaurant
stompClient.subscribe('/topic/kitchen/{restaurantId}', (message) => {
  const order = JSON.parse(message.body);
  console.log('New order:', order);
});
```

### Topics for Restaurant

| Topic | Description |
|-------|-------------|
| `/topic/kitchen/{restaurantId}` | New incoming orders |
| `/topic/orders/{orderId}/status` | Order status updates |
| `/user/queue/notifications` | Personal notifications |

### Kitchen Display Message Format

```json
{
  "orderId": 123,
  "orderNumber": "ORD-2024-0001",
  "orderType": "DELIVERY",
  "items": [
    {
      "name": "Margherita Pizza",
      "quantity": 2,
      "variant": "Large",
      "options": ["Extra Cheese"],
      "notes": "No onions please"
    }
  ],
  "customerName": "John D.",
  "createdAt": "2024-01-15T12:30:00Z",
  "priority": "NORMAL"
}
```

---

## Order Status Flow (Restaurant Perspective)

```
CREATED ──► ACCEPTED ──► PREPARING ──► READY ──► COURIER_ASSIGNED ──► PICKED_UP ──► DELIVERED
    │                                    │              │
    └──► CANCELLED (with reason) ◄───────┴──────────────┘
```

**API Calls by Action:**

| Action | Endpoint | Request Body |
|--------|----------|--------------|
| Accept Order | `PATCH /orders/{id}/status` | `{"status": "ACCEPTED", "estimatedPrepTimeMinutes": 20}` |
| Start Preparing | `PATCH /orders/{id}/status` | `{"status": "PREPARING"}` |
| Mark Ready | `PATCH /orders/{id}/status` | `{"status": "READY"}` |
| Reject/Cancel | `POST /orders/{id}/cancel` | `{"reason": "Item out of stock"}` |

**Valid Status Transitions:**

| Current Status | Can Change To |
|----------------|---------------|
| CREATED | ACCEPTED, CANCELLED |
| ACCEPTED | PREPARING, CANCELLED |
| PREPARING | READY, CANCELLED |
| READY | COURIER_ASSIGNED, CANCELLED |
| COURIER_ASSIGNED | PICKED_UP, CANCELLED |

**Status Descriptions:**

| Status | Description |
|--------|-------------|
| CREATED | Order placed, waiting for restaurant |
| ACCEPTED | Restaurant accepted the order |
| PREPARING | Kitchen is preparing the food |
| READY | Food ready, waiting for courier |
| COURIER_ASSIGNED | Courier accepted, on the way to pickup |
| PICKED_UP | Courier picked up from restaurant |
| IN_TRANSIT | Courier driving to customer |
| DELIVERED | Order delivered to customer |
| COMPLETED | Order finalized |
| CANCELLED | Order cancelled |
| REFUNDED | Payment refunded |

---

## Error Responses

```json
{
  "success": false,
  "message": "Error description",
  "data": {
    "field": "Validation error"
  }
}
```

### Common Error Codes

| Code | Description |
|------|-------------|
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Invalid/expired token |
| 403 | Forbidden - Not your restaurant |
| 404 | Not Found - Resource doesn't exist |
| 409 | Conflict - Invalid status transition |

---

## Test Accounts

| Email | Role | Password |
|-------|------|----------|
| owner@pizzapalace.com | RESTAURANT_OWNER | password |

---

## Integration Tips

### Kitchen Display System (KDS)

1. Connect to WebSocket on page load
2. Subscribe to `/topic/kitchen/{restaurantId}`
3. Display incoming orders in queue
4. Use REST API to update order status
5. Auto-print tickets on new order (optional)

### Recommended Polling Intervals

| Data | Method | Interval |
|------|--------|----------|
| Active Orders | WebSocket | Real-time |
| Order List | REST | On-demand |
| Notifications | WebSocket | Real-time |
