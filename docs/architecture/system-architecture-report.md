# System Architecture Audit Report

## 1. Is it Event-Driven or Function-Based?

**Hybrid — primarily request-driven with event-driven side effects.**

```
┌─────────────────────────────────────────────────────────────────┐
│                    REQUEST FLOW (Synchronous)                    │
│                                                                  │
│  Mobile App ──HTTP──▶ Controller ──▶ Service ──▶ Repository     │
│                                        │                         │
│                                        │ (side effect)           │
│                                        ▼                         │
│                              EventPublisher.publishAsync()       │
│                                        │                         │
│                                        ▼                         │
│              ┌─────────────────────────────────────────┐        │
│              │         EVENT FLOW (Asynchronous)        │        │
│              │                                          │        │
│              │  RabbitMQ ──▶ Consumer ──▶ Notification  │        │
│              │                        ──▶ WebSocket     │        │
│              │                        ──▶ Kitchen       │        │
│              │                        ──▶ Analytics     │        │
│              │                        ──▶ Compensation  │        │
│              └─────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────────┘
```

**Evidence:**

| Pattern | Count | Verdict |
|---------|-------|---------|
| `@Transactional` (read-write) | 115 | Synchronous DB writes dominate |
| `@Transactional(readOnly=true)` | 105 | Synchronous DB reads dominate |
| `@RabbitListener` consumers | 11 | Async event consumers |
| `rabbitTemplate.convertAndSend` | 10 | Async event publishers |
| `@Async` methods | 26 | Fire-and-forget side effects |
| `@TransactionalEventListener` | 3 | Spring domain events |

**Core business logic (orders, payments, courier assignment) is synchronous.** Events are fired AFTER the transaction commits for side effects: notifications, WebSocket push, analytics tracking, kitchen display, compensation.

---

