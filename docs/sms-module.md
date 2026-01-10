# SMS Module Documentation

Internal SMS notification service for the Food Delivery Platform. Provides asynchronous SMS delivery through the Eskiz SMS gateway using RabbitMQ message queuing.

## Overview

The SMS module is an **internal service** that does not expose its own REST API. Instead, it:
- Receives messages via **RabbitMQ** queues
- Sends SMS through the **Eskiz SMS gateway** (Uzbekistan-based)
- Implements retry logic with exponential backoff
- Supports multiple SMS types (OTP, orders, payments, etc.)

---

## Table of Contents

1. [Architecture](#1-architecture)
2. [SMS Types](#2-sms-types)
3. [Integration Points](#3-integration-points)
4. [Phone Authentication API](#4-phone-authentication-api)
5. [Configuration](#5-configuration)
6. [Message Flow](#6-message-flow)

---

## 1. Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    APPLICATION SERVICES                      │
├─────────────────────────────────────────────────────────────┤
│  AuthService    OrderService    PaymentService    Others     │
│      │               │               │              │        │
└──────┼───────────────┼───────────────┼──────────────┼────────┘
       │               │               │              │
       └───────────────┴───────────────┴──────────────┘
                              │
                              ▼
              ┌───────────────────────────┐
              │   SmsNotificationService  │
              │                           │
              │ • sendOtp()               │
              │ • sendOrderConfirmation() │
              │ • sendDeliveryUpdate()    │
              │ • sendPaymentConfirm()    │
              └───────────────────────────┘
                              │
                              ▼
              ┌───────────────────────────┐
              │   RabbitMQ (Async Queue)  │
              │                           │
              │ Queue: notification.sms   │
              │ Exchange: notification    │
              └───────────────────────────┘
                              │
                              ▼
              ┌───────────────────────────┐
              │   SmsMessageConsumer      │
              │                           │
              │ • Retry logic (3 attempts)│
              │ • Exponential backoff     │
              │ • Dead Letter Queue       │
              └───────────────────────────┘
                              │
                              ▼
              ┌───────────────────────────┐
              │   EskizSmsClient          │
              │                           │
              │ • Token management        │
              │ • Auto-refresh (25 days)  │
              │ • Status tracking         │
              └───────────────────────────┘
                              │
                              ▼
              ┌───────────────────────────┐
              │   Eskiz SMS Gateway       │
              │   (notify.eskiz.uz)       │
              └───────────────────────────┘
```

---

## 2. SMS Types

### SmsType Enum

| Value | Priority | Description |
|-------|----------|-------------|
| `OTP` | 10 | One-Time Password for authentication |
| `PASSWORD_RESET` | 9 | Password reset verification code |
| `ORDER_CONFIRMATION` | 8 | Order confirmed notification |
| `DELIVERY_UPDATE` | 8 | Delivery in progress notification |
| `ORDER_STATUS_UPDATE` | 7 | Order status change notification |
| `PAYMENT_CONFIRMATION` | 7 | Payment received confirmation |
| `WELCOME` | 5 | Welcome message for new users |
| `PROMOTIONAL` | 3 | Marketing/promotional message |
| `GENERAL` | 5 | General/miscellaneous message |

### Message Templates

**OTP:**
```
Your verification code is: {code}
Valid for 5 minutes.
- Food Delivery
```

**Order Confirmation:**
```
Order {orderNo} confirmed!
Restaurant: {restaurantName}
Total: {totalAmount}
Track your order in the app.
```

**Order Status Update:**
```
Order {orderNo}: {status}
{details}
```

**Delivery Update:**
```
Order {orderNo} is on the way!
Courier: {courierName}
ETA: {estimatedTime}
```

**Payment Confirmation:**
```
Payment received for order {orderNo}.
Amount: {amount}
Thank you!
```

**Password Reset:**
```
Your password reset code: {resetCode}
Valid for 1 hour.
If you didn't request this, ignore this message.
```

---

## 3. Integration Points

### From Auth Module (OTP Authentication)

The SMS module handles OTP delivery for phone-based authentication:

```java
// OtpService calls:
smsNotificationService.sendOtp(phoneNumber, otpCode);
```

**Flow:**
1. User requests OTP via `/api/v1/auth/phone/send-otp`
2. AuthService generates 6-digit OTP code
3. OTP saved to database with 5-minute expiry
4. SMS queued to RabbitMQ with priority 10
5. SmsMessageConsumer sends via Eskiz
6. User receives SMS and verifies via `/api/v1/auth/phone/verify`

### From Order Module

Order-related SMS notifications:

```java
// Order created
smsNotificationService.sendOrderConfirmation(phone, orderNo, restaurant, total);

// Status changed
smsNotificationService.sendOrderStatusUpdate(phone, orderNo, status, details);

// Delivery assigned
smsNotificationService.sendDeliveryUpdate(phone, orderNo, courierName, eta);
```

### From Payment Module

Payment confirmations:

```java
smsNotificationService.sendPaymentConfirmation(phone, orderNo, amount);
```

### From User Module

Welcome messages:

```java
smsNotificationService.sendWelcomeSms(user);
```

---

## 4. Phone Authentication API

The SMS module powers the phone authentication endpoints (part of Auth module):

### Send OTP

**POST** `/api/v1/auth/phone/send-otp`

Send OTP verification code to phone number.

**Request:**
```json
{
  "phone": "998901234567"
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Verification code sent",
  "data": {
    "phone": "998***4567",
    "message": "OTP sent successfully",
    "expiresInSeconds": 300,
    "isNewUser": false,
    "remainingAttempts": 3
  }
}
```

**Rate Limit:** 5 OTPs per phone per hour

---

### Verify OTP

**POST** `/api/v1/auth/phone/verify`

Verify OTP and authenticate user.

**Request:**
```json
{
  "phone": "998901234567",
  "code": "123456"
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userId": 100,
    "roles": ["CONSUMER"],
    "user": {...}
  }
}
```

**Validation:**
- OTP must be 6 digits
- Max 3 verification attempts per OTP
- OTP expires after 5 minutes

---

### Resend OTP

**POST** `/api/v1/auth/phone/resend-otp`

Resend OTP verification code. Invalidates previous OTPs.

**Request:**
```json
{
  "phone": "998901234567"
}
```

---

## 5. Configuration

### Application Properties

```yaml
app:
  sms:
    eskiz:
      enabled: true
      base-url: https://notify.eskiz.uz/api
      email: ${SMS_EMAIL}
      password: ${SMS_PASSWORD}
      from: "4546"
      token-ttl-seconds: 2505600  # 29 days
      connect-timeout: 5000
      read-timeout: 30000
      max-retries: 3
      callback-url: ${SMS_CALLBACK_URL}  # Optional

  otp:
    expiry-minutes: 5
    max-attempts: 3
    rate-limit-per-hour: 5
    code-length: 6
```

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `SMS_EMAIL` | Yes | Eskiz account email |
| `SMS_PASSWORD` | Yes | Eskiz account password |
| `SMS_BASE_URL` | No | Eskiz API URL (default: https://notify.eskiz.uz/api) |
| `SMS_CALLBACK_URL` | No | Delivery status callback URL |

### RabbitMQ Configuration

| Setting | Value |
|---------|-------|
| Queue | `notification.sms.queue` |
| Exchange | `notification.exchange` |
| Routing Key | `notification.sms` |
| Dead Letter Queue | `dlq.queue` |

---

## 6. Message Flow

### Successful SMS Delivery

```
1. Service calls SmsNotificationService.sendXxx()
2. Message created with SmsType and priority
3. Message published to RabbitMQ queue
4. SmsMessageConsumer picks up message
5. EskizSmsClient sends to Eskiz API
6. Eskiz returns success status
7. Message marked as delivered
```

### Failed SMS with Retry

```
1. EskizSmsClient returns error
2. SmsMessageConsumer checks retry count
3. If retries < 3:
   - Calculate backoff: 1s, 2s, 4s
   - Requeue message with incremented retry count
4. If retries >= 3:
   - Send to Dead Letter Queue
   - Log error for monitoring
```

### Retry Timing

| Attempt | Delay |
|---------|-------|
| 1st retry | 1 second |
| 2nd retry | 2 seconds |
| 3rd retry | 4 seconds |
| After 3rd | Send to DLQ |

---

## Phone Number Handling

### Accepted Formats

| Input | Normalized |
|-------|------------|
| `+998901234567` | `998901234567` |
| `998901234567` | `998901234567` |
| `8901234567` | `998901234567` |
| `901234567` | `998901234567` |

### Masking for Logs

Phone numbers are masked in logs for privacy:
- `998901234567` → `998***4567`

---

## OTP Entity

### Database Schema

```sql
CREATE TABLE otp_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    attempts INT DEFAULT 0,
    max_attempts INT DEFAULT 3,
    is_used BOOLEAN DEFAULT FALSE,
    purpose VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_otp_phone (phone),
    INDEX idx_otp_phone_code (phone, code),
    INDEX idx_otp_expires_at (expires_at)
);
```

### OtpPurpose Enum

| Value | Description |
|-------|-------------|
| `LOGIN` | User login |
| `SIGNUP` | New user registration |
| `PASSWORD_RESET` | Password reset flow |
| `PHONE_VERIFICATION` | Phone number verification |

### Scheduled Cleanup

- **Schedule:** Daily at 3:00 AM
- **Action:** Delete OTPs older than 7 days

---

## Error Handling

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `OTP_EXPIRED` | OTP exceeded 5-minute validity | Request new OTP |
| `OTP_INVALID` | Wrong code entered | Check code, max 3 attempts |
| `OTP_MAX_ATTEMPTS` | Too many failed attempts | Request new OTP |
| `RATE_LIMIT_EXCEEDED` | >5 OTPs per hour | Wait before retrying |
| `SMS_DELIVERY_FAILED` | Eskiz API error | Automatic retry (3 times) |

### Error Response Example

```json
{
  "success": false,
  "message": "Invalid OTP code",
  "errors": ["OTP code is incorrect. 2 attempts remaining."]
}
```

---

## Eskiz API Integration

### Endpoints Used

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/auth/login` | POST | Authenticate and get token |
| `/auth/refresh` | PATCH | Refresh authentication token |
| `/message/sms/send` | POST | Send single SMS |
| `/message/sms/status/{id}` | GET | Get delivery status |

### Token Management

- Token TTL: 29 days
- Auto-refresh: Every 25 days (scheduled)
- On 401 error: Automatic re-authentication and retry

---

## Monitoring

### Key Metrics

- SMS sent count (by type)
- SMS delivery success rate
- Average delivery time
- Retry count distribution
- DLQ message count

### Log Events

| Event | Level | Description |
|-------|-------|-------------|
| SMS queued | INFO | Message added to queue |
| SMS sent | INFO | Successfully delivered |
| SMS retry | WARN | Retrying failed delivery |
| SMS failed | ERROR | Moved to DLQ after retries |
| Token refreshed | INFO | Eskiz token renewed |
