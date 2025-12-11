# Admin Dashboard API Documentation

## Overview

The Admin Dashboard provides real-time operational insights for administrators, covering orders, restaurants, couriers, finance, and customer support.

## Base URL

```
/api/v1/admin/dashboard
```

## Authentication

All endpoints require authentication. Access is controlled by role-based permissions:

| Role | Accessible Endpoints |
|------|---------------------|
| ADMIN | All endpoints |
| OPERATIONS_MANAGER | Overview, Orders, Restaurants, Couriers |
| FINANCE_MANAGER | Overview, Finance |
| SUPPORT_MANAGER | Overview, Support |
| SUPPORT_AGENT | Active Orders, Support |
| RESTAURANT_MANAGER | Restaurants |
| FLEET_MANAGER | Couriers |

---

## Endpoints

### 1. Dashboard Overview

**GET** `/overview`

Returns comprehensive dashboard overview with key metrics.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| startDate | date | No | Start date (ISO format, defaults to today) |
| endDate | date | No | End date (ISO format, defaults to now) |

**Response:**
```json
{
  "ordersToday": 150,
  "revenueToday": 15000.00,
  "activeCouriers": 25,
  "activeRestaurants": 50,
  "avgDeliveryTimeMinutes": 32.5,
  "orderComparison": {
    "previousPeriodOrders": 140,
    "changePercent": 7.14,
    "trend": "UP"
  },
  "systemStatus": {
    "status": "HEALTHY",
    "apiLatencyMs": 150.0,
    "dbLoadPercent": 45.0,
    "redisLatencyMs": 5.0,
    "errorRate": 0.1,
    "healthScore": 95.0
  },
  "generatedAt": "2024-01-15T10:30:00"
}
```

---

### 2. Active Orders

**GET** `/orders/active`

Returns all active orders with optional pagination.

**POST** `/orders/active`

Returns active orders with advanced filtering.

**Query Parameters (GET):**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| date | date | today | Filter by date |
| page | integer | 0 | Page number |
| pageSize | integer | 50 | Items per page |

**Request Body (POST):**
```json
{
  "startDate": "2024-01-15T00:00:00",
  "endDate": "2024-01-15T23:59:59",
  "restaurantIds": [1, 2, 3],
  "courierIds": [10, 20],
  "orderStatuses": ["PENDING", "PREPARING"],
  "page": 0,
  "pageSize": 50,
  "sortBy": "createdAt",
  "sortDirection": "DESC"
}
```

**Response:**
```json
{
  "totalActiveOrders": 45,
  "orders": [
    {
      "orderId": 1001,
      "orderNumber": "ORD-1001",
      "customerId": 501,
      "customerName": "John Doe",
      "restaurantId": 10,
      "restaurantName": "Pizza Palace",
      "courierId": 20,
      "courierName": "Mike Smith",
      "status": "PREPARING",
      "orderTotal": 45.50,
      "createdAt": "2024-01-15T10:00:00",
      "estimatedDeliveryAt": "2024-01-15T10:45:00",
      "deliveryAddress": "123 Main St"
    }
  ],
  "statusBreakdown": {
    "PENDING": 10,
    "ACCEPTED": 5,
    "PREPARING": 15,
    "READY": 5,
    "IN_TRANSIT": 10
  },
  "generatedAt": "2024-01-15T10:30:00"
}
```

---

### 3. Stuck Orders

**GET** `/orders/stuck`

Returns orders that have exceeded their stage thresholds.

**POST** `/orders/stuck`

Returns stuck orders with custom thresholds.

**Request Body (POST):**
```json
{
  "startDate": "2024-01-15T00:00:00",
  "endDate": "2024-01-15T23:59:59",
  "stuckThresholdPending": 10,
  "stuckThresholdAccepted": 15,
  "stuckThresholdPreparing": 30,
  "stuckThresholdReady": 15,
  "stuckThresholdInTransit": 45,
  "page": 0,
  "pageSize": 50
}
```

