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
  "fullAddress": "123 Main Street, Tashkent",
  "latitude": 41.2995,
  "longitude": 69.2401,
  "cuisineTypes": ["ITALIAN", "FAST_FOOD"],
  "deliveryFee": 15000,
  "minOrderAmount": 30000,
  "avgPrepTime": 25,
  "operatingHours": {
    "MONDAY": {"open": "09:00", "close": "22:00"},
    "TUESDAY": {"open": "09:00", "close": "22:00"},
    "WEDNESDAY": {"open": "09:00", "close": "22:00"},
    "THURSDAY": {"open": "09:00", "close": "22:00"},
    "FRIDAY": {"open": "09:00", "close": "23:00"},
    "SATURDAY": {"open": "10:00", "close": "23:00"},
    "SUNDAY": {"open": "10:00", "close": "21:00"}
  }
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "slug": "pizza-palace",
    "name": "Pizza Palace",
    "status": "PENDING",
    "isOpen": false,
    ...
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
  "minOrderAmount": 25000
}
```

### Toggle Open/Closed Status

> **Required Role:** RESTAURANT_OWNER, RESTAURANT_STAFF

```
PUT /restaurants/{id}/toggle-open
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "isOpen": true,
    "message": "Restaurant is now open"
  }
}
```

### Update Operating Hours

```
PUT /restaurants/{id}/hours
Authorization: Bearer {token}
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
  "sortOrder": 1
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

#### Reorder Categories
```
PUT /restaurants/{restaurantId}/menu/categories/reorder
Authorization: Bearer {token}
```

**Request:**
```json
{
  "categoryOrders": [
    {"categoryId": 1, "sortOrder": 1},
    {"categoryId": 2, "sortOrder": 2},
    {"categoryId": 3, "sortOrder": 3}
  ]
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
  "imageUrl": "/images/margherita.jpg",
  "inStock": true,
  "vegetarian": true,
  "vegan": false,
  "glutenFree": false,
  "spicyLevel": 0,
  "preparationTime": 15,
  "variants": [
    {"name": "Small", "price": 35000},
    {"name": "Medium", "price": 45000},
    {"name": "Large", "price": 55000}
  ],
  "options": [
    {
      "name": "Extra Toppings",
      "required": false,
      "multiSelect": true,
      "choices": [
        {"name": "Extra Cheese", "price": 5000},
        {"name": "Mushrooms", "price": 4000},
        {"name": "Olives", "price": 3000}
      ]
    }
  ]
}
```

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

#### Toggle Item Availability
```
PUT /restaurants/{restaurantId}/menu/items/{itemId}/toggle-stock
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 15,
    "name": "Margherita Pizza",
    "inStock": false
  }
}
```

#### Bulk Update Stock Status
```
PUT /restaurants/{restaurantId}/menu/items/bulk-stock
Authorization: Bearer {token}
```

**Request:**
```json
{
  "items": [
    {"itemId": 1, "inStock": true},
    {"itemId": 2, "inStock": false},
    {"itemId": 3, "inStock": true}
  ]
}
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

### Accept Order

> **Required Role:** RESTAURANT_OWNER, RESTAURANT_STAFF

```
PUT /orders/{orderId}/accept
Authorization: Bearer {token}
```

**Request (optional estimated time):**
```json
{
  "estimatedPrepTime": 20
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "ACCEPTED",
    "estimatedReadyTime": "2024-01-15T12:50:00Z"
  }
}
```

### Start Preparing
```
PUT /orders/{orderId}/preparing
Authorization: Bearer {token}
```

### Mark as Ready
```
PUT /orders/{orderId}/ready
Authorization: Bearer {token}
```

### Reject Order
```
PUT /orders/{orderId}/reject
Authorization: Bearer {token}
```

**Request:**
```json
{
  "reason": "Item out of stock"
}
```

### Update Order Status
```
PUT /orders/{orderId}/status
Authorization: Bearer {token}
```

**Request:**
```json
{
  "status": "PREPARING",
  "note": "Started cooking"
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

### Dismiss Notification
```
PUT /notifications/{id}/dismiss
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
CREATED ──► ACCEPTED ──► PREPARING ──► READY ──► [Courier picks up]
    │
    └──► REJECTED (with reason)
```

**Actions by Status:**

| Current Status | Available Actions |
|----------------|-------------------|
| CREATED | Accept, Reject |
| ACCEPTED | Start Preparing |
| PREPARING | Mark Ready |
| READY | Wait for courier pickup |

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