## 2. RabbitMQ Event Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                         RABBITMQ TOPOLOGY                             │
│                                                                       │
│  ┌──────────────┐     ┌─────────────────────┐     ┌───────────────┐ │
│  │ order.exchange│────▶│ order.created.queue  │────▶│OrderEvent     │ │
│  │ (Topic)       │────▶│ order.status.queue   │────▶│Consumer       │ │
│  └──────────────┘     └─────────────────────┘     └───────────────┘ │
│                                                                       │
│  ┌──────────────┐     ┌─────────────────────┐     ┌───────────────┐ │
│  │payment.exchange────▶│ payment.confirmed.q  │────▶│OrderEvent     │ │
│  │ (Topic)       │────▶│ payment.failed.queue │────▶│PaymentEvent   │ │
│  └──────────────┘     └─────────────────────┘     │Consumer       │ │
│                                                    └───────────────┘ │
│  ┌──────────────┐     ┌─────────────────────┐     ┌───────────────┐ │
│  │courier.exchange────▶│ courier.assigned.q   │────▶│OrderEvent     │ │
│  │ (Topic)       │────▶│ courier.location.q   │────▶│LocationConsumr│ │
│  └──────────────┘     └─────────────────────┘     └───────────────┘ │
│                                                                       │
│  ┌──────────────┐     ┌─────────────────────┐     ┌───────────────┐ │
│  │notif.exchange │────▶│ notification.email.q │────▶│EmailConsumer  │ │
│  │ (Topic)       │────▶│ notification.sms.q   │────▶│SmsConsumer    │ │
│  │               │────▶│ notification.push.q  │────▶│PushConsumer   │ │
│  └──────────────┘     └─────────────────────┘     └───────────────┘ │
│                                                                       │
│  ┌──────────────┐     ┌─────────────────────┐     ┌───────────────┐ │
│  │kitchen.exchng │────▶│ kitchen.ticket.queue │────▶│KitchenConsumer│ │
│  └──────────────┘     └─────────────────────┘     └───────────────┘ │
│                                                                       │
│  ALL 10 QUEUES ──(on failure)──▶ dlx.exchange ──▶ dlq.queue          │
└──────────────────────────────────────────────────────────────────────┘
```

**5 exchanges, 10 queues, 11 consumers, 1 DLQ.** All queues are durable with dead-letter routing.

### Event Flow Detail

| Exchange | Queue | Routing Key | Consumer | Processes |
|----------|-------|-------------|----------|-----------|
| order.exchange | order.created.queue | order.created | OrderEventConsumer | Creates notifications for new orders |
| order.exchange | order.status.changed.queue | order.status.changed | OrderEventConsumer | Routes status-specific notifications (ACCEPTED, PREPARING, READY, etc.) |
| payment.exchange | payment.confirmed.queue | payment.confirmed | OrderEventConsumer | Payment received notifications |
| payment.exchange | payment.failed.queue | payment.failed | PaymentEventConsumer | Payment failure notifications |
| courier.exchange | courier.assigned.queue | courier.assigned | OrderEventConsumer | Courier assignment notifications |
| courier.exchange | courier.location.queue | courier.location | CourierLocationConsumer | Updates DB + broadcasts to WebSocket |
| notification.exchange | notification.email.queue | notification.email | EmailNotificationConsumer | Sends emails |
| notification.exchange | notification.sms.queue | notification.sms | SmsMessageConsumer | Sends SMS via Eskiz |
| notification.exchange | notification.push.queue | notification.push | PushNotificationConsumer | Sends FCM push notifications |
| kitchen.exchange | kitchen.ticket.queue | kitchen.ticket | KitchenTicketConsumer | Forwards to KDS WebSocket |

### RabbitMQ Retry Configuration

```
Initial interval: 1000ms
Max attempts: 3
Multiplier: 2x (1s → 2s → 4s)
Requeue rejected: false (sends to DLQ)
```

---

## 3. Cache Architecture (Redis)

### Overview

```
┌──────────────────────────────────────────────────────────────┐
│                        REDIS (Single Instance)                │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐ │
│  │  PRIMARY     │  │  ANALYTICS   │  │  DASHBOARD          │ │
│  │  CacheManager│  │  CacheManager│  │  CacheManager       │ │
│  │  (8 caches)  │  │  (6 caches)  │  │  (7 caches)         │ │
│  │  1-15 min    │  │  30s-10min   │  │  5-30 sec           │ │
│  └─────────────┘  └──────────────┘  └─────────────────────┘ │
│                                                               │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐ │
│  │  FINANCIAL   │  │  FRAUD       │  │  CX                 │ │
│  │  CacheManager│  │  CacheManager│  │  CacheManager       │ │
│  │  (8 caches)  │  │  (4 caches)  │  │  (6 caches)         │ │
│  │  1-5 min     │  │  30s-5min    │  │  1-10 min           │ │
│  └─────────────┘  └──────────────┘  └─────────────────────┘ │
│                                                               │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐ │
│  │  TECHNICAL   │  │  OPERATIONS  │  │  NON-CACHE          │ │
│  │  CacheManager│  │  CacheManager│  │  (RedisTemplate)    │ │
│  │  (6 caches)  │  │  (6 caches)  │  │  Rate limiting      │ │
│  │  5-60 sec    │  │  30s-5min    │  │  Health monitoring   │ │
│  └─────────────┘  └──────────────┘  └─────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

**8 independent CacheManagers, 51 caches total, TTLs from 5 seconds to 15 minutes.**

### Primary CacheManager (core business data)

| Cache Name | TTL | Key Pattern | Cached DTO |
|---|---|---|---|
| `users` | 15 min | `#id` | UserDto |
| `restaurants` | 5 min | `#id` or `'slug:' + #slug` | RestaurantDto |
| `menus` | 5 min | `#restaurantId` or `'full:' + #restaurantId` | List\<MenuCategoryDto\> |
| `menuItems` | 5 min | `#itemId` | MenuItemDto |
| `deliveryFeeSettings` | 10 min | `'all'` | DeliveryFeeSettingsDto |
| `orders` | 1 min | (configured, not used by @Cacheable) | - |
| `courierLocations` | 30 sec | (configured, not used by @Cacheable) | - |
| `rateLimit` | 1 min | (configured, not used by @Cacheable) | - |

