# Notification System

A comprehensive persistent notification system for the food delivery platform, supporting multi-role notifications, order lifecycle events, read/unread tracking, and automated cleanup.

## Overview

The notification system provides:
- **Multi-role targeting**: Notifications for customers, couriers, restaurants, admins, and more
- **Order lifecycle notifications**: Automatic notifications for every order status change
- **Read/unread status tracking**: Track notification state per user
- **Soft delete (dismiss)**: Users can dismiss without permanently deleting
- **TTL and expiration**: Optional time-to-live for notifications
- **Full CRUD operations**: Complete REST API for notification management
- **Event-driven architecture**: Automatic notifications triggered by domain events

## Architecture

```
notification/
├── controller/
│   └── NotificationController.java       # REST API endpoints
├── dto/
│   ├── NotificationCreateDto.java        # Create request
│   ├── NotificationResponseDto.java      # Response with computed fields
│   ├── NotificationListDto.java          # Paginated list response
│   ├── NotificationFilterDto.java        # Query filter parameters
│   ├── NotificationBulkActionDto.java    # Bulk operations
│   ├── NotificationCountDto.java         # Notification counts
│   └── OrderNotificationRequest.java     # Order event notification request
├── event/
│   ├── NotificationEventListener.java    # Domain event handler
│   ├── SupportTicketEvent.java           # Support ticket events
│   ├── PaymentFailedEvent.java           # Payment failure events
│   └── PayoutIssuedEvent.java            # Payout events
├── mapper/
│   └── NotificationMapper.java           # MapStruct entity/DTO mapper
├── model/
│   ├── Notification.java                 # Main entity
│   ├── NotificationRole.java             # Role enum
│   ├── NotificationCategory.java         # Category enum
│   ├── NotificationType.java             # Type enum with ~50 event types
│   ├── NotificationPriority.java         # Priority enum
│   └── NotificationTemplate.java         # Message templates
├── repository/
│   ├── NotificationRepository.java       # JPA repository
│   ├── NotificationTemplateRepository.java
│   └── NotificationSpecifications.java   # Dynamic query specs
├── service/
│   ├── PersistentNotificationService.java    # Service interface
│   └── PersistentNotificationServiceImpl.java # Implementation
└── util/
    ├── NotificationConstants.java        # Constants and configuration
    ├── NotificationTimeUtils.java        # Time formatting utilities
    └── NotificationMessageBuilder.java   # Role-specific message builders
```

## Notification Roles

| Role | Description |
|------|-------------|
| `CUSTOMER` | End-user ordering food |
| `COURIER` | Delivery driver |
| `RESTAURANT` | Restaurant staff/owner |
| `ADMIN` | Platform administrator |
| `SUPPORT` | Customer support agent |
| `FINANCE` | Finance team member |
| `OPERATIONS` | Operations team member |
| `ALL` | Broadcast to all users |

## Notification Categories

| Category | Description |
|----------|-------------|
| `ORDER` | Order-related notifications |
| `FINANCE` | Payment, refund, payout notifications |
| `SUPPORT` | Support ticket notifications |
| `SYSTEM` | System announcements |
| `PROMOTION` | Promotional notifications |
| `ACCOUNT` | Account-related notifications |
| `DELIVERY` | Delivery status notifications |
| `RESTAURANT_OPS` | Restaurant operations |
| `ALERT` | Urgent alerts |

## REST API Endpoints

### Create Notification
```http
POST /api/v1/notifications
Content-Type: application/json

{
  "userId": 1,
  "role": "CUSTOMER",
  "title": "Order Confirmed",
  "message": "Your order #ORD-123 has been confirmed",
  "category": "ORDER",
  "notificationType": "ORDER_ACCEPTED",
  "priority": "HIGH",
  "orderId": 123,
  "metadata": {
    "restaurantName": "Pizza Palace"
  }
}
```

### Get Notification by ID
```http
GET /api/v1/notifications/{id}
```

### Get Notifications with Filters
```http
GET /api/v1/notifications?userId=1&role=CUSTOMER&isRead=false&category=ORDER&page=0&pageSize=20
```

### Search Notifications (POST)
```http
POST /api/v1/notifications/search
Content-Type: application/json

{
  "userId": 1,
  "role": "CUSTOMER",
  "isRead": false,
  "category": "ORDER",
  "createdFrom": "2024-01-01T00:00:00",
  "createdTo": "2024-01-31T23:59:59",
  "page": 0,
  "pageSize": 20,
  "sortBy": "createdAt",
  "sortDirection": "DESC"
}
```

### Get Unread Notifications
```http
GET /api/v1/notifications/user/{userId}/unread?role=CUSTOMER&page=0&pageSize=20
```

### Get Notification Counts
```http
GET /api/v1/notifications/user/{userId}/counts?role=CUSTOMER
```

Response:
```json
{
  "total": 50,
  "unread": 10,
  "read": 40,
  "urgentCount": 2,
  "highPriorityCount": 5,
  "unreadByCategory": {
    "ORDER": 5,
    "FINANCE": 3,
    "SUPPORT": 2
  },
  "unreadByPriority": {
    "URGENT": 2,
    "HIGH": 3,
    "NORMAL": 5
  }
}
```

### Get Unread Count
```http
GET /api/v1/notifications/user/{userId}/unread-count?role=CUSTOMER
```

### Mark as Read
```http
PATCH /api/v1/notifications/{id}/read
```

### Mark All as Read
```http
PATCH /api/v1/notifications/read-all?userId=1&role=CUSTOMER
```

