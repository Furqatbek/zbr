# Technical Analytics Module Documentation

## Overview

The Technical Analytics module provides comprehensive platform health and performance monitoring for the food delivery system. It collects, processes, and exposes metrics from various system components to enable proactive monitoring and troubleshooting.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Technical Analytics Module                        │
├─────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ │
│  │    REST     │  │   Service   │  │ Collectors  │  │   Redis    │ │
│  │ Controller  │──│    Layer    │──│   Layer     │──│   Cache    │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └────────────┘ │
├─────────────────────────────────────────────────────────────────────┤
│                          Data Sources                                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │   HTTP   │ │ Database │ │ WebSocket│ │ Storage  │ │  Message │  │
│  │   Logs   │ │pg_stat_* │ │   Logs   │ │ Metadata │ │  Queues  │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

## Metrics Categories

### 1. API Performance Metrics

Track HTTP API performance and reliability.

| Metric | Type | Description | Formula |
|--------|------|-------------|---------|
| `requestsPerMinute` | Rate | API throughput | `COUNT(requests) / minutes` |
| `avgResponseTimeMs` | Latency | Mean response time | `AVG(response_time_ms)` |
| `p50/p75/p90/p95/p99` | Percentile | Response time distribution | Percentile calculation |
| `errorRate` | Ratio | Total error rate (4xx + 5xx) | `COUNT(status >= 400) / COUNT(*)` |
| `clientErrorRate` | Ratio | Client error rate (4xx) | `COUNT(status 400-499) / COUNT(*)` |
| `serverErrorRate` | Ratio | Server error rate (5xx) | `COUNT(status >= 500) / COUNT(*)` |
| `timeoutRate` | Ratio | Request timeout rate | `COUNT(is_timeout=true) / COUNT(*)` |

**Health Thresholds:**
- 🟢 Healthy: Error rate < 1%, P99 < 500ms, Timeout < 0.1%
- 🟡 Degraded: Error rate 1-5%, P99 500-2000ms, Timeout 0.1-1%
- 🔴 Critical: Error rate > 5%, P99 > 2000ms, Timeout > 1%

### 2. Backend Resource Metrics

Monitor server resources and infrastructure health.

#### CPU Metrics
| Metric | Description | Warning | Critical |
|--------|-------------|---------|----------|
| `systemCpuUsage` | Overall system CPU | 70% | 90% |
| `processCpuUsage` | JVM process CPU | 70% | 90% |
| `loadPerProcessor` | Load average per core | 0.7 | 0.9 |

#### Memory Metrics
| Metric | Description | Warning | Critical |
|--------|-------------|---------|----------|
| `usagePercentage` | Memory utilization | 75% | 90% |
| `heapUsagePercentage` | JVM heap usage | 80% | 95% |

#### Database Pool Metrics
| Metric | Description | Warning | Critical |
|--------|-------------|---------|----------|
| `activeConnections` | Current active connections | 80% of max | 95% of max |
| `pendingConnections` | Waiting for connection | > 0 | > 5 |

#### Redis Metrics
| Metric | Description | Warning | Critical |
|--------|-------------|---------|----------|
| `pingLatencyMs` | Redis ping latency | 10ms | 50ms |
| `hitRate` | Cache hit ratio | < 90% | < 80% |

#### Message Queue Metrics
| Metric | Description | Warning | Critical |
|--------|-------------|---------|----------|
| `totalQueueDepth` | Messages waiting | 1000 | 10000 |
| `consumerLag` | Consumer behind producer | 1000 | 10000 |

### 3. Database Performance Metrics

Monitor PostgreSQL performance and health.

#### Slow Queries
| Metric | Description |
|--------|-------------|
| `totalSlowQueries` | Queries above threshold |
| `avgDurationMs` | Average slow query duration |
| `maxDurationMs` | Longest query duration |
| `queryTypeBreakdown` | Distribution by SELECT/INSERT/UPDATE/DELETE |

#### Index Usage
| Metric | Description | Warning | Critical |
|--------|-------------|---------|----------|
| `indexHitRate` | Queries using indexes | < 95% | < 90% |
| `unusedIndexesCount` | Indexes never used | > 5 | > 20 |
| `totalSeqScans` | Sequential scans (slow) | High count | Very high |

#### Storage Statistics
| Metric | Description | Warning | Critical |
|--------|-------------|---------|----------|
| `deadTuplePercentage` | Bloat from dead rows | 10% | 20% |
| `dailyGrowthBytes` | Storage growth rate | Unusual spike | Rapid growth |

