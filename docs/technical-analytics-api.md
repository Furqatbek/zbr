# Technical Analytics API Documentation

Platform health and performance metrics for the Food Delivery Platform.

## Base URL

```
/api/v1/analytics/technical
```

---

## 1. Technical Summary

**GET** `/summary`

Returns a high-level summary of all technical metrics with health scores.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| startDate | DateTime | Yes | Start of analysis period (ISO format) |
| endDate | DateTime | Yes | End of analysis period (ISO format) |

**Response:**
```json
{
  "apiHealthScore": 95,
  "backendHealthScore": 88,
  "databaseHealthScore": 92,
  "websocketHealthScore": 100,
  "storageHealthScore": 85,
  "overallHealthScore": 92,
  "criticalAlerts": [],
  "warningAlerts": ["Database connection pool at 75% capacity"],
  "generatedAt": "2024-01-15T10:30:00"
}
```

---

## 2. API Performance Metrics

**POST** `/api-performance`

Returns detailed API performance metrics including response times, error rates, and endpoint analysis.

**Request Body:**
```json
{
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59",
  "includeEndpointBreakdown": true,
  "includeErrorDetails": true
}
```

**Response:**
```json
{
  "totalRequests": 1500000,
  "averageResponseTimeMs": 125,
  "p50ResponseTimeMs": 85,
  "p95ResponseTimeMs": 250,
  "p99ResponseTimeMs": 450,
  "errorRate": 0.5,
  "successRate": 99.5,
  "requestsPerSecond": 580,
  "endpointBreakdown": [
    {
      "endpoint": "/api/v1/orders",
      "method": "POST",
      "requestCount": 50000,
      "avgResponseTimeMs": 150,
      "errorRate": 0.3
    }
  ],
  "errorDetails": {
    "400": 2500,
    "401": 1200,
    "500": 300,
    "503": 50
  },
  "generatedAt": "2024-01-15T10:30:00"
}
```

**GET** `/api-performance/realtime`

Returns current API performance from Micrometer metrics (no parameters required).

---

## 3. Backend Resource Metrics

**GET** `/backend-resources`

Returns current backend resource usage (real-time).

**Response:**
```json
{
  "cpu": {
    "usagePercent": 45.5,
    "systemLoad": 2.5,
    "availableProcessors": 8
  },
  "memory": {
    "heapUsedMb": 1024,
    "heapMaxMb": 2048,
    "heapUsagePercent": 50.0,
    "nonHeapUsedMb": 128
  },
  "jvm": {
    "uptime": "5d 12h 30m",
    "threadCount": 150,
    "gcPauseTimeMs": 25
  },
  "databasePool": {
    "activeConnections": 15,
    "idleConnections": 5,
    "maxConnections": 50,
    "usagePercent": 30.0
  },
  "redis": {
    "connectedClients": 25,
    "usedMemoryMb": 512,
    "hitRate": 95.5,
    "evictedKeys": 0
  },
  "generatedAt": "2024-01-15T10:30:00"
}
```

**GET** `/backend-resources/historical`

Returns averaged backend metrics for a time period.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| startDate | DateTime | Yes | Start of analysis period |
| endDate | DateTime | Yes | End of analysis period |

---

## 4. Database Performance Metrics

**POST** `/database-performance`

Returns database performance metrics including slow queries, index usage, and backup status.

**Request Body:**
```json
{
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59",
  "includeSlowQueries": true,
  "slowQueryThresholdMs": 1000
}
```

**Response:**
```json
{
  "totalQueries": 5000000,
  "averageQueryTimeMs": 15,
  "slowQueryCount": 250,
  "slowQueryPercentage": 0.005,
  "indexUsageRate": 98.5,
  "tableScans": 150,
  "deadlockCount": 0,
  "slowQueries": [
    {
      "queryHash": "abc123",
      "queryText": "SELECT * FROM orders WHERE...",
      "executionCount": 50,
      "avgDurationMs": 2500,
      "maxDurationMs": 5000,
      "tableName": "orders",
      "isUsingIndex": false
    }
  ],
  "backupStatus": {
    "lastBackupAt": "2024-01-15T03:00:00",
    "backupSizeMb": 15360,
    "backupDurationMinutes": 12,
    "isEncrypted": true,
    "status": "COMPLETED"
  },
  "generatedAt": "2024-01-15T10:30:00"
}
```