**Response:**
```json
{
  "totalStuckOrders": 5,
  "criticalCount": 2,
  "highCount": 2,
  "mediumCount": 1,
  "thresholds": {
    "PENDING": 10,
    "ACCEPTED": 15,
    "PREPARING": 30,
    "READY": 15,
    "IN_TRANSIT": 45
  },
  "orders": [
    {
      "orderId": 1005,
      "orderNumber": "ORD-1005",
      "status": "PREPARING",
      "minutesInCurrentStatus": 45,
      "thresholdMinutes": 30,
      "priority": "CRITICAL",
      "priorityScore": 85.0,
      "suggestedAction": "Escalate to restaurant manager",
      "restaurantId": 10,
      "restaurantName": "Pizza Palace",
      "orderTotal": 55.00,
      "createdAt": "2024-01-15T09:00:00"
    }
  ],
  "statusBreakdown": {
    "PREPARING": 3,
    "IN_TRANSIT": 2
  },
  "generatedAt": "2024-01-15T10:30:00"
}
```

---

### 4. Cancelled Orders

**POST** `/orders/cancelled`

Returns cancelled orders with breakdown by reason.

**Response:**
```json
{
  "totalCancelledOrders": 25,
  "cancelledByCustomer": 15,
  "cancelledByRestaurant": 5,
  "cancelledBySystem": 5,
  "totalRefundAmount": 750.00,
  "orders": [...],
  "reasonBreakdown": {
    "CUSTOMER_REQUEST": 10,
    "RESTAURANT_BUSY": 5,
    "OUT_OF_STOCK": 3,
    "NO_COURIER": 2,
    "OTHER": 5
  },
  "hourlyDistribution": {
    "10": 3,
    "11": 5,
    "12": 8,
    "13": 4,
    "14": 5
  },
  "generatedAt": "2024-01-15T10:30:00"
}
```

---

### 5. Restaurant Metrics

**GET** `/restaurants`

Returns restaurant metrics with performance data.

**POST** `/restaurants`

Returns restaurant metrics with advanced filtering.

**Response:**
```json
{
  "totalRestaurants": 100,
  "onlineRestaurants": 75,
  "offlineRestaurants": 20,
  "busyRestaurants": 5,
  "onlinePercentage": 75.0,
  "performanceSummary": {
    "avgPreparationTimeMinutes": 22.5,
    "avgAcceptanceLatencySeconds": 45.0,
    "avgRating": 4.2,
    "avgAcceptanceRate": 92.5,
    "totalOrdersProcessed": 1500,
    "rejectionRate": 3.5
  },
  "restaurants": [
    {
      "restaurantId": 10,
      "name": "Pizza Palace",
      "status": "ONLINE",
      "isOnline": true,
      "rating": 4.5,
      "totalOrdersToday": 45,
      "avgPreparationTimeMinutes": 20.5,
      "orderAcceptanceLatencySeconds": 30.0,
      "acceptanceRate": 95.0,
      "rejectedOrdersToday": 2,
      "cuisineType": "Italian",
      "city": "New York",
      "performanceScore": 85.0
    }
  ],
  "statusBreakdown": {
    "ONLINE": 75,
    "OFFLINE": 20,
    "BUSY": 5
  },
  "cuisineDistribution": {
    "Italian": 25,
    "Chinese": 20,
    "Indian": 15,
    "American": 40
  },
  "generatedAt": "2024-01-15T10:30:00"
}
```

---

### 6. Courier Metrics

**GET** `/couriers`

Returns courier metrics with location data.

**POST** `/couriers`

Returns courier metrics with advanced filtering.

