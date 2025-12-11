# Analytics Module Documentation

This document describes the Business Logic Layer for calculating core product metrics in the Food Delivery Platform.

## Table of Contents

1. [Overview](#overview)
2. [Metric Definitions](#metric-definitions)
3. [Calculation Formulas](#calculation-formulas)
4. [Caching Strategy](#caching-strategy)
5. [API Endpoints](#api-endpoints)
6. [Example Queries](#example-queries)
7. [Extending with New KPIs](#extending-with-new-kpis)
8. [Performance Considerations](#performance-considerations)

---

## Overview

The analytics module provides real-time business metrics for the food delivery platform. It uses:

- **Spring Data JPA** for database queries
- **Redis** for caching heavy metrics
- **PostgreSQL** optimized indexes for query performance

### Package Structure

```
analytics/
├── controller/      # REST endpoints
├── service/         # Business logic
├── repository/      # JPA queries
├── dto/             # Data transfer objects
├── model/           # ActivityLog entity
├── mapper/          # MapStruct mappers
├── util/            # Utility classes
└── config/          # Cache configuration
```

---

## Metric Definitions

### 1. User Activity (DAU/WAU/MAU)

| Metric | Definition |
|--------|------------|
| **DAU** | Daily Active Users - Distinct users who performed ANY action today |
| **WAU** | Weekly Active Users - Distinct users in the past 7 days |
| **MAU** | Monthly Active Users - Distinct users in the past 30 days |
| **DAU/MAU Ratio** | Stickiness metric - Higher ratio indicates more engaged users (typical: 10-30%) |

### 2. Order Volume

| Metric | Definition |
|--------|------------|
| **Orders per Day** | Total orders created in a 24-hour period |
| **Orders per Hour** | Distribution of orders by hour to identify peak times |
| **First Orders** | Orders from users placing their first-ever order |
| **Repeat Orders** | Orders from users who have ordered before |
| **Success Rate** | Percentage of orders that completed successfully |
| **Cancellation Rate** | Percentage of orders that were cancelled |

### 3. Conversion Funnel

| Step | Event Type | Description |
|------|------------|-------------|
| 1 | `RESTAURANT_VIEW` | User viewed a restaurant page |
| 2 | `ADD_TO_CART` | User added an item to cart |
| 3 | `CHECKOUT_START` | User started checkout process |
| 4 | `PAYMENT_COMPLETED` | Payment was successful |

### 4. Average Order Value (AOV)

| Metric | Definition |
|--------|------------|
| **AOV** | Total Order Revenue / Number of Completed Orders |
| **Median Order Value** | 50th percentile of order values |
| **Average Items per Order** | Mean number of items in completed orders |

### 5. User Activation

| Metric | Definition |
|--------|------------|
| **First Delivery Count** | Users who completed their first delivery |
| **Activation Time** | Time from registration to first order (in hours) |
| **Referral Usage Rate** | Users who signed up with referral / Total new users |

### 6. Churn

| Metric | Definition | Period |
|--------|------------|--------|
| **User Churn** | Users not ordering in specified period | 30 days |
| **Restaurant Churn** | Restaurants with zero orders | 30 days |
| **Courier Churn** | Couriers with no deliveries | 14 days |

---

## Calculation Formulas

### DAU/MAU Ratio (Stickiness)
```
DAU/MAU Ratio = Daily Active Users / Monthly Active Users
```

### Conversion Rate
```
Overall Conversion Rate = (Payment Completed / Restaurant Views) × 100

Step Conversion Rates:
- View to Cart = (Add to Cart / Restaurant Views) × 100
- Cart to Checkout = (Checkout Start / Add to Cart) × 100
- Checkout to Payment = (Payment Completed / Checkout Start) × 100
```

### Average Order Value
```
AOV = SUM(completed_order_totals) / COUNT(completed_orders)
```

### Activation Time
```
Activation Time = First Order Timestamp - User Registration Timestamp
```

### Churn Rate
```
User Churn Rate = (Users without orders in 30 days / Total users with orders) × 100
Restaurant Churn Rate = (Restaurants with 0 orders in 30 days / Active restaurants) × 100
Courier Churn Rate = (Inactive couriers in 14 days / Verified couriers) × 100
```

---

## Caching Strategy

All metrics are cached in Redis with specific TTLs based on data volatility:

| Cache Name | TTL | Reason |
|------------|-----|--------|
| `analytics:dau_wau_mau` | 5 minutes | User activity is relatively stable |
| `analytics:order_volume` | 1 minute | Orders change frequently during peak hours |
| `analytics:conversion` | 30 seconds | Real-time conversion tracking needed |
| `analytics:aov` | 1 minute | Revenue metrics need moderate refresh |
| `analytics:activation` | 5 minutes | Activation doesn't change rapidly |
| `analytics:churn` | 10 minutes | Long-term metric, less frequent updates |

### Cache Keys

```
analytics:dau_wau_mau::dau          - Daily active users count
analytics:dau_wau_mau::wau          - Weekly active users count
analytics:dau_wau_mau::mau          - Monthly active users count
analytics:dau_wau_mau::full         - Complete DAU/WAU/MAU DTO
analytics:order_volume::daily       - Orders per day
analytics:order_volume::hourly      - Orders per hour
analytics:order_volume::full        - Complete order volume DTO
analytics:conversion::rate          - Overall conversion rate
analytics:conversion::full          - Complete conversion DTO
analytics:conversion::period_N      - Conversion for N days
analytics:aov::today                - Today's AOV
analytics:aov::full                 - Complete AOV DTO
analytics:activation::full          - Complete activation DTO
analytics:churn::full               - Complete churn DTO
```

### Manual Cache Refresh

```bash
# Refresh all caches
POST /api/v1/analytics/cache/refresh

# Refresh specific cache
POST /api/v1/analytics/cache/refresh/dau_wau_mau
```

---

## API Endpoints

All endpoints require `ADMIN` or `PLATFORM` role.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/analytics/activity` | User activity metrics (DAU/WAU/MAU) |
| GET | `/api/v1/analytics/orders` | Order volume metrics |
| GET | `/api/v1/analytics/conversion?days=N` | Conversion funnel metrics |
| GET | `/api/v1/analytics/aov` | Average Order Value metrics |
| GET | `/api/v1/analytics/activation` | User activation metrics |
| GET | `/api/v1/analytics/churn` | Churn metrics |
| GET | `/api/v1/analytics/summary` | Quick summary of all key metrics |
| POST | `/api/v1/analytics/cache/refresh` | Force cache refresh (ADMIN only) |

### Example Responses

#### GET /api/v1/analytics/activity

```json
{
  "success": true,
  "message": "User activity metrics retrieved",
  "data": {
    "dailyActiveUsers": 1500,
    "weeklyActiveUsers": 8000,
    "monthlyActiveUsers": 25000,
    "dauMauRatio": 0.06,
    "dauWauRatio": 0.1875,
    "wauMauRatio": 0.32,
    "referenceDate": "2024-01-15",
    "newUsersToday": 120,
    "newUsersThisWeek": 850,
    "newUsersThisMonth": 3500,
    "calculatedAt": "2024-01-15T14:30:00"
  }
}
```

#### GET /api/v1/analytics/conversion?days=7

```json
{
  "success": true,
  "message": "Conversion metrics retrieved",
  "data": {
    "restaurantViews": 50000,
    "addToCartEvents": 15000,
    "checkoutStartEvents": 8000,
    "paymentCompletedEvents": 5000,
    "overallConversionRate": 10.0,
    "viewToCartRate": 30.0,
    "cartToCheckoutRate": 53.33,
    "checkoutToPaymentRate": 62.5,
    "funnelSteps": [
      {"stepNumber": 1, "stepName": "Restaurant View", "count": 50000, "conversionFromPrevious": 100.0, "dropOffFromPrevious": 0.0},
      {"stepNumber": 2, "stepName": "Add to Cart", "count": 15000, "conversionFromPrevious": 30.0, "dropOffFromPrevious": 70.0},
      {"stepNumber": 3, "stepName": "Checkout Started", "count": 8000, "conversionFromPrevious": 53.33, "dropOffFromPrevious": 46.67},
      {"stepNumber": 4, "stepName": "Payment Completed", "count": 5000, "conversionFromPrevious": 62.5, "dropOffFromPrevious": 37.5}
    ],
    "periodDays": 7,
    "referenceDate": "2024-01-15",
    "calculatedAt": "2024-01-15T14:30:00"
  }
}
```

---

## Example Queries

### Count Distinct Active Users (DAU)
```sql
SELECT COUNT(DISTINCT user_id)
FROM activity_logs
WHERE user_id IS NOT NULL
  AND created_at >= CURRENT_DATE;
```

### Orders Per Hour Distribution
```sql
SELECT EXTRACT(HOUR FROM created_at) as hour, COUNT(*) as count
FROM orders
WHERE created_at >= CURRENT_DATE
  AND created_at < CURRENT_DATE + INTERVAL '1 day'
GROUP BY EXTRACT(HOUR FROM created_at)
ORDER BY hour;
```

### First-Time Orders
```sql
SELECT COUNT(*) FROM orders o
WHERE o.created_at >= CURRENT_DATE
  AND o.created_at = (
    SELECT MIN(o2.created_at)
    FROM orders o2
    WHERE o2.consumer_id = o.consumer_id
  );
```

### User Churn (30-day inactivity)
```sql
WITH users_with_orders AS (
  SELECT DISTINCT consumer_id, MAX(created_at) as last_order
  FROM orders
  GROUP BY consumer_id
)
SELECT
  COUNT(*) as total_users,
  COUNT(CASE WHEN last_order >= NOW() - INTERVAL '30 days' THEN 1 END) as active,
  COUNT(CASE WHEN last_order < NOW() - INTERVAL '30 days' THEN 1 END) as churned
FROM users_with_orders;
```

---

## Extending with New KPIs

### Adding a New Metric

1. **Define the metric** in a DTO class:
```java
@Data
@Builder
public class NewMetricDto {
    private Long value;
    private Double rate;
    private LocalDateTime calculatedAt;
}
```

2. **Add repository query**:
```java
@Query("SELECT COUNT(o) FROM Order o WHERE ...")
Long countNewMetric(@Param("since") LocalDateTime since);
```

3. **Add service method**:
```java
@Cacheable(value = "analytics:new_metric", key = "'full'")
public NewMetricDto getNewMetric() {
    // Implementation
}
```

4. **Add controller endpoint**:
```java
@GetMapping("/new-metric")
public ResponseEntity<ApiResponse<NewMetricDto>> getNewMetric() {
    return ResponseEntity.ok(ApiResponse.success(analyticsService.getNewMetric()));
}
```

5. **Configure cache TTL** in `AnalyticsCacheConfig.java`

---

## Performance Considerations

### Database Indexes

The following indexes optimize analytics queries:

```sql
-- User activity queries (DAU/WAU/MAU)
CREATE INDEX idx_activity_user_event_date ON activity_logs(user_id, event_type, created_at);

-- Conversion funnel queries
CREATE INDEX idx_activity_funnel ON activity_logs(event_type, user_id, created_at) WHERE user_id IS NOT NULL;

-- Order time-based queries
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_status_created ON orders(status, created_at);
```

### Query Optimization Tips

1. **Use date range partitioning** for activity_logs table when data grows large
2. **Pre-aggregate daily metrics** in a summary table for historical analysis
3. **Use read replicas** for analytics queries to avoid impacting main database
4. **Batch activity log inserts** using async processing

### Monitoring Recommendations

- Monitor Redis cache hit rates
- Track query execution times for analytics endpoints
- Set alerts for cache miss rates exceeding 20%
- Monitor database connection pool during analytics queries

### Scaling Strategies

1. **Horizontal Scaling**: Use read replicas for analytics queries
2. **Data Warehousing**: Export historical data to a separate analytics database
3. **Time-Series Optimization**: Consider TimescaleDB for activity logs
4. **Pre-computation**: Calculate and store daily aggregates overnight
