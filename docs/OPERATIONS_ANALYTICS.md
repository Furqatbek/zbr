# Operations Analytics Module

This document describes the Operations Analytics module for the Food Delivery Platform. It provides comprehensive operational metrics for monitoring and optimizing order fulfillment, restaurant performance, courier efficiency, and delivery quality.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Metrics Reference](#metrics-reference)
4. [API Endpoints](#api-endpoints)
5. [Caching Strategy](#caching-strategy)
6. [Database Schema](#database-schema)
7. [Performance Considerations](#performance-considerations)
8. [Usage Examples](#usage-examples)

---

## Overview

The Operations Analytics module tracks and calculates key performance indicators (KPIs) for:

- **Order Fulfillment Time (OFT)** - End-to-end order lifecycle timing
- **Restaurant Performance** - Acceptance rates, preparation times, uptime
- **Courier Performance** - Acceptance rates, delivery times, utilization
- **Delivery Success Rate** - Completion rates, cancellation analysis
- **ETA Accuracy** - Prediction accuracy and error analysis

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        REST Controller                               │
│              OperationsAnalyticsController                          │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Service Layer                                │
│              OperationsAnalyticsServiceImpl                         │
│                    (with @Cacheable)                                │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
            ┌───────────────┼───────────────┐
            ▼               ▼               ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│  Order Repo   │  │  Event Repos  │  │  History Repo │
│               │  │  - Courier    │  │  - ETA        │
│               │  │  - Restaurant │  │  - Menu       │
└───────┬───────┘  └───────┬───────┘  └───────┬───────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        PostgreSQL                                    │
│  orders | courier_* | restaurant_* | eta_history | menu_update_*    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Metrics Reference

### 1. Order Fulfillment Time (OFT)

Tracks the complete order lifecycle from placement to delivery.

```
┌─────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌───────────┐
│ PLACED  │───►│ ACCEPTED │───►│ PREPARED │───►│ PICKED UP│───►│ DELIVERED │
└─────────┘    └──────────┘    └──────────┘    └──────────┘    └───────────┘
     │              │               │               │               │
     └──────────────┴───────────────┴───────────────┴───────────────┘
           │               │               │               │
     Acceptance     Preparation      Pickup Wait      Delivery
        Time           Time            Time            Time
```

| Metric | Formula | Description |
|--------|---------|-------------|
| Total Fulfillment Time | `deliveredAt - placedAt` | End-to-end time |
| Acceptance Time | `acceptedAt - placedAt` | Time for restaurant to accept |
| Preparation Time | `readyAt - acceptedAt` | Kitchen preparation time |
| Pickup Wait Time | `pickedUpAt - readyAt` | Time waiting for courier |
| Delivery Time | `deliveredAt - pickedUpAt` | Transit to customer |

### 2. Restaurant Performance

| Metric | Formula | Description |
|--------|---------|-------------|
| Order Acceptance Rate | `accepted / received × 100` | % of orders accepted |
| Avg Acceptance Time | `AVG(acceptedAt - placedAt)` | Average acceptance delay |
| Avg Preparation Time | `AVG(readyAt - acceptedAt)` | Average prep duration |
| On-time Prep Rate | `preparedOnTime / total × 100` | % prepared within estimate |
| Uptime % | `(totalMinutes - offlineMinutes) / totalMinutes × 100` | Availability |
| Menu Update Frequency | `COUNT(menuUpdates) / days` | Updates per day |

### 3. Courier Performance

| Metric | Formula | Description |
|--------|---------|-------------|
| Acceptance Rate | `accepted / offered × 100` | % of offers accepted |
| Avg Response Time | `AVG(responseTimeSeconds)` | Time to respond to offer |
| Avg Delivery Time | `AVG(deliveredAt - pickedUpAt)` | Transit duration |
| Utilization Rate | `busyTime / activeTime × 100` | Working vs idle ratio |
| Location Update Rate | `COUNT(locationUpdates) / hours` | GPS pings per hour |
| Cancellation Rate | `cancelled / accepted × 100` | % cancelled after accept |

### 4. Delivery Success Rate

| Metric | Formula | Description |
|--------|---------|-------------|
| Success Rate | `delivered / total × 100` | Overall completion rate |
| Cancellation Rate | `cancelled / total × 100` | Orders cancelled |
| On-time Rate | `onTime / delivered × 100` | Within 5 min of estimate |
| Major Delay Rate | `majorDelays / delivered × 100` | > 15 min late |

### 5. ETA Accuracy

| Metric | Formula | Description |
|--------|---------|-------------|
| Avg ETA Error | `AVG(actualTime - predictedTime)` | Mean error (+ = late) |
| Avg Absolute Error | `AVG(ABS(error))` | Mean absolute deviation |
| Accuracy Rate (5 min) | `within5min / total × 100` | % within 5 min |
| Over-estimation Rate | `earlyDeliveries / total × 100` | % delivered early |
| Under-estimation Rate | `lateDeliveries / total × 100` | % delivered late |

---

## API Endpoints

### Base URL: `/api/v1/analytics/operations`

All endpoints require `ADMIN` or `PLATFORM` role.

### GET /order/{orderId}

Get order fulfillment metrics for a specific order.

**Response:**
```json
{
  "success": true,
  "message": "Order fulfillment metrics retrieved",
  "data": {
    "orderId": 12345,
    "externalOrderNo": "ORD-12345",
    "restaurantId": 1,
    "courierId": 5,
    "orderStatus": "DELIVERED",
    "placedAt": "2024-01-15T12:00:00",
    "acceptedAt": "2024-01-15T12:03:00",
    "readyAt": "2024-01-15T12:18:00",
    "pickedUpAt": "2024-01-15T12:26:00",
    "deliveredAt": "2024-01-15T12:41:00",
    "totalFulfillmentTimeMinutes": 41,
    "acceptanceTimeMinutes": 3,
    "preparationTimeMinutes": 15,
    "pickupWaitTimeMinutes": 8,
    "deliveryTimeMinutes": 15,
    "onTime": true
  }
}
```

### GET /restaurant/{restaurantId}

Get restaurant performance metrics.

**Parameters:**
- `startDate` (required): Start date (yyyy-MM-dd)
- `endDate` (required): End date (yyyy-MM-dd)

**Response:**
```json
{
  "success": true,
  "data": {
    "restaurantId": 1,
    "restaurantName": "Pizza Palace",
    "totalOrdersReceived": 500,
    "ordersAccepted": 475,
    "orderAcceptanceRate": 95.0,
    "averageAcceptanceTimeMinutes": 2.5,
    "averagePreparationTimeMinutes": 18.3,
    "medianPreparationTimeMinutes": 17.0,
    "p90PreparationTimeMinutes": 25.0,
    "uptimePercentage": 98.5,
    "totalMenuUpdates": 12,
    "menuUpdatesPerWeek": 3.0
  }
}
```

### GET /courier/{courierId}

Get courier performance metrics.

**Parameters:**
- `startDate` (required): Start date (yyyy-MM-dd)
- `endDate` (required): End date (yyyy-MM-dd)

**Response:**
```json
{
  "success": true,
  "data": {
    "courierId": 5,
    "courierName": "John Doe",
    "totalOrdersOffered": 120,
    "ordersAccepted": 105,
    "acceptanceRate": 87.5,
    "averageResponseTimeSeconds": 12.3,
    "deliveriesCompleted": 100,
    "averageDeliveryTimeMinutes": 12.5,
    "utilizationRate": 72.3,
    "totalLocationUpdates": 5400,
    "locationUpdatesPerHour": 120.0
  }
}
```

### GET /delivery-success

Get delivery success rate metrics.

**Parameters:**
- `startDate` (required): Start date (yyyy-MM-dd)
- `endDate` (required): End date (yyyy-MM-dd)

**Response:**
```json
{
  "success": true,
  "data": {
    "totalOrders": 1250,
    "successfulDeliveries": 1182,
    "successRate": 94.56,
    "cancellationRate": 4.16,
    "onTimeRate": 89.2,
    "majorDelayRate": 2.8,
    "cancelledByCustomer": 25,
    "cancelledByRestaurant": 15,
    "cancelledByCourier": 12
  }
}
```

### GET /eta-accuracy

Get ETA accuracy metrics.

**Parameters:**
- `startDate` (required): Start date (yyyy-MM-dd)
- `endDate` (required): End date (yyyy-MM-dd)

**Response:**
```json
{
  "success": true,
  "data": {
    "totalDeliveries": 1182,
    "averageEtaErrorMinutes": 2.5,
    "averageAbsoluteErrorMinutes": 5.3,
    "accuracyRate5Min": 72.5,
    "accuracyRate10Min": 88.2,
    "overEstimationCount": 423,
    "underEstimationCount": 759,
    "overEstimationRate": 35.8,
    "underEstimationRate": 64.2
  }
}
```

### GET /summary

Get operations summary dashboard.

**Parameters:**
- `startDate` (required): Start date (yyyy-MM-dd)
- `endDate` (required): End date (yyyy-MM-dd)

### POST /cache/refresh

Force refresh all caches. Requires `ADMIN` role.

### POST /cache/refresh/{cacheName}

Refresh specific cache. Requires `ADMIN` role.

---

## Caching Strategy

| Cache Name | TTL | Rationale |
|------------|-----|-----------|
| `ops:order_fulfillment` | 60s | Real-time order tracking |
| `ops:restaurant_performance` | 5 min | Dashboard metrics |
| `ops:courier_performance` | 1 min | Frequent courier updates |
| `ops:delivery_success` | 2 min | Aggregate stats |
| `ops:eta_accuracy` | 30s | Real-time accuracy |
| `ops:summary` | 2 min | Dashboard overview |

### Cache Eviction

Caches are automatically evicted after TTL expires. Manual eviction is available via:

```bash
# Evict all caches
POST /api/v1/analytics/operations/cache/refresh

# Evict specific cache
POST /api/v1/analytics/operations/cache/refresh/order_fulfillment
```

---

## Database Schema

### Event Tables

```sql
-- Courier location tracking
CREATE TABLE courier_location_events (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    recorded_at TIMESTAMP NOT NULL
);

-- Courier order events (offers, accepts, etc.)
CREATE TABLE courier_order_events (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,  -- OFFERED, ACCEPTED, REJECTED, etc.
    response_time_seconds BIGINT,
    event_timestamp TIMESTAMP NOT NULL
);

-- Restaurant order events
CREATE TABLE restaurant_order_events (
    id BIGSERIAL PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,  -- RECEIVED, ACCEPTED, READY, etc.
    actual_prep_minutes INTEGER,
    event_timestamp TIMESTAMP NOT NULL
);

-- ETA predictions vs actual
CREATE TABLE eta_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    eta_shown_minutes INTEGER NOT NULL,
    actual_delivery_minutes INTEGER,
    eta_error_minutes INTEGER,
    was_late BOOLEAN,
    predicted_at TIMESTAMP NOT NULL
);
```

### Key Indexes

```sql
-- For time-range queries
CREATE INDEX idx_coe_courier_timestamp ON courier_order_events(courier_id, event_timestamp);
CREATE INDEX idx_roe_restaurant_timestamp ON restaurant_order_events(restaurant_id, event_timestamp);
CREATE INDEX idx_eta_predicted_at ON eta_history(predicted_at);

-- For fulfillment analysis
CREATE INDEX idx_orders_restaurant_created ON orders(restaurant_id, created_at);
CREATE INDEX idx_orders_courier_created ON orders(courier_id, created_at);
```

---

## Performance Considerations

### Big-O Complexity

| Operation | Complexity | Notes |
|-----------|------------|-------|
| Order Fulfillment | O(1) | Single order lookup |
| Restaurant Performance | O(n) | n = orders in range |
| Courier Performance | O(n) | n = events in range |
| Delivery Success Rate | O(n) | n = orders in range |
| ETA Accuracy | O(n) | n = deliveries in range |

### Query Optimization

1. **Date Range Partitioning**: Consider table partitioning by month for large datasets
2. **Materialized Views**: Pre-compute daily aggregates for historical data
3. **Connection Pooling**: Use HikariCP with appropriate pool size
4. **Read Replicas**: Route analytics queries to read replicas

### Scaling Recommendations

For high-volume deployments (>10K orders/day):

1. **Separate Analytics Service**: Extract to dedicated microservice
2. **Event Streaming**: Use Kafka for real-time event ingestion
3. **Time-Series DB**: Consider TimescaleDB or InfluxDB for metrics
4. **Data Warehouse**: Replicate to BigQuery/Redshift for heavy analytics

---

## Usage Examples

### Sample SQL Queries

**Average fulfillment time by restaurant (last 7 days):**
```sql
SELECT
    r.name,
    AVG(EXTRACT(EPOCH FROM (o.delivered_at - o.created_at)) / 60) as avg_fulfillment_minutes
FROM orders o
JOIN restaurants r ON o.restaurant_id = r.id
WHERE o.delivered_at IS NOT NULL
  AND o.created_at >= NOW() - INTERVAL '7 days'
GROUP BY r.id, r.name
ORDER BY avg_fulfillment_minutes;
```

**Courier acceptance rate ranking:**
```sql
SELECT
    c.id,
    COUNT(CASE WHEN event_type = 'OFFERED' THEN 1 END) as offered,
    COUNT(CASE WHEN event_type = 'ACCEPTED' THEN 1 END) as accepted,
    ROUND(
        COUNT(CASE WHEN event_type = 'ACCEPTED' THEN 1 END) * 100.0 /
        NULLIF(COUNT(CASE WHEN event_type = 'OFFERED' THEN 1 END), 0),
        2
    ) as acceptance_rate
FROM courier_order_events coe
JOIN couriers c ON coe.courier_id = c.id
WHERE event_timestamp >= NOW() - INTERVAL '7 days'
GROUP BY c.id
HAVING COUNT(CASE WHEN event_type = 'OFFERED' THEN 1 END) > 10
ORDER BY acceptance_rate DESC;
```

**ETA accuracy by hour of day:**
```sql
SELECT
    EXTRACT(HOUR FROM predicted_at) as hour,
    AVG(eta_error_abs_minutes) as avg_error,
    COUNT(*) as deliveries
FROM eta_history
WHERE was_completed = true
  AND predicted_at >= NOW() - INTERVAL '30 days'
GROUP BY EXTRACT(HOUR FROM predicted_at)
ORDER BY hour;
```

---

## Future Enhancements

1. **Machine Learning Integration**: Predict delays before they happen
2. **Anomaly Detection**: Alert on unusual patterns
3. **Cohort Analysis**: Track performance trends over time
4. **Geographic Heatmaps**: Visualize performance by area
5. **Real-time Streaming**: WebSocket updates for live dashboards