#### Backup Status
| Metric | Description | Warning | Critical |
|--------|-------------|---------|----------|
| `successRate` | Backup success ratio | < 95% | < 90% |
| `lastSuccessfulBackup` | Time since last backup | > 24h | > 48h |

### 4. WebSocket Metrics

Monitor real-time communication health.

#### Connection Metrics
| Metric | Description |
|--------|-------------|
| `totalActiveConnections` | Current open connections |
| `connectedCouriers` | Active courier connections |
| `connectedConsumers` | Active consumer connections |
| `connectedRestaurants` | Active restaurant connections |
| `peakHour` | Hour with most connections |

#### Message Delivery
| Metric | Description | Warning | Critical |
|--------|-------------|---------|----------|
| `deliverySuccessRate` | Messages delivered | < 99% | < 95% |
| `p99DeliveryDelayMs` | 99th percentile delay | 100ms | 500ms |
| `messagesPerSecond` | Message throughput | - | - |

#### Dropped Connections
| Metric | Description | Warning | Critical |
|--------|-------------|---------|----------|
| `droppedRate` | Forced disconnection rate | 1% | 5% |
| `droppedByReason` | Breakdown by cause | - | - |

### 5. Storage/Upload Metrics

Monitor file storage and upload performance.

#### Storage Overview
| Metric | Description |
|--------|-------------|
| `totalFileCount` | Total files stored |
| `totalStorageSizeGb` | Total storage used |
| `imageStoragePercentage` | Storage used by images |

#### Upload Performance
| Metric | Description | Warning | Critical |
|--------|-------------|---------|----------|
| `successRate` | Upload success ratio | < 99% | < 95% |
| `p90UploadLatencyMs` | 90th percentile upload time | 2000ms | 5000ms |
| `failedUploads` | Failed upload count | > 10/day | > 50/day |

#### Storage Growth
| Metric | Description | Warning | Critical |
|--------|-------------|---------|----------|
| `growthPercentage` | Period growth rate | > 10% | > 25% |
| `projectedSize30Days` | 30-day projection | Near limit | Over limit |

## Health Score Calculation

Each subsystem has a health score (0-100) calculated as:

```
API Score = 100
  - errorPenalty (max 40)
  - responseTimePenalty (max 30)
  - timeoutPenalty (max 30)

Backend Score = 100
  - cpuPenalty (max 25)
  - memoryPenalty (max 25)
  - heapPenalty (max 20)
  - dbPoolPenalty (max 15)
  - redisPenalty (max 15)

Database Score = 100
  - indexHitPenalty (max 30)
  - backupPenalty (max 30)
  - slowQueryPenalty (max 25)
  - deadTuplePenalty (max 15)

WebSocket Score = 100
  - deliveryPenalty (max 40)
  - delayPenalty (max 30)
  - droppedPenalty (max 30)

Storage Score = 100
  - uploadSuccessPenalty (max 40)
  - latencyPenalty (max 30)
  - growthPenalty (max 30)
```

**Overall Health Status:**
- Score ≥ 75: 🟢 HEALTHY
- Score 50-74: 🟡 DEGRADED
- Score < 50: 🔴 CRITICAL

## API Endpoints

### Summary & Health
```
GET /api/v1/analytics/technical/summary?startDate={ISO}&endDate={ISO}
GET /api/v1/analytics/technical/health-scores?startDate={ISO}&endDate={ISO}
POST /api/v1/analytics/technical/refresh
```

### API Performance
```
POST /api/v1/analytics/technical/api-performance
GET /api/v1/analytics/technical/api-performance/realtime
```

### Backend Resources
```
GET /api/v1/analytics/technical/backend-resources
GET /api/v1/analytics/technical/backend-resources/historical?startDate={ISO}&endDate={ISO}
```

### Database Performance
```
POST /api/v1/analytics/technical/database-performance
```

### WebSocket
```
POST /api/v1/analytics/technical/websocket
GET /api/v1/analytics/technical/websocket/realtime
```

### Storage
```
POST /api/v1/analytics/technical/storage
GET /api/v1/analytics/technical/storage/summary
```

## Request/Response Examples

### Technical Summary Request
```bash
curl -X GET "http://localhost:8080/api/v1/analytics/technical/summary?\
startDate=2024-01-01T00:00:00&endDate=2024-01-01T23:59:59"
```