**Response:**
```json
{
  "totalCouriers": 50,
  "activeCouriers": 30,
  "onDeliveryCouriers": 20,
  "availableCouriers": 10,
  "offlineCouriers": 20,
  "activePercentage": 60.0,
  "utilizationRate": 66.67,
  "performanceSummary": {
    "avgDeliveryTimeMinutes": 28.5,
    "avgRating": 4.3,
    "totalDeliveriesToday": 450,
    "onTimeDeliveryRate": 92.0,
    "avgDeliveriesPerCourier": 15.0,
    "avgDistancePerDeliveryKm": 3.5
  },
  "couriers": [
    {
      "courierId": 20,
      "name": "Mike Smith",
      "status": "ON_DELIVERY",
      "isActive": true,
      "rating": 4.7,
      "deliveriesToday": 18,
      "avgDeliveryTimeMinutes": 25.0,
      "onTimeDeliveryRate": 95.0,
      "vehicleType": "MOTORCYCLE",
      "currentLatitude": 40.7128,
      "currentLongitude": -74.0060,
      "currentOrderId": 1001,
      "performanceScore": 88.0
    }
  ],
  "courierLocations": [
    {
      "courierId": 20,
      "courierName": "Mike Smith",
      "latitude": 40.7128,
      "longitude": -74.0060,
      "status": "ON_DELIVERY",
      "currentOrderId": 1001,
      "lastPingAt": "2024-01-15T10:29:00"
    }
  ],
  "statusBreakdown": {
    "AVAILABLE": 10,
    "ON_DELIVERY": 20,
    "RETURNING": 5,
    "ON_BREAK": 3,
    "OFFLINE": 12
  },
  "vehicleDistribution": {
    "MOTORCYCLE": 25,
    "BICYCLE": 15,
    "CAR": 10
  },
  "generatedAt": "2024-01-15T10:30:00"
}
```

---

### 7. Finance Metrics

**GET** `/finance`

Returns financial metrics with GMV and revenue.

**POST** `/finance`

Returns finance metrics with date range.

**Response:**
```json
{
  "gmv": 100000.00,
  "commissionRevenue": 15000.00,
  "deliveryFeeRevenue": 5000.00,
  "totalRevenue": 20000.00,
  "netRevenue": 18000.00,
  "restaurantPayouts": 85000.00,
  "courierPayouts": 10000.00,
  "discountsUsed": 1500.00,
  "refundsPaid": 500.00,
  "unsettledRestaurantPayouts": 5000.00,
  "unsettledCourierPayouts": 1000.00,
  "totalOrders": 2500,
  "avgOrderValue": 40.00,
  "commissionRate": 15.0,
  "payoutSummary": {
    "totalRestaurantPayouts": 85000.00,
    "totalCourierPayouts": 10000.00,
    "pendingRestaurantPayouts": 50,
    "pendingCourierPayouts": 25,
    "completedPayoutsCount": 200,
    "avgSettlementTimeHours": 24.5
  },
  "discountSummary": {
    "totalDiscounts": 1500.00,
    "ordersWithDiscount": 300,
    "discountUsageRate": 12.0,
    "avgDiscountPerOrder": 5.00,
    "discountByType": {
      "PROMO_CODE": 800.00,
      "LOYALTY": 400.00,
      "FIRST_ORDER": 300.00
    }
  },
  "refundSummary": {
    "totalRefundAmount": 500.00,
    "refundCount": 20,
    "approvedRefunds": 18,
    "pendingRefunds": 2,
    "approvalRate": 90.0,
    "refundByReason": {
      "QUALITY_ISSUE": 10,
      "LATE_DELIVERY": 5,
      "WRONG_ORDER": 3,
      "OTHER": 2
    }
  },
  "dailyRevenue": [
    {
      "date": "2024-01-15",
      "gmv": 15000.00,
      "commissionRevenue": 2250.00,
      "deliveryFeeRevenue": 750.00,
      "discounts": 200.00,
      "orderCount": 375,
      "avgOrderValue": 40.00
    }
  ],
  "paymentMethodBreakdown": {
    "CARD": 70000.00,
    "CASH": 20000.00,
    "WALLET": 10000.00
  },
  "generatedAt": "2024-01-15T10:30:00"
}
```

---

### 8. Support Metrics

**GET** `/support`

Returns support metrics with ticket data.

**POST** `/support`

Returns support metrics with filtering.

