# Kitchen Display System (KDS) Documentation

Internal service module for kitchen operations in the Food Delivery Platform.

## Overview

The Kitchen module is an **internal service** with no REST API endpoints. It handles:

- Kitchen ticket generation when orders are accepted
- Real-time updates to Kitchen Display Systems via WebSocket
- Print queue management via RabbitMQ

---

## Architecture

```
Order Accepted
      │
      ▼
┌─────────────────┐
│ KitchenService  │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐  ┌───────────┐
│RabbitMQ│  │ WebSocket │
│ Queue  │  │  Topic    │
└────┬───┘  └─────┬─────┘
     │            │
     ▼            ▼
┌────────┐  ┌───────────┐
│ Printer│  │ KDS Screen│
└────────┘  └───────────┘
```

---

## Components

### KitchenService

**Location:** `com.fooddelivery.kitchen.service.KitchenService`

| Method | Description |
|--------|-------------|
| `generateKitchenTicket(orderId)` | Creates and broadcasts kitchen ticket |
| `updateTicketStatus(orderId, status)` | Broadcasts ticket status change |

---

## Kitchen Ticket Structure

### KitchenTicketDto

```json
{
  "ticketNumber": "R001-0042",
  "orderId": 12345,
  "externalOrderNo": "ORD-2024-12345",
  "restaurantId": 1,
  "orderType": "DELIVERY",
  "tableId": null,
  "items": [
    {
      "name": "Margherita Pizza",
      "quantity": 2,
      "variant": "Large",
      "modifiers": "Extra cheese, No olives",
      "specialInstructions": "Cut into 8 slices"
    },
    {
      "name": "Caesar Salad",
      "quantity": 1,
      "variant": null,
      "modifiers": "Dressing on side",
      "specialInstructions": null
    }
  ],
  "notes": "Please include extra napkins",
  "estimatedPrepTime": 25,
  "createdAt": "2024-01-15T12:30:00"
}
```

### TicketItem Structure

| Field | Type | Description |
|-------|------|-------------|
| name | string | Menu item name |
| quantity | int | Number of items |
| variant | string | Size/variant (Small, Medium, Large) |
| modifiers | string | Selected modifiers/toppings |
| specialInstructions | string | Customer special requests |

---

## WebSocket Integration

### Topics

#### New Kitchen Ticket

**Topic:** `/topic/restaurants/{restaurantId}/kitchen`

**Payload:** `KitchenTicketDto` object

**Subscribe Example (JavaScript):**
```javascript
stompClient.subscribe('/topic/restaurants/1/kitchen', function(message) {
  const ticket = JSON.parse(message.body);
  displayNewTicket(ticket);
});
```

---

#### Ticket Status Update

**Topic:** `/topic/restaurants/{restaurantId}/kitchen/status`

**Payload:**
```json
{
  "orderId": 12345,
  "status": "PREPARING",
  "timestamp": "2024-01-15T12:35:00"
}
```

**Status Values:**
| Status | Description |
|--------|-------------|
| `NEW` | Ticket just received |
| `PREPARING` | Kitchen started cooking |
| `READY` | Food ready for pickup |
| `COMPLETED` | Order handed to courier |

---

## RabbitMQ Integration

### Exchange Configuration

| Setting | Value |
|---------|-------|
| Exchange | `kitchen.exchange` |
| Type | Topic |
| Routing Key | `kitchen.ticket` |

### Message Format

```json
{
  "eventId": "uuid",
  "eventType": "KitchenTicketEvent",
  "aggregateId": "12345",
  "aggregateType": "KitchenTicket",
  "timestamp": "2024-01-15T12:30:00",
  "ticket": { ... KitchenTicketDto ... }
}
```

### Consumer Implementation

```java
@RabbitListener(queues = "kitchen.ticket.queue")
public void handleKitchenTicket(KitchenTicketEvent event) {
    KitchenTicketDto ticket = event.getTicket();
    // Send to thermal printer
    printerService.printTicket(ticket);
}
```

---

## Ticket Number Format

Ticket numbers are generated using the format: `R{restaurantId}-{sequence}`

| Component | Description | Example |
|-----------|-------------|---------|
| R | Prefix | R |
| restaurantId | Restaurant ID (3 digits) | 001 |
| sequence | Daily sequence number | 0042 |

**Full Example:** `R001-0042`

---

## Order Type Support

| Order Type | Table ID | Description |
|------------|----------|-------------|
| `DELIVERY` | null | Delivery order |
| `PICKUP` | null | Customer pickup |
| `DINE_IN` | "T12" | Dine-in with table number |

---

## Integration Points

### Triggered By

The kitchen ticket is generated when:

1. **Order is accepted by restaurant**
   - `OrderService.acceptOrder()` → `KitchenService.generateKitchenTicket()`

2. **Order status changes**
   - `OrderService.updateStatus()` → `KitchenService.updateTicketStatus()`

### Consumed By

1. **Kitchen Display System (KDS)**
   - WebSocket client subscribed to `/topic/restaurants/{id}/kitchen`
   - Displays orders in real-time on kitchen screens

2. **Thermal Printer Service**
   - RabbitMQ consumer listening to `kitchen.ticket.queue`
   - Prints physical tickets for kitchen staff

---

## KDS Display Recommendations

### Ticket Display

```
┌─────────────────────────────────┐
│ #R001-0042        12:30 PM      │
│ ORD-2024-12345    DELIVERY      │
├─────────────────────────────────┤
│ 2x Margherita Pizza (Large)     │
│    → Extra cheese, No olives    │
│    → Cut into 8 slices          │
│                                 │
│ 1x Caesar Salad                 │
│    → Dressing on side           │
├─────────────────────────────────┤
│ Notes: Extra napkins please     │
│ Est. Time: 25 min               │
└─────────────────────────────────┘
```

### Color Coding

| Time Since Creation | Color | Meaning |
|--------------------|-------|---------|
| 0-10 min | Green | On track |
| 10-20 min | Yellow | Attention needed |
| 20+ min | Red | Urgent |

---

## Error Handling

### Order Not Found

If the order ID doesn't exist when generating a ticket:
- Warning logged: `"Order not found for kitchen ticket: {orderId}"`
- No ticket generated
- No exception thrown (graceful degradation)

### WebSocket Disconnection

KDS clients should implement:
- Automatic reconnection with exponential backoff
- Local queue for missed tickets
- Sync mechanism on reconnect

---

## Module Structure

```
kitchen/
├── service/
│   └── KitchenService.java      # Main service logic
├── dto/
│   └── KitchenTicketDto.java    # Ticket data structure
└── event/
    └── KitchenTicketEvent.java  # RabbitMQ event wrapper
```

---

## No REST API

This module intentionally has **no REST API endpoints**. All communication is through:

1. **Internal service calls** from OrderService
2. **WebSocket** for real-time KDS updates
3. **RabbitMQ** for print queue

For order management, use the [Order API](./order-api.md).
