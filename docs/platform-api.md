# Platform API Documentation

Platform administration and referral program management for the Food Delivery Platform.

## Base URLs

```
/api/v1/referrals   - Referral program (user-facing)
/api/v1/admin       - Platform administration
```

---

## Table of Contents

1. [Referral Program](#1-referral-program)
2. [Admin Analytics](#2-admin-analytics)
3. [Admin Referral Management](#3-admin-referral-management)
4. [Enumerations](#enumerations)
5. [Configuration](#configuration)

---

## 1. Referral Program

User-facing endpoints for managing referral codes and tracking rewards.

### Generate Referral Code

**POST** `/api/v1/referrals/generate`

Generate a new referral code for the authenticated user.

**Response (201):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "REF-ABC123",
    "referrerId": 100,
    "referrerName": "John Doe",
    "referredId": null,
    "referredName": null,
    "status": "PENDING",
    "rewardAmount": 10.00,
    "currency": "USD",
    "referrerRewarded": false,
    "referredRewarded": false,
    "expiresAt": "2024-02-15T10:30:00",
    "completedAt": null,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

**Roles:** Any authenticated user

---

### Get My Referrals

**GET** `/api/v1/referrals/my`

Get all referrals created by the authenticated user.

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
    "content": [
      {
        "id": 1,
        "code": "REF-ABC123",
        "referrerId": 100,
        "referrerName": "John Doe",
        "referredId": 150,
        "referredName": "Jane Smith",
        "status": "COMPLETED",
        "rewardAmount": 10.00,
        "currency": "USD",
        "referrerRewarded": true,
        "referredRewarded": true,
        "completedAt": "2024-01-20T14:30:00",
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
  }
}
```

**Roles:** Any authenticated user

---

### Get Referral Statistics

**GET** `/api/v1/referrals/my/stats`

Get referral statistics for the authenticated user.

**Response (200):**
```json
{
  "success": true,
  "data": {
    "userId": 100,
    "completedReferrals": 5,
    "totalRewards": 50.00,
    "rewardPerReferral": 10.00
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| userId | Long | User ID |
| completedReferrals | long | Number of successfully completed referrals |
| totalRewards | BigDecimal | Total reward amount earned |
| rewardPerReferral | BigDecimal | Standard reward per successful referral |

**Roles:** Any authenticated user

---

### Get Referral by Code

**GET** `/api/v1/referrals/{code}`

Get referral details by referral code.

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| code | string | Unique referral code |

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "REF-ABC123",
    "referrerId": 100,
    "referrerName": "John Doe",
    "referredId": null,
    "referredName": null,
    "status": "PENDING",
    "rewardAmount": 10.00,
    "currency": "USD",
    "referrerRewarded": false,
    "referredRewarded": false,
    "expiresAt": "2024-02-15T10:30:00",
    "completedAt": null,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

**Roles:** Any authenticated user

---

## 2. Admin Analytics

Platform analytics endpoints for monitoring business metrics.

### Get Order Analytics

**GET** `/api/v1/admin/analytics/orders`

Get basic order metrics for the specified period.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| days | int | 7 | Number of days to look back |

**Response (200):**
```json
{
  "success": true,
  "data": {
    "periodDays": 7,
    "totalOrders": 1250,
    "gmv": 45678.90,
    "averageOrderValue": 36.54,
    "currency": "USD"
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| periodDays | int | Number of days covered |
| totalOrders | long | Total completed orders |
| gmv | BigDecimal | Gross Merchandise Value |
| averageOrderValue | BigDecimal | Average order value |
| currency | string | Currency code |

**Roles:** ADMIN, PLATFORM

---

## 3. Admin Referral Management

Administrative endpoints for monitoring the referral program.

### Get All Referrals (Admin)

**GET** `/api/v1/admin/referrals`

List all referrals in the system with pagination.

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
    "content": [
      {
        "id": 1,
        "code": "REF-ABC123",
        "referrerId": 100,
        "referrerName": "John Doe",
        "referredId": 150,
        "referredName": "Jane Smith",
        "status": "COMPLETED",
        "rewardAmount": 10.00,
        "currency": "USD",
        "referrerRewarded": true,
        "referredRewarded": true,
        "expiresAt": "2024-02-15T10:30:00",
        "completedAt": "2024-01-20T14:30:00",
        "createdAt": "2024-01-15T10:30:00"
      },
      {
        "id": 2,
        "code": "REF-XYZ789",
        "referrerId": 101,
        "referrerName": "Bob Wilson",
        "referredId": null,
        "referredName": null,
        "status": "PENDING",
        "rewardAmount": 10.00,
        "currency": "USD",
        "referrerRewarded": false,
        "referredRewarded": false,
        "expiresAt": "2024-02-20T09:00:00",
        "completedAt": null,
        "createdAt": "2024-01-20T09:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

**Roles:** ADMIN, PLATFORM

---

## Enumerations

### ReferralStatus

| Value | Description |
|-------|-------------|
| `PENDING` | Referral code created, awaiting use |
| `USED` | Referral code used, awaiting reward criteria completion |
| `COMPLETED` | Referral completed and rewards distributed |
| `EXPIRED` | Referral code has expired |
| `CANCELLED` | Referral code was cancelled |

### Referral Lifecycle

```
1. User generates referral code
   └── Status: PENDING

2. New user signs up with code
   └── Status: USED

3. New user completes qualifying action (e.g., first order)
   └── Status: COMPLETED
   └── Rewards distributed to both users

4. If code expires before use
   └── Status: EXPIRED

5. If code is manually cancelled
   └── Status: CANCELLED
```

---

## Configuration

### Referral Program Settings

| Property | Default | Description |
|----------|---------|-------------|
| `app.referral.reward-amount` | 10.00 | Reward amount per successful referral |
| `app.referral.expiry-days` | 30 | Days until referral code expires |

---

## Data Transfer Objects

### ReferralDto

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Referral ID |
| code | string | Unique referral code |
| referrerId | Long | ID of user who created the referral |
| referrerName | string | Name of referrer |
| referredId | Long | ID of user who used the code (nullable) |
| referredName | string | Name of referred user (nullable) |
| status | ReferralStatus | Current status |
| rewardAmount | BigDecimal | Reward amount in currency |
| currency | string | Currency code (e.g., "USD") |
| referrerRewarded | Boolean | Whether referrer received reward |
| referredRewarded | Boolean | Whether referred user received reward |
| expiresAt | datetime | Expiration timestamp |
| completedAt | datetime | Completion timestamp (nullable) |
| createdAt | datetime | Creation timestamp |

### AnalyticsDto

| Field | Type | Description |
|-------|------|-------------|
| periodDays | int | Number of days covered |
| totalOrders | long | Total completed orders |
| gmv | BigDecimal | Gross Merchandise Value |
| averageOrderValue | BigDecimal | Average order value |
| currency | string | Currency code |

### ReferralStatsDto

| Field | Type | Description |
|-------|------|-------------|
| userId | Long | User ID |
| completedReferrals | long | Number of completed referrals |
| totalRewards | BigDecimal | Total rewards earned |
| rewardPerReferral | BigDecimal | Reward per successful referral |

---

## Error Responses

### 404 Referral Not Found

```json
{
  "success": false,
  "message": "Referral not found"
}
```

### 400 Invalid Referral Code

```json
{
  "success": false,
  "message": "Referral code is expired or invalid"
}
```

### 400 Self-Referral Not Allowed

```json
{
  "success": false,
  "message": "Cannot use your own referral code"
}
```

### 403 Forbidden

```json
{
  "success": false,
  "message": "Access denied"
}
```

---

## Role Requirements Summary

| Endpoint | Roles |
|----------|-------|
| POST `/referrals/generate` | Any authenticated |
| GET `/referrals/my` | Any authenticated |
| GET `/referrals/my/stats` | Any authenticated |
| GET `/referrals/{code}` | Any authenticated |
| GET `/admin/analytics/orders` | ADMIN, PLATFORM |
| GET `/admin/referrals` | ADMIN, PLATFORM |