**Response:**
```json
{
  "totalTickets": 100,
  "openTickets": 25,
  "inProgressTickets": 30,
  "resolvedTickets": 35,
  "closedTickets": 10,
  "escalatedTickets": 5,
  "complaintCount": 40,
  "refundRequestCount": 30,
  "inquiryCount": 20,
  "feedbackCount": 10,
  "avgResolutionTimeMinutes": 45.5,
  "avgResolutionTimeFormatted": "45m",
  "avgFirstResponseTimeMinutes": 8.5,
  "avgFirstResponseTimeFormatted": "8m",
  "customerSatisfactionScore": 4.2,
  "resolutionRate": 45.0,
  "tickets": [
    {
      "ticketId": 501,
      "ticketNumber": "TKT-501",
      "customerId": 101,
      "customerName": "Jane Doe",
      "orderId": 1001,
      "category": "COMPLAINT",
      "priority": "HIGH",
      "status": "IN_PROGRESS",
      "subject": "Late delivery",
      "createdAt": "2024-01-15T09:00:00",
      "assignedAgentId": 10,
      "assignedAgentName": "Agent Smith",
      "waitTimeMinutes": 30,
      "waitTimeFormatted": "30m",
      "slaBreached": false
    }
  ],
  "statusBreakdown": {
    "OPEN": 25,
    "IN_PROGRESS": 30,
    "WAITING_CUSTOMER": 10,
    "RESOLVED": 35
  },
  "priorityBreakdown": {
    "CRITICAL": 5,
    "HIGH": 20,
    "MEDIUM": 50,
    "LOW": 25
  },
  "agentPerformance": [
    {
      "agentId": 10,
      "agentName": "Agent Smith",
      "ticketsHandled": 50,
      "ticketsResolved": 45,
      "resolutionRate": 90.0,
      "avgResolutionTimeMinutes": 40.0,
      "avgSatisfactionScore": 4.5,
      "performanceScore": 88.0
    }
  ],
  "commonIssues": [
    {
      "category": "DELIVERY",
      "subcategory": "Late delivery",
      "count": 25,
      "percentage": 25.0,
      "avgResolutionTimeMinutes": 35.0,
      "trend": "UP"
    }
  ],
  "slaMetrics": {
    "totalTickets": 100,
    "slaMetTickets": 85,
    "slaBreachedTickets": 15,
    "slaComplianceRate": 85.0
  },
  "generatedAt": "2024-01-15T10:30:00"
}
```

---

### 9. Cache Management

**POST** `/cache/refresh`

Refresh all dashboard caches. Admin only.

**Response:**
```json
{
  "status": "success",
  "message": "All caches refreshed"
}
```

**POST** `/cache/refresh/{cacheKey}`

Refresh specific cache. Admin only.

**Valid cache keys:**
- `overview`
- `activeOrders`
- `stuckOrders`
- `restaurants`
- `couriers`
- `finance`
- `support`

---

## Caching Strategy

| Component | TTL | Rationale |
|-----------|-----|-----------|
| Overview | 15s | Frequently changing metrics |
| Active Orders | 5s | Real-time critical |
| Stuck Orders | 10s | High priority monitoring |
| Restaurants | 30s | Moderate update frequency |
| Couriers | 15s | Location tracking |
| Finance | 30s | Less volatile |
| Support | 30s | Less volatile |

---

## Error Responses

| Code | Description |
|------|-------------|
| 400 | Bad Request - Invalid parameters |
| 401 | Unauthorized - Authentication required |
| 403 | Forbidden - Insufficient permissions |
| 500 | Internal Server Error |

**Error Response Format:**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid date format",
  "path": "/api/v1/admin/dashboard/overview"
}
```

---

## Rate Limiting

- Standard rate limit: 100 requests per minute
- Cache refresh endpoints: 10 requests per minute
- Burst allowance: 20 additional requests

---

## Stuck Order Thresholds

Default thresholds (configurable via request):

| Status | Threshold (minutes) | Description |
|--------|---------------------|-------------|
| PENDING | 10 | Awaiting restaurant confirmation |
| ACCEPTED | 15 | Confirmed, not yet preparing |
| PREPARING | 30 | Being prepared |
| READY | 15 | Ready for pickup |
| IN_TRANSIT | 45 | On the way to customer |
