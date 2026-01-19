# Notification API Documentation

Notification management for the Food Delivery Platform. Provides endpoints for creating, retrieving, updating, and managing notifications across all user roles.

## Base URL

```
/api/v1/notifications
```

---

## Table of Contents

1. [Create Operations](#1-create-operations)
2. [Read Operations](#2-read-operations)
3. [Update Operations](#3-update-operations)
4. [Delete Operations](#4-delete-operations)
5. [Admin Operations](#5-admin-operations)
6. [Reference Data Endpoints](#6-reference-data-endpoints)
7. [Template Management](#7-template-management)
8. [Enumerations](#enumerations)
9. [Caching & Performance](#caching--performance)

---

## 1. Create Operations

### Create Notification

**POST** `/`

Create a new notification for a user or role. Admin/System only.

**Request Body:**
```json
{
  "userId": 100,
  "role": "CUSTOMER",
  "category": "ORDER",
  "title": "Order Confirmed",
  "message": "Your order #ORD-12345 has been confirmed",
  "icon": "shopping-bag",
  "actionUrl": "/orders/12345",
  "relatedEntityType": "ORDER",
  "relatedEntityId": 12345,
  "orderId": 12345,
  "priority": "NORMAL",
  "expiresAt": "2024-02-15T10:30:00"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| userId | Long | No* | Target user ID |
| role | NotificationRole | No* | Target role for broadcast |
| category | NotificationCategory | Yes | Notification category |
| title | string | Yes | Notification title |
| message | string | Yes | Notification body |
| icon | string | No | Icon identifier |
| actionUrl | string | No | URL for action button |
| relatedEntityType | string | No | Related entity type (ORDER, RESTAURANT, etc.) |
| relatedEntityId | Long | No | Related entity ID |
| orderId | Long | No | Associated order ID |
| priority | string | No | Priority level (LOW, NORMAL, HIGH, URGENT) |
| expiresAt | datetime | No | Expiration timestamp |

*Either `userId` or `role` must be provided.

**Response (201):**
```json
{
  "id": 1,
  "userId": 100,
  "role": "CUSTOMER",
  "category": "ORDER",
  "title": "Order Confirmed",
  "message": "Your order #ORD-12345 has been confirmed",
  "icon": "shopping-bag",
  "actionUrl": "/orders/12345",
  "isRead": false,
  "isDismissed": false,
  "readAt": null,
  "createdAt": "2024-01-15T10:30:00",
  "expiresAt": "2024-02-15T10:30:00"
}
```

**Roles:** ADMIN, SYSTEM

---

## 2. Read Operations

### Get Notification by ID

**GET** `/{id}`

Retrieve a specific notification by its ID.

**Response (200):**
```json
{
  "id": 1,
  "userId": 100,
  "role": "CUSTOMER",
  "category": "ORDER",
  "title": "Order Confirmed",
  "message": "Your order #ORD-12345 has been confirmed",
  "icon": "shopping-bag",
  "actionUrl": "/orders/12345",
  "isRead": true,
  "isDismissed": false,
  "readAt": "2024-01-15T10:35:00",
  "createdAt": "2024-01-15T10:30:00"
}
```

---

### Get Notifications (with filters)

**GET** `/`

Get notifications with optional filters and pagination.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| userId | Long | - | Filter by user ID |
| role | NotificationRole | - | Filter by role |
| isRead | Boolean | - | Filter by read status |
| category | NotificationCategory | - | Filter by category |
| orderId | Long | - | Filter by order ID |
| createdFrom | datetime | - | Start date filter |
| createdTo | datetime | - | End date filter |
| searchTerm | string | - | Search in title/message |
| includeDismissed | Boolean | false | Include dismissed notifications |
| page | int | 0 | Page number (0-indexed) |
| pageSize | int | 20 | Page size (max 100) |
| sortBy | string | createdAt | Sort field |
| sortDirection | string | DESC | Sort direction (ASC/DESC) |

**Response (200):**
```json
{
  "content": [
    {
      "id": 1,
      "userId": 100,
      "category": "ORDER",
      "title": "Order Confirmed",
      "message": "Your order has been confirmed",
      "isRead": false,
      "createdAt": "2024-01-15T10:30:00"
    }
  ],
  "page": 0,
  "pageSize": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

---

### Search Notifications (POST)

**POST** `/search`

Search notifications with advanced filter in request body.

**Request Body:**
```json
{
  "userId": 100,
  "role": "CUSTOMER",
  "isRead": false,
  "category": "ORDER",
  "createdFrom": "2024-01-01T00:00:00",
  "createdTo": "2024-01-31T23:59:59",
  "searchTerm": "confirmed",
  "includeDismissed": false,
  "page": 0,
  "pageSize": 20,
  "sortBy": "createdAt",
  "sortDirection": "DESC"
}
```

---

### Get My Notifications

**GET** `/me`

Get notifications for the currently authenticated user.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| role | NotificationRole | - | Filter by role |
| isRead | Boolean | - | Filter by read status |
| category | NotificationCategory | - | Filter by category |
| page | int | 0 | Page number |
| pageSize | int | 20 | Page size |

**Roles:** Authenticated users

---

### Get Unread Notifications

**GET** `/user/{userId}/unread`

Get all unread notifications for a specific user.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| userId | Long | User ID |

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| role | NotificationRole | - | Filter by role |
| page | int | 0 | Page number |
| pageSize | int | 20 | Page size |

---

### Get Notification Counts

**GET** `/user/{userId}/counts`

Get notification counts (total, unread, by category) for a user.

**Response (200):**
```json
{
  "total": 150,
  "unread": 12,
  "byCategory": {
    "ORDER": 5,
    "FINANCE": 3,
    "PROMOTION": 2,
    "SYSTEM": 2
  }
}
```

---

### Get Unread Count

**GET** `/user/{userId}/unread-count`

Get only the count of unread notifications.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| role | NotificationRole | Optional role filter |

**Response (200):**
```json
{
  "unreadCount": 12
}
```

---

## 3. Update Operations

### Mark as Read

**PATCH** `/{id}/read`

Mark a specific notification as read.

**Response (200):**
```json
{
  "id": 1,
  "isRead": true,
  "readAt": "2024-01-15T10:35:00",
  ...
}
```

---

### Mark All as Read

**PATCH** `/read-all`

Mark all notifications as read for a user.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| userId | Long | Yes | User ID |
| role | NotificationRole | No | Optional role filter |

**Response (200):**
```json
{
  "status": "success",
  "markedCount": 15
}
```

---

### Mark Batch as Read

**PATCH** `/read-batch`

Mark multiple notifications as read by IDs.

**Request Body:**
```json
[1, 2, 3, 4, 5]
```

**Response (200):**
```json
{
  "status": "success",
  "markedCount": 5
}
```

---

### Dismiss Notification

**PATCH** `/{id}/dismiss`

Dismiss (soft delete) a notification. Dismissed notifications are hidden from default views.

**Response (200):**
```json
{
  "id": 1,
  "isDismissed": true,
  ...
}
```

---

### Bulk Action

**POST** `/bulk-action`

Perform bulk actions on multiple notifications.

**Request Body:**
```json
{
  "notificationIds": [1, 2, 3, 4, 5],
  "action": "MARK_READ"
}
```

| Action | Description |
|--------|-------------|
| `MARK_READ` | Mark all as read |
| `DISMISS` | Dismiss all |
| `DELETE` | Delete all |

**Response (200):**
```json
{
  "status": "success",
  "affectedCount": 5
}
```

---

## 4. Delete Operations

### Delete Notification

**DELETE** `/{id}`

Permanently delete a notification.

**Response:** 204 No Content

---

### Delete All for User

**DELETE** `/user/{userId}`

Delete all notifications for a specific user.

**Response (200):**
```json
{
  "status": "success",
  "deletedCount": 45
}
```

**Roles:** ADMIN or owner of notifications

---

## 5. Admin Operations

### Cleanup Expired Notifications

**POST** `/admin/cleanup/expired`

Remove all expired notifications from the system.

**Response (200):**
```json
{
  "status": "success",
  "deletedCount": 120
}
```

**Roles:** ADMIN

---

### Cleanup Dismissed Notifications

**POST** `/admin/cleanup/dismissed`

Remove old dismissed notifications.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| daysOld | int | 7 | Delete dismissed older than N days |

**Response (200):**
```json
{
  "status": "success",
  "deletedCount": 85
}
```

**Roles:** ADMIN

---

### Cleanup Old Read Notifications

**POST** `/admin/cleanup/read`

Remove old read notifications.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| daysOld | int | 90 | Delete read older than N days |

**Response (200):**
```json
{
  "status": "success",
  "deletedCount": 1500
}
```

**Roles:** ADMIN

---

## 6. Reference Data Endpoints

### Get Notification Types

**GET** `/types`

Get all available notification types with their display names and default settings.

**Response (200):**
```json
[
  {
    "value": "ORDER_CREATED",
    "displayName": "Order Created",
    "defaultCategory": "ORDER",
    "defaultPriority": "NORMAL"
  },
  {
    "value": "PAYMENT_FAILED",
    "displayName": "Payment Failed",
    "defaultCategory": "FINANCE",
    "defaultPriority": "URGENT"
  }
]
```

---

### Get Notification Roles

**GET** `/roles`

Get all available notification roles with their display names.

**Response (200):**
```json
[
  {
    "value": "CUSTOMER",
    "displayName": "Customer"
  },
  {
    "value": "COURIER",
    "displayName": "Courier"
  },
  {
    "value": "RESTAURANT",
    "displayName": "Restaurant"
  }
]
```

---

### Get Notification Categories

**GET** `/categories`

Get all available notification categories with their display names and codes.

**Response (200):**
```json
[
  {
    "value": "ORDER",
    "displayName": "Order Updates",
    "code": "order"
  },
  {
    "value": "FINANCE",
    "displayName": "Financial",
    "code": "finance"
  }
]
```

---

### Get Notification Priorities

**GET** `/priorities`

Get all available notification priorities with their display names and weights.

**Response (200):**
```json
[
  {
    "value": "LOW",
    "displayName": "Low",
    "weight": 1,
    "requiresImmediateDisplay": false
  },
  {
    "value": "URGENT",
    "displayName": "Urgent",
    "weight": 4,
    "requiresImmediateDisplay": true
  }
]
```

---

### Get All Reference Data

**GET** `/reference-data`

Get all notification reference data (types, roles, categories, priorities) in a single call.

**Response (200):**
```json
{
  "types": [...],
  "roles": [...],
  "categories": [...],
  "priorities": [...]
}
```

---

## 7. Template Management

### Get All Templates

**GET** `/templates`

Get all notification templates.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| activeOnly | Boolean | false | Return only active templates |

**Response (200):**
```json
[
  {
    "id": 1,
    "notificationType": "ORDER_CREATED",
    "role": "CUSTOMER",
    "titleTemplate": "Order #{orderId} Created",
    "messageTemplate": "Your order has been placed successfully. We'll notify you when the restaurant confirms it.",
    "icon": "shopping-bag",
    "actionUrlTemplate": "/orders/{orderId}",
    "active": true,
    "defaultTtlHours": 720,
    "createdAt": "2024-01-15T10:00:00",
    "updatedAt": null
  }
]
```

---

### Get Template by ID

**GET** `/templates/{id}`

Get a specific notification template by its ID.

**Response (200):**
```json
{
  "id": 1,
  "notificationType": "ORDER_CREATED",
  "role": "CUSTOMER",
  "titleTemplate": "Order #{orderId} Created",
  "messageTemplate": "Your order has been placed successfully.",
  "icon": "shopping-bag",
  "actionUrlTemplate": "/orders/{orderId}",
  "active": true,
  "defaultTtlHours": 720
}
```

---

### Get Templates by Type

**GET** `/templates/by-type/{type}`

Get all templates for a specific notification type.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| type | NotificationType | The notification type |

---

### Get Templates by Role

**GET** `/templates/by-role/{role}`

Get all templates for a specific role.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| role | NotificationRole | The target role |

---

### Get Template by Type and Role

**GET** `/templates/by-type-and-role`

Get a specific template for a notification type and role combination.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| type | NotificationType | Yes | The notification type |
| role | NotificationRole | Yes | The target role |

---

### Create Template

**POST** `/templates`

Create a new notification template.

**Request Body:**
```json
{
  "notificationType": "ORDER_DELIVERED",
  "role": "CUSTOMER",
  "titleTemplate": "Order #{orderId} Delivered!",
  "messageTemplate": "Your order has been delivered. Enjoy your meal! Rate your experience.",
  "icon": "check-circle",
  "actionUrlTemplate": "/orders/{orderId}/rate",
  "active": true,
  "defaultTtlHours": 168
}
```

**Response (201):** Created template

**Response (409):** Template for type/role combination already exists

**Roles:** ADMIN

---

### Update Template

**PUT** `/templates/{id}`

Update an existing notification template.

**Request Body:**
```json
{
  "titleTemplate": "Order #{orderId} Delivered Successfully!",
  "messageTemplate": "Your order has arrived. We hope you enjoy it!",
  "icon": "check-circle",
  "actionUrlTemplate": "/orders/{orderId}/rate",
  "active": true,
  "defaultTtlHours": 168
}
```

**Response (200):** Updated template

**Roles:** ADMIN

---

### Delete Template

**DELETE** `/templates/{id}`

Delete a notification template.

**Response:** 204 No Content

**Roles:** ADMIN

---

### Toggle Template Active Status

**PATCH** `/templates/{id}/toggle-active`

Toggle the active status of a notification template.

**Response (200):**
```json
{
  "id": 1,
  "active": false,
  ...
}
```

**Roles:** ADMIN

---

## Enumerations

### NotificationCategory

| Value | Display Name | Description |
|-------|--------------|-------------|
| `ORDER` | Order Updates | Order status updates, assignments |
| `FINANCE` | Financial | Payments, payouts, refunds |
| `SUPPORT` | Customer Support | Tickets, responses |
| `SYSTEM` | System | Maintenance, announcements |
| `PROMOTION` | Promotions | Offers, discounts |
| `ACCOUNT` | Account | Profile, security changes |
| `DELIVERY` | Delivery | Courier-specific notifications |
| `RESTAURANT_OPS` | Restaurant Operations | Restaurant operational alerts |
| `ALERT` | Alerts | Urgent/critical notifications |

### NotificationRole

| Value | Display Name | Description |
|-------|--------------|-------------|
| `CUSTOMER` | Customer | Order updates, promotions |
| `COURIER` | Courier | Delivery assignments |
| `RESTAURANT` | Restaurant | Order notifications |
| `ADMIN` | Admin | System and operational alerts |
| `SUPPORT` | Support Agent | Support ticket updates |
| `FINANCE` | Finance | Payment/payout notifications |
| `OPERATIONS` | Operations Manager | Operational alerts |
| `ALL` | All Users | System-wide broadcasts |

### Related Entity Types

| Type | Description |
|------|-------------|
| `ORDER` | Order entity |
| `RESTAURANT` | Restaurant entity |
| `COURIER` | Courier entity |
| `CUSTOMER` | Customer entity |
| `SUPPORT_TICKET` | Support ticket entity |
| `PAYMENT` | Payment entity |
| `PAYOUT` | Payout entity |

---

## Caching & Performance

### Cache Keys & TTL

| Cache Type | TTL | Description |
|------------|-----|-------------|
| Unread Count | 60s | Per-user unread count |
| Notification List | 30s | Paginated results |
| Notification Detail | 300s | Individual notification |

### Default Settings

| Setting | Value |
|---------|-------|
| Default page size | 20 |
| Max page size | 100 |
| Default TTL | 30 days (720 hours) |

### Cleanup Schedule

| Cleanup Type | Default Threshold |
|--------------|-------------------|
| Dismissed | 7 days |
| Read | 90 days |
| Unread | 180 days |

---

## Icons Reference

| Icon | Usage |
|------|-------|
| `shopping-bag` | Order notifications |
| `truck` | Delivery notifications |
| `credit-card` | Payment notifications |
| `alert-triangle` | Alert notifications |
| `check-circle` | Success notifications |
| `x-circle` | Error notifications |
| `info` | Informational notifications |
| `headphones` | Support notifications |
| `tag` | Promotional notifications |
| `user` | Account notifications |
| `settings` | System notifications |

---

## Action URL Templates

| Template | Usage |
|----------|-------|
| `/orders/{orderId}` | Order detail |
| `/tracking/{orderId}` | Delivery tracking |
| `/support/tickets/{ticketId}` | Support ticket |
| `/payments/{paymentId}` | Payment detail |
| `/restaurant/orders` | Restaurant orders |
| `/courier/deliveries` | Courier deliveries |

---

## Error Responses

### 404 Notification Not Found

```json
{
  "success": false,
  "message": "Notification not found"
}
```

### 400 Invalid Request

```json
{
  "success": false,
  "message": "Either userId or role must be provided"
}
```

### 403 Forbidden

```json
{
  "success": false,
  "message": "Not authorized to access this notification"
}
```