### Mark Batch as Read
```http
PATCH /api/v1/notifications/read-batch
Content-Type: application/json

[1, 2, 3, 4, 5]
```

### Dismiss Notification
```http
PATCH /api/v1/notifications/{id}/dismiss
```

### Bulk Action
```http
POST /api/v1/notifications/bulk-action
Content-Type: application/json

{
  "notificationIds": [1, 2, 3],
  "action": "MARK_READ"
}
```

Actions: `MARK_READ`, `MARK_UNREAD`, `DISMISS`, `DELETE`

### Delete Notification
```http
DELETE /api/v1/notifications/{id}
```

### Delete All for User
```http
DELETE /api/v1/notifications/user/{userId}
```

### Admin: Cleanup Expired
```http
POST /api/v1/notifications/admin/cleanup/expired
```

### Admin: Cleanup Dismissed
```http
POST /api/v1/notifications/admin/cleanup/dismissed?daysOld=7
```

### Admin: Cleanup Old Read
```http
POST /api/v1/notifications/admin/cleanup/read?daysOld=90
```

## Order Lifecycle Notifications

The system automatically creates notifications for order events:

| Event | Recipients |
|-------|------------|
| Order Created | Customer, Restaurant |
| Order Accepted | Customer |
| Order Rejected | Customer, Admin |
| Order Preparing | Customer |
| Order Ready | Customer, Courier |
| Courier Assigned | Customer, Courier, Restaurant |
| Order Picked Up | Customer, Restaurant |
| Order In Transit | Customer |
| Order Delivered | Customer, Restaurant, Courier |
| Order Cancelled | Customer, Restaurant, Courier, Admin |
| Payment Failed | Customer, Admin |
| Payment Received | Customer |
| Refund Processed | Customer |
| Payout Issued | Restaurant/Courier |

## Event-Driven Integration

The `NotificationEventListener` automatically listens to domain events:

```java
// Order status changes trigger notifications
applicationEventPublisher.publishEvent(new OrderStatusChangedEvent(
    orderId, orderNumber, restaurantId, customerId, courierId,
    previousStatus, newStatus, reason
));

// Payment events
applicationEventPublisher.publishEvent(new PaymentConfirmedEvent(...));
applicationEventPublisher.publishEvent(new PaymentFailedEvent(...));

// Support ticket events
applicationEventPublisher.publishEvent(new SupportTicketEvent(
    ticketId, userId, subject, TicketEventType.CREATED, message
));

// Payout events
applicationEventPublisher.publishEvent(new PayoutIssuedEvent(
    payoutId, recipientId, role, amount, currency, method, periodStart, periodEnd
));
```

## Database Schema

Tables:
- `notifications` - Main notification storage
- `notification_templates` - Message templates
- `notification_preferences` - User preferences
- `notification_read_status` - Broadcast read tracking

Key indexes for performance:
- `idx_notifications_user_unread` - Fast unread count queries
- `idx_notifications_user_category` - Category filtering
- `idx_notifications_cleanup` - Efficient cleanup operations

## Configuration

Constants in `NotificationConstants.java`:

```java
// Cache
CACHE_UNREAD_COUNT = "notification_unread_count"
CACHE_TTL_SECONDS = 300

// Pagination
DEFAULT_PAGE_SIZE = 20
MAX_PAGE_SIZE = 100

// Cleanup
DEFAULT_EXPIRED_TTL_HOURS = 168 (7 days)
CLEANUP_DISMISSED_DAYS = 7
CLEANUP_READ_DAYS = 90
```

## Caching

Redis caching is used for:
- Unread counts per user
- Notification counts by category

Cache keys:
- `notification_unread_count:{userId}`
- `notification_unread_count:{userId}-{role}`

## Testing

Unit tests:
- `PersistentNotificationServiceTest` - Service layer tests
- `NotificationControllerTest` - Controller tests
- `NotificationEventListenerTest` - Event listener tests
- `NotificationTimeUtilsTest` - Utility tests

Integration tests:
- `NotificationIntegrationTest` - Full integration with Testcontainers

Run tests:
```bash
./mvnw test -Dtest="*Notification*"
```

## Postman Collection Examples

### Create Order Notification
```
POST {{baseUrl}}/api/v1/notifications
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "userId": 1,
  "role": "CUSTOMER",
  "title": "Order #ORD-100 Placed!",
  "message": "Your order from Pizza Palace has been placed successfully. Total: $25.99",
  "category": "ORDER",
  "notificationType": "ORDER_CREATED",
  "priority": "HIGH",
  "orderId": 100,
  "metadata": {
    "restaurantName": "Pizza Palace",
    "totalAmount": 25.99,
    "itemCount": 3
  },
  "actionUrl": "/orders/100"
}
```

### Get Unread Notifications
```
GET {{baseUrl}}/api/v1/notifications/user/1/unread?role=CUSTOMER&page=0&pageSize=10
Authorization: Bearer {{token}}
```

### Mark All as Read
```
PATCH {{baseUrl}}/api/v1/notifications/read-all?userId=1&role=CUSTOMER
Authorization: Bearer {{token}}
```

### Bulk Dismiss
```
POST {{baseUrl}}/api/v1/notifications/bulk-action
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "notificationIds": [1, 2, 3, 4, 5],
  "action": "DISMISS"
}
```

### Admin Cleanup
```
POST {{baseUrl}}/api/v1/notifications/admin/cleanup/expired
Authorization: Bearer {{adminToken}}
```

## Security

- Create operations require `ADMIN` or `SYSTEM` role
- User-specific endpoints validate ownership
- Admin cleanup endpoints require `ADMIN` role
- All endpoints require authentication