### Technical Summary Response
```json
{
  "overallHealth": "HEALTHY",
  "apiHealthScore": 95,
  "backendHealthScore": 90,
  "databaseHealthScore": 92,
  "websocketHealthScore": 98,
  "storageHealthScore": 96,
  "api": {
    "requestsPerMinute": 1250.5,
    "p99ResponseTimeMs": 185,
    "errorRate": 0.005,
    "timeoutRate": 0.0001,
    "status": "HEALTHY"
  },
  "backend": {
    "cpuUsage": 0.45,
    "memoryUsage": 0.62,
    "heapUsage": 0.58,
    "activeDbConnections": 12,
    "redisLatencyMs": 3,
    "messageQueueBacklog": 150,
    "status": "HEALTHY"
  },
  "database": {
    "slowQueryCount": 15,
    "indexHitRate": 0.985,
    "backupSuccessRate": 1.0,
    "storageSizeBytes": 52428800000,
    "status": "HEALTHY"
  },
  "websocket": {
    "activeConnections": 850,
    "connectedCouriers": 180,
    "connectedConsumers": 620,
    "p99MessageDelayMs": 45,
    "droppedConnectionRate": 0.002,
    "status": "HEALTHY"
  },
  "storage": {
    "totalFiles": 125000,
    "totalStorageGb": 45.8,
    "uploadSuccessRate": 0.998,
    "p90UploadLatencyMs": 650,
    "failedUploads": 12,
    "status": "HEALTHY"
  },
  "activeAlertsCount": 0,
  "generatedAt": "2024-01-01T23:59:59"
}
```

### API Performance Request
```bash
curl -X POST "http://localhost:8080/api/v1/analytics/technical/api-performance" \
  -H "Content-Type: application/json" \
  -d '{
    "startDate": "2024-01-01T00:00:00",
    "endDate": "2024-01-01T23:59:59",
    "topEndpointsLimit": 10,
    "includeHourlyDistribution": true
  }'
```

## Caching Strategy

Redis caching with subsystem-specific TTLs:

| Cache | TTL | Rationale |
|-------|-----|-----------|
| API Metrics | 30s | Balance freshness with query cost |
| Backend Metrics | 10s | Near real-time resource monitoring |
| Database Metrics | 60s | Stats don't change rapidly |
| WebSocket Metrics | 5s | Real-time connection status |
| Storage Metrics | 60s | File stats relatively stable |
| Summary | 30s | Aggregate of all metrics |

## Database Tables

| Table | Purpose | Retention |
|-------|---------|-----------|
| `http_request_logs` | API request tracking | 30 days |
| `slow_query_logs` | Slow query analysis | 30 days |
| `websocket_connection_logs` | Connection tracking | 30 days |
| `websocket_message_logs` | Message delivery | 30 days |
| `storage_metadata` | File metadata | Permanent |
| `message_queue_stats` | Queue statistics | 7 days |
| `backup_logs` | Backup history | Permanent |
| `system_metric_snapshots` | Metric history | 7 days |

## Integration with Monitoring

### Prometheus Metrics
The module exposes metrics compatible with Prometheus via Micrometer:
- `api.performance.*`
- `backend.resource.*`
- `database.performance.*`
- `websocket.*`
- `storage.*`

### Grafana Dashboards
Pre-built dashboards available:
- Technical Health Overview
- API Performance Deep Dive
- Database Performance Analysis
- Real-time WebSocket Monitoring
- Storage Analytics

### Alerting Rules
Recommended alerts:
1. API error rate > 5% for 5 minutes
2. P99 response time > 2s for 5 minutes
3. CPU usage > 90% for 10 minutes
4. Database connection pool > 95% for 5 minutes
5. WebSocket delivery rate < 95%
6. Storage growth > 25% per week

## Troubleshooting

### High Error Rate
1. Check `slowestEndpoints` for problematic routes
2. Review `errorsByStatus` distribution
3. Examine backend resource metrics
4. Check database slow queries

### Slow Response Times
1. Review P99 breakdown by endpoint
2. Check database index usage
3. Verify Redis connectivity and latency
4. Review message queue backlog

### WebSocket Issues
1. Check `droppedByReason` for patterns
2. Review `p99DeliveryDelayMs` trends
3. Verify server resource availability
4. Check network connectivity

### Storage Problems
1. Review `failuresByReason` breakdown
2. Check `growthPercentage` trends
3. Verify storage provider health
4. Review `p90UploadLatencyMs` for timeouts