### Dashboard CacheManager (admin real-time)

| Cache Name | TTL | Key Pattern |
|---|---|---|
| `dashboardOverview` | 15 sec | date range hash |
| `dashboardActiveOrders` | 5 sec | filter hash |
| `dashboardStuckOrders` | 10 sec | filter hash |
| `dashboardRestaurantMetrics` | 30 sec | filter hash |
| `dashboardCourierMetrics` | 15 sec | filter hash |
| `dashboardFinanceMetrics` | 30 sec | filter hash |
| `dashboardSupportMetrics` | 30 sec | filter hash |

### Analytics CacheManagers (6 managers, 36 caches)

| Manager | Caches | TTL Range |
|---|---|---|
| Analytics (core) | 6 | 30 sec - 10 min |
| Financial | 8 | 1 - 5 min |
| Fraud | 4 | 30 sec - 5 min |
| CX | 6 | 1 - 10 min |
| Technical | 6 | 5 - 60 sec |
| Operations | 6 | 30 sec - 5 min |

### Cache Jitter

**NOT IMPLEMENTED.** All 51 caches use fixed TTLs with zero randomization. This is a stampede risk — when multiple entries expire simultaneously, all requests hit the database at once.

### ETags

**NOT IMPLEMENTED.** No `ETag`, `If-None-Match`, or `If-Match` headers used anywhere. All caching is server-side Redis only — no HTTP-level cache validation for mobile clients.

### Cache Error Handling

Custom `RedisCacheErrorHandler` implemented:
- **GET fails** (deserialization error): Logs warning, evicts stale entry, returns cache miss
- **PUT/EVICT/CLEAR fails**: Logs warning, continues (non-blocking)

### Non-Cache Redis Usage

| Usage | Purpose |
|---|---|
| `RateLimitService` via RedisTemplate | Token bucket rate limiting (Bucket4j) |
| `BackendResourceCollector` via RedisTemplate | Redis health check (PING, INFO) for monitoring |

---

## 4. Database: ACID or BASE?

**ACID. PostgreSQL with strict relational constraints.**

```
┌─────────────────────────────────────────────────────────┐
│                    PostgreSQL (ACID)                      │
│                                                          │
│  ┌────────────┐  ┌────────────┐  ┌────────────────────┐│
│  │ Atomicity   │  │ Consistency │  │ Isolation           ││
│  │ @Transaction│  │ FK, UNIQUE, │  │ PESSIMISTIC_WRITE  ││
│  │ rollback on │  │ NOT NULL,   │  │ on order accept    ││
│  │ exception   │  │ CHECK       │  │ OPTIMISTIC @Version││
│  └────────────┘  └────────────┘  │ on 7 entities      ││
│                                   └────────────────────┘│
│  ┌────────────────────────────────────────────────────┐ │
│  │ Durability                                          │ │
│  │ Flyway migrations (V1-V29), WAL, fsync             │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

| Property | Implementation |
|----------|---------------|
| **Atomicity** | `@Transactional` on all write operations (115 methods). Rollback on any exception. |
| **Consistency** | UNIQUE (email, phone, slug, order_no), NOT NULL constraints, CHECK (rating 1-5). |
| **Isolation** | 2x `PESSIMISTIC_WRITE` locks (order accept, credit update), 7x `@Version` optimistic locking (User, Restaurant, MenuItem, Order, Payment, Courier, Referral) |
| **Durability** | PostgreSQL default (WAL + fsync), Flyway managed schema (29 migrations) |

### Database Configuration

```yaml
ddl-auto: validate           # Never auto-modifies schema
dialect: PostgreSQLDialect
hikari:
  max-pool: 20
  min-idle: 5
  connection-timeout: 20s
  idle-timeout: 300s
  max-lifetime: 1200s
