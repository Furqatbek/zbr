# Admin Panel API Documentation

API endpoints for the **Admin Panel** frontend application.

**Target Users:** ADMIN, PLATFORM roles

**Base URL:** `http://localhost:8080/api/v1`

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [User Management](#2-user-management)
3. [Courier Management](#3-courier-management)
4. [Restaurant Management](#4-restaurant-management)
5. [Order Management](#5-order-management)
6. [Admin Dashboard](#6-admin-dashboard)
7. [Analytics](#7-analytics)
8. [Notifications (Admin)](#8-notifications-admin)
9. [System Configuration](#9-system-configuration)

---

## 1. Authentication

### Login
```
POST /auth/login
```

**Request:**
```json
{
  "emailOrPhone": "admin@fooddelivery.com",
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
    "expiresIn": 3600
  }
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

---

## 2. User Management

> **Required Role:** ADMIN, PLATFORM

### List All Users
```
GET /users
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (default: 0) |
| size | int | Page size (default: 20) |
| role | string | Filter by role |
| status | string | Filter by status (ACTIVE, INACTIVE, SUSPENDED) |

### Get User by ID
```
GET /users/{id}
Authorization: Bearer {token}
```

### Update User Status
```
PUT /users/{id}/status
Authorization: Bearer {token}
```

**Request:**
```json
{
  "status": "SUSPENDED",
  "reason": "Violation of terms"
}
```

### Lock User Account
```
POST /users/{id}/lock
Authorization: Bearer {token}
```

### Unlock User Account
```
POST /users/{id}/unlock
Authorization: Bearer {token}
```

### Delete User (ADMIN only)
```
DELETE /users/{id}
Authorization: Bearer {token}
```

### Assign Role (ADMIN only)
```
POST /users/{id}/roles
Authorization: Bearer {token}
```

**Request:**
```json
{
  "role": "PLATFORM"
}
```

### Remove Role (ADMIN only)
```
DELETE /users/{id}/roles/{role}
Authorization: Bearer {token}
```

---

## 3. Courier Management

> **Required Role:** ADMIN, PLATFORM

### List All Couriers
```
GET /couriers
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number |
| size | int | Page size |
| status | string | PENDING_APPROVAL, AVAILABLE, BUSY, OFFLINE |
| verified | boolean | Filter by verification status |

### Get Courier Details
```
GET /couriers/{id}
Authorization: Bearer {token}
```

### Verify Courier
```
POST /couriers/{id}/verify
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 5,
    "userName": "John Courier",
    "status": "OFFLINE",
    "verified": true,
    "verifiedAt": "2024-01-15T10:30:00Z"
  }
}
```

### Find Available Couriers (for manual assignment)
```
GET /couriers/nearby?lat={lat}&lng={lng}&radiusKm={radius}
Authorization: Bearer {token}
```

---

## 4. Restaurant Management

> **Required Role:** ADMIN, PLATFORM

### List All Restaurants
```
GET /restaurants
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number |
| size | int | Page size |
| status | string | PENDING, APPROVED, SUSPENDED |
| isOpen | boolean | Filter by open status |

### Get Restaurant Details
```
GET /restaurants/{id}
Authorization: Bearer {token}
```

### Update Restaurant Status
```
PUT /restaurants/{id}/status
Authorization: Bearer {token}
```

**Request:**
```json
{
  "status": "APPROVED"
}
```

**Status Values:** `PENDING`, `APPROVED`, `SUSPENDED`, `REJECTED`

### Get Restaurant Orders
```
GET /restaurants/{id}/orders
Authorization: Bearer {token}
```

---

## 5. Order Management

> **Required Role:** ADMIN, PLATFORM

### List All Orders
```
GET /orders
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number |
| size | int | Page size |
| status | string | Order status filter |
| restaurantId | long | Filter by restaurant |
| consumerId | long | Filter by consumer |
| courierId | long | Filter by courier |
| dateFrom | date | Start date |
| dateTo | date | End date |

### Get Order Details
```
GET /orders/{id}
Authorization: Bearer {token}
```

### Update Order Status (override)
```
PUT /orders/{id}/status
Authorization: Bearer {token}
```

**Request:**
```json
{
  "status": "CANCELLED",
  "reason": "Customer requested cancellation"
}
```

---

## 6. Admin Dashboard

> **Required Role:** ADMIN, OPERATIONS_MANAGER

### Get Dashboard Overview
```
GET /admin/dashboard/overview
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalOrders": 15420,
    "totalRevenue": 458750.00,
    "activeUsers": 3250,
    "activeRestaurants": 128,
    "activeCouriers": 45,
    "pendingApprovals": 12
  }
}
```

### Get Stuck Orders
```
GET /admin/dashboard/orders/stuck
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| stuckMinutes | int | Minutes threshold (default: 30) |

### Get Canceled Orders Analysis
```
GET /admin/dashboard/orders/canceled
Authorization: Bearer {token}
```

### Get Peak Hours Analysis
```
GET /admin/dashboard/peak-hours
Authorization: Bearer {token}
```

### Get Restaurant Performance
```
GET /admin/dashboard/restaurants/performance
Authorization: Bearer {token}
```

### Get Courier Performance
```
GET /admin/dashboard/couriers/performance
Authorization: Bearer {token}
```

### Get Revenue Summary
```
GET /admin/dashboard/revenue
Authorization: Bearer {token}
```

### Get Support Tickets Summary
```
GET /admin/dashboard/support/summary
Authorization: Bearer {token}
```

### Get System Health
```
GET /admin/dashboard/system/health
Authorization: Bearer {token}
```

---

## 7. Analytics

> **Required Role:** ADMIN, PLATFORM

### General Analytics

#### Revenue Analytics
```
GET /analytics/revenue
Authorization: Bearer {token}
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| startDate | date | Period start |
| endDate | date | Period end |
| groupBy | string | DAY, WEEK, MONTH |

#### Order Analytics
```
GET /analytics/orders
Authorization: Bearer {token}
```

#### User Growth Analytics
```
GET /analytics/users
Authorization: Bearer {token}
```

#### Restaurant Analytics
```
GET /analytics/restaurants
Authorization: Bearer {token}
```

### Operations Analytics

```
GET /analytics/operations/delivery-times
GET /analytics/operations/order-volume
GET /analytics/operations/courier-utilization
GET /analytics/operations/restaurant-preparation
Authorization: Bearer {token}
```

### Financial Analytics

```
GET /analytics/financial/revenue
GET /analytics/financial/payouts
GET /analytics/financial/commissions
GET /analytics/financial/refunds
Authorization: Bearer {token}
```

### Customer Experience Analytics

```
GET /analytics/cx/satisfaction
GET /analytics/cx/retention
GET /analytics/cx/complaints
Authorization: Bearer {token}
```

### Fraud Analytics (ADMIN only)

```
GET /analytics/fraud/suspicious-orders
GET /analytics/fraud/payment-anomalies
GET /analytics/fraud/account-anomalies
GET /analytics/fraud/promo-abuse
GET /analytics/fraud/velocity-checks
Authorization: Bearer {token}
```

### Technical Analytics

```
GET /analytics/technical/api-performance
GET /analytics/technical/error-rates
GET /analytics/technical/websocket-metrics
Authorization: Bearer {token}
```

---

## 8. Notifications (Admin)

> **Required Role:** ADMIN

### Send Broadcast Notification
```
POST /notifications/broadcast
Authorization: Bearer {token}
```

**Request:**
```json
{
  "title": "System Maintenance",
  "message": "Scheduled maintenance on Sunday 2AM-4AM",
  "targetRoles": ["CONSUMER", "RESTAURANT_OWNER"],
  "priority": "HIGH"
}
```

### Cleanup Expired Notifications
```
POST /notifications/admin/cleanup/expired
Authorization: Bearer {token}
```

### Cleanup Dismissed Notifications
```
POST /notifications/admin/cleanup/dismissed?daysOld=7
Authorization: Bearer {token}
```

### Cleanup Old Read Notifications
```
POST /notifications/admin/cleanup/read?daysOld=90
Authorization: Bearer {token}
```

---

## 9. System Configuration

> **Required Role:** ADMIN only

### Platform Settings
```
GET /admin/settings
PUT /admin/settings
Authorization: Bearer {token}
```

**Settings Include:**
- Platform commission rates
- Delivery fee ranges
- Order limits
- Operating regions
- Feature flags

### Export Data (ADMIN only)
```
POST /analytics/export
Authorization: Bearer {token}
```

**Request:**
```json
{
  "type": "ORDERS",
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "format": "CSV"
}
```

---

## Error Responses

All endpoints return standard error format:

```json
{
  "success": false,
  "message": "Error description",
  "data": {
    "field": "Validation error details"
  }
}
```

### HTTP Status Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden (insufficient role) |
| 404 | Not Found |
| 500 | Internal Server Error |

---

## WebSocket Endpoints (Real-time)

### Connection
```
ws://localhost:8080/ws
Authorization: Bearer {token} (in STOMP CONNECT header)
```

### Topics for Admin

| Topic | Description |
|-------|-------------|
| `/topic/orders/new` | New order notifications |
| `/topic/orders/stuck` | Stuck order alerts |
| `/topic/couriers/status` | Courier status changes |
| `/topic/system/alerts` | System alerts |

---

## Test Accounts

| Email | Role | Password |
|-------|------|----------|
| admin@fooddelivery.com | ADMIN | password |
| platform@fooddelivery.com | PLATFORM | password |