---

## 5. WebSocket Metrics

**POST** `/websocket`

Returns WebSocket connection and message delivery metrics.

**Request Body:**
```json
{
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59"
}
```

**Response:**
```json
{
  "totalConnections": 25000,
  "activeConnections": 5000,
  "peakConnections": 8500,
  "connectionSuccessRate": 99.8,
  "averageSessionDurationMinutes": 45,
  "messagesDelivered": 1500000,
  "messageDeliveryRate": 99.95,
  "failedDeliveries": 750,
  "reconnectionRate": 2.5,
  "connectionsByType": {
    "CUSTOMER": 4000,
    "COURIER": 800,
    "RESTAURANT": 200
  },
  "generatedAt": "2024-01-15T10:30:00"
}
```

**GET** `/websocket/realtime`

Returns current WebSocket connection status (no parameters required).

---

## 6. Storage Metrics

**POST** `/storage`

Returns storage and file upload metrics.

**Request Body:**
```json
{
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59"
}
```

**Response:**
```json
{
  "totalStorageUsedGb": 256.5,
  "storageLimit": 1000,
  "usagePercent": 25.65,
  "totalFiles": 150000,
  "uploadCount": 5000,
  "uploadSuccessRate": 99.5,
  "averageUploadSizeKb": 512,
  "storageByType": {
    "RESTAURANT_IMAGE": 100.5,
    "MENU_IMAGE": 50.2,
    "PROFILE_IMAGE": 25.8,
    "DOCUMENT": 80.0
  },
  "failedUploads": 25,
  "errorBreakdown": {
    "FILE_TOO_LARGE": 10,
    "INVALID_FORMAT": 8,
    "NETWORK_ERROR": 7
  },
  "generatedAt": "2024-01-15T10:30:00"
}
```

**GET** `/storage/summary`

Returns current storage totals (no parameters required).

---

## 7. Health Scores

**GET** `/health-scores`

Returns health scores for all subsystems (0-100 scale).

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| startDate | DateTime | Yes | Start of analysis period |
| endDate | DateTime | Yes | End of analysis period |

**Response:**
```json
{
  "apiHealthScore": 95,
  "backendHealthScore": 88,
  "databaseHealthScore": 92,
  "websocketHealthScore": 100,
  "storageHealthScore": 85
}
```

### Health Score Calculation

| Subsystem | Excellent (90-100) | Good (70-89) | Warning (50-69) | Critical (<50) |
|-----------|-------------------|--------------|-----------------|----------------|
| API | Error rate <1%, P95 <300ms | Error rate <3%, P95 <500ms | Error rate <5% | Error rate >5% |
| Backend | CPU <50%, Memory <70% | CPU <70%, Memory <85% | CPU <85% | CPU >85% |
| Database | Slow queries <0.1% | Slow queries <1% | Slow queries <5% | Slow queries >5% |
| WebSocket | Success rate >99.5% | Success rate >98% | Success rate >95% | Success rate <95% |
| Storage | Usage <50%, Success >99% | Usage <75% | Usage <90% | Usage >90% |

---

## 8. Cache Management

**POST** `/refresh`

Forces a refresh of all cached technical metrics.

**Response:** `200 OK` (empty body)

---

## Caching Strategy

| Cache Name | TTL | Description |
|------------|-----|-------------|
| `technical:summary` | 30 seconds | Overall summary |
| `technical:api_performance` | 15 seconds | API metrics |
| `technical:backend_resources` | 10 seconds | Backend metrics |
| `technical:database_performance` | 1 minute | DB metrics |
| `technical:websocket` | 30 seconds | WebSocket metrics |
| `technical:storage` | 5 minutes | Storage metrics |

---

## Monitoring Alerts

The technical analytics system automatically generates alerts:

| Alert Type | Condition | Severity |
|------------|-----------|----------|
| High Error Rate | API error rate > 5% | CRITICAL |
| Slow Response | P95 > 1000ms | WARNING |
| High CPU | CPU usage > 85% for 5 min | CRITICAL |
| Memory Pressure | Heap usage > 90% | CRITICAL |
| Slow Queries | Slow query rate > 5% | WARNING |
| WebSocket Failures | Connection success < 95% | WARNING |
| Storage Full | Usage > 90% | CRITICAL |
| DB Pool Exhaustion | Active connections > 90% | CRITICAL |