```

### Locking Strategy

| Lock Type | Where | Purpose |
|-----------|-------|---------|
| `PESSIMISTIC_WRITE` | `OrderRepository.findByIdForCourierAssignment()` | Prevents race condition: two couriers accepting same order |
| `PESSIMISTIC_WRITE` | `CustomerCreditRepository.findByUserIdWithLock()` | Safe concurrent credit updates |
| `OPTIMISTIC` (`@Version`) | User, Restaurant, MenuItem, Order, Payment, Courier, Referral | Detects concurrent modifications, throws OptimisticLockException |

### Distributed Transaction Pattern

**No XA/2PC.** Uses event-driven compensation (saga-like) instead:

```
Order Created ──▶ Payment Confirmed ──▶ Courier Assigned ──▶ Delivered
     │                   │                    │                  │
     │ (if fails)        │ (if fails)         │ (if stuck)       │ (if late)
     ▼                   ▼                    ▼                  ▼
  Auto-cancel        PaymentFailed       Reassignment      Compensation
  (30 min timeout)   Event → DLQ         Service           Service
                                          (find new courier) (issue credit)
```

---

## 5. Full System Component Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENTS                                     │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐  ┌──────────────────┐  │
│  │Consumer  │  │Courier   │  │Restaurant │  │Admin Dashboard   │  │
│  │Mobile App│  │Mobile App│  │Dashboard  │  │Web Panel         │  │
│  └────┬─────┘  └────┬─────┘  └─────┬─────┘  └────────┬─────────┘  │
│       │              │              │                  │             │
│       └──────────────┴──────────────┴──────────────────┘             │
│                          │ HTTPS + WSS                               │
└──────────────────────────┼───────────────────────────────────────────┘
                           │
                    ┌──────▼──────┐
                    │   Nginx     │
                    │ (SSL, WS    │
                    │  upgrade)   │
                    └──────┬──────┘
                           │
┌──────────────────────────▼───────────────────────────────────────────┐
│                   SPRING BOOT APPLICATION (:8080)                     │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │                     API LAYER (Controllers)                      │ │
│  │  Auth  │ Order  │ Restaurant │ Courier │ Notification │ Admin   │ │
│  │  CORS  │ Rate Limiting (Bucket4j + Redis) │ JWT Authentication │ │
│  └────────────────────────┬────────────────────────────────────────┘ │
│                           │                                          │
│  ┌────────────────────────▼────────────────────────────────────────┐ │
│  │                    SERVICE LAYER                                 │ │
│  │                                                                  │ │
│  │  OrderService          RestaurantService     CourierService      │ │
│  │  PaymentService        MenuService           UserService         │ │
│  │  DeliveryFeeService    ReviewService         CompensationService │ │
│  │  PromoService          KitchenService        EscalationService   │ │
│  │                                                                  │ │
│  │  ┌──────────────────────────────────────────────────────────┐   │ │
│  │  │ Cross-cutting: @Transactional, @Auditable, @RateLimited │   │ │
│  │  │                @Cacheable, @CacheEvict, @Async           │   │ │
│  │  └──────────────────────────────────────────────────────────┘   │ │
│  └────────────────────────┬────────────────────────────────────────┘ │
│                           │                                          │
│  ┌────────────────────────▼────────────────────────────────────────┐ │
│  │                  DATA ACCESS LAYER                               │ │
│  │  Spring Data JPA Repositories                                    │ │
│  │  PESSIMISTIC_WRITE (order accept, credit)                        │ │
│  │  @Version optimistic locking (7 entities)                        │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │                  REAL-TIME LAYER                                  │ │
│  │  WebSocket (STOMP)           EventPublisher                      │ │
│  │  8 topic channels            @Async fire-and-forget              │ │
│  │  JWT auth on connect         @TransactionalEventListener         │ │
│  │  Auto OFFLINE on disconnect  26 async methods                    │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │                  ANALYTICS LAYER                                  │ │
│  │  8 CacheManagers  │  51 Redis caches  │  6 analytics domains    │ │
│  │  Financial │ Fraud │ CX │ Operations │ Technical │ Dashboard    │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │                  ASYNC EXECUTOR POOLS                             │ │
│  │  taskExecutor (5-20 threads)    │ General async tasks            │ │
│  │  notificationExecutor (3-10)    │ Email, SMS, Push dispatch      │ │
│  │  eventExecutor (3-10)           │ RabbitMQ event publishing      │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │                  SCHEDULED TASKS                                  │ │
│  │  OTP cleanup (daily 3am)    │ Stuck delivery check (5 min)      │ │
│  │  SMS token refresh (25 day) │ SLA breach check (5 min)          │ │
│  │  Auto-cancel unpaid (30min) │ Courier reassignment              │ │
│  └─────────────────────────────────────────────────────────────────┘ │
└───────┬──────────────┬──────────────┬──────────────┬─────────────────┘
        │              │              │              │
        ▼              ▼              ▼              ▼
┌──────────────┐ ┌──────────┐ ┌──────────┐ ┌────────────────┐
│ PostgreSQL   │ │  Redis   │ │ RabbitMQ │ │ External APIs  │
│              │ │          │ │          │ │                │
│ 29 tables    │ │ 51 caches│ │5 exchanges│ │ Eskiz SMS     │
│ ACID         │ │ 8 mgrs   │ │10 queues │ │ Firebase FCM  │
│ Flyway V1-29│ │ no jitter│ │ 1 DLQ    │ │ OSRM routing  │
│ @Version x7  │ │ no ETags │ │ DLX      │ │ SMTP email    │
│ PESSIM_WR x2│ │ 5s-15min │ │          │ │               │
└──────────────┘ └──────────┘ └──────────┘ └────────────────┘
```

---

## 6. Resilience Patterns

| Pattern | Status | Detail |
|---------|--------|--------|
| Rate limiting | Implemented | Bucket4j + Redis, 60 req/min, burst 10 |
| Retry | Implemented | RabbitMQ: 3 attempts, 2x backoff (1s→2s→4s). Spring Retry on notification data fetch. |
| DLQ | Implemented | All 10 RabbitMQ queues route failed messages to `dlq.queue` via `dlx.exchange` |
| Optimistic locking | Implemented | `@Version` on 7 entities (User, Restaurant, MenuItem, Order, Payment, Courier, Referral) |
| Pessimistic locking | Implemented | Order courier assignment + customer credit update |
| Compensation (Saga) | Implemented | Event-driven: auto-cancel unpaid, courier reassignment, customer credit for late delivery |
| Circuit breaker | **Missing** | No Resilience4j / Hystrix on external calls (SMS, OSRM, FCM) |
| Bulkhead | **Missing** | No thread pool isolation between domains |
| Timeout on external calls | Partial | OSRM has 5s timeout. SMS/FCM have none configured. |
| Cache jitter | **Missing** | All 51 caches use fixed TTLs. Stampede risk on expiration. |
| Cache warming | **Missing** | No pre-population on startup |
| ETags | **Missing** | No HTTP-level cache validation for mobile clients |
| Publisher confirms | **Missing** | RabbitMQ messages could be lost if broker crashes |

---

## 7. Summary Scorecard

| Question | Answer |
|----------|--------|
| **Event-driven or function-based?** | **Hybrid.** Synchronous request/response for core business logic (223 @Transactional methods). Async events (RabbitMQ + @Async) for side effects only (notifications, analytics, WebSocket, compensation). |
| **Cache jitter?** | **No.** All 51 caches use fixed TTLs with zero randomization. Thundering herd risk on high-traffic caches. |
| **ACID or BASE?** | **ACID.** PostgreSQL with full transactional integrity, FK constraints, optimistic + pessimistic locking. Events are fire-and-forget side effects, not the source of truth. No eventual consistency in core data model. |
| **Production ready?** | **Mostly.** Missing circuit breakers on external APIs, cache jitter, ETags, and RabbitMQ publisher confirms. Good foundations otherwise. |
