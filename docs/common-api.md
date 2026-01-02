# Common Module Documentation

Shared infrastructure, utilities, and API standards for the Food Delivery Platform.

## Table of Contents

1. [Image API](#image-api)
2. [API Response Format](#api-response-format)
3. [Error Handling](#error-handling)
4. [Rate Limiting](#rate-limiting)
5. [Pagination](#pagination)

---

## Image API

Base URL: `/api/v1/images`

Image upload and retrieval service for the platform.

### 1. Upload Image

**POST** `/upload/{category}`

Upload an image to the specified category.

**Path Parameters:**
| Parameter | Description |
|-----------|-------------|
| category | Image category: `restaurants`, `menu-items`, `profiles`, `documents` |

**Request:**
```
Content-Type: multipart/form-data

file: <binary image data>
```

**Response (200):**
```json
{
  "success": true,
  "message": "Image uploaded successfully",
  "data": {
    "filename": "a1b2c3d4-restaurant-logo.jpg",
    "originalFilename": "logo.jpg",
    "url": "/api/v1/images/restaurants/a1b2c3d4-restaurant-logo.jpg",
    "size": 102400,
    "contentType": "image/jpeg"
  }
}
```

**Roles:** ADMIN, PLATFORM, RESTAURANT_OWNER, RESTAURANT_STAFF

---

### 2. Upload Menu Item Image

**POST** `/menu-items/{menuItemId}`

Upload an image for a specific menu item.

**Path Parameters:**
| Parameter | Description |
|-----------|-------------|
| menuItemId | Menu item ID |

**Request:** Same as Upload Image

**Roles:** ADMIN, PLATFORM, RESTAURANT_OWNER, RESTAURANT_STAFF

---

### 3. Get Image

**GET** `/{category}/{filename}`

Retrieve an image by category and filename.

**Response:** Binary image data with appropriate content-type header.

**Cache Headers:**
```
Cache-Control: max-age=31536000, public
```

**No authentication required** - Images are publicly accessible.

---

### 4. Delete Image

**DELETE** `/{category}/{filename}`

Delete an image by category and filename.

**Response (200):**
```json
{
  "success": true,
  "message": "Image deleted successfully"
}
```

**Roles:** ADMIN, PLATFORM, RESTAURANT_OWNER, RESTAURANT_STAFF

---

### Image Categories

| Category | Usage | Example |
|----------|-------|---------|
| `restaurants` | Restaurant logos, banners | `/images/restaurants/logo.jpg` |
| `menu-items` | Food item images | `/images/menu-items/pizza.jpg` |
| `profiles` | User profile pictures | `/images/profiles/user123.jpg` |
| `documents` | Verification documents | `/images/documents/license.pdf` |

### Supported Formats

| Format | Content-Type | Extension |
|--------|--------------|-----------|
| JPEG | image/jpeg | .jpg, .jpeg |
| PNG | image/png | .png |
| GIF | image/gif | .gif |
| WebP | image/webp | .webp |

### Limits

- **Max file size:** 5 MB
- **Max dimensions:** 4096 x 4096 pixels

---

## API Response Format

All API endpoints use a consistent response structure.

### Success Response

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2024-01-15T10:30:00"
}
```

### Error Response

```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

### Validation Error Response

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "email": "must be a valid email address",
    "password": "must be at least 8 characters"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## Error Handling

### HTTP Status Codes

| Code | Description | When Used |
|------|-------------|-----------|
| 200 | OK | Successful request |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Validation errors, missing parameters |
| 401 | Unauthorized | Invalid or missing authentication |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Duplicate resource |
| 422 | Unprocessable Entity | Invalid operation |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Unexpected server error |
| 502 | Payment Required | Payment processing failed |

### Exception Types

| Exception | HTTP Status | Description |
|-----------|-------------|-------------|
| ResourceNotFoundException | 404 | Requested resource not found |
| DuplicateResourceException | 409 | Resource already exists |
| BusinessException | 400 | Business rule violation |
| InvalidOperationException | 422 | Operation not allowed |
| PaymentException | 402 | Payment processing error |
| RateLimitExceededException | 429 | Rate limit exceeded |
| BadCredentialsException | 401 | Invalid credentials |
| AccessDeniedException | 403 | Access denied |

---

## Rate Limiting

Rate limiting is implemented using Redis sliding window counters.

### Configuration

Rate limits are configured per endpoint using the `@RateLimited` annotation:

```java
@RateLimited(requestsPerMinute = 10, keyType = RateLimited.KeyType.IP)
```

### Key Types

| Key Type | Description |
|----------|-------------|
| `IP` | Rate limit per IP address |
| `USER` | Rate limit per authenticated user |
| `GLOBAL` | Rate limit shared across all requests |

### Default Limits

| Endpoint Category | Limit | Window |
|-------------------|-------|--------|
| Authentication | 10-20/min | Per IP |
| Password Reset | 5/min | Per IP |
| OTP Send | 5/hour | Per phone |
| API General | 100/min | Per user |
| File Upload | 10/min | Per user |

### Rate Limit Response

When rate limit is exceeded:

```json
{
  "success": false,
  "message": "Rate limit exceeded. Please try again later.",
  "timestamp": "2024-01-15T10:30:00"
}
```

HTTP Status: `429 Too Many Requests`

Headers:
```
Retry-After: 60
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1704113460
```

---

## Pagination

Paginated endpoints use Spring Data's `Pageable` interface.

### Request Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-indexed) |
| size | int | 20 | Items per page |
| sort | string | - | Sort field and direction (e.g., `createdAt,desc`) |

### Example Request

```
GET /api/v1/users?page=0&size=20&sort=createdAt,desc
```

### Paginated Response

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### PagedResponse Structure

| Field | Type | Description |
|-------|------|-------------|
| content | array | Page data |
| page | int | Current page number |
| size | int | Page size |
| totalElements | long | Total items across all pages |
| totalPages | int | Total number of pages |
| first | boolean | Is this the first page? |
| last | boolean | Is this the last page? |
| hasNext | boolean | Is there a next page? |
| hasPrevious | boolean | Is there a previous page? |

---

## Common Utilities

### Slug Generation

URLs use slugified names for SEO-friendly endpoints:

```
Restaurant: "Pizza Palace" → "pizza-palace"
Menu Item: "Margherita Pizza" → "margherita-pizza"
```

### Date/Time Format

All timestamps use ISO 8601 format:

```
2024-01-15T10:30:00
2024-01-15T10:30:00.123
2024-01-15T10:30:00+05:00
```

### Currency

All monetary values are in the platform's base currency (UZS - Uzbek Som):

```json
{
  "amount": 50000,
  "currency": "UZS"
}
```

---

## CORS Configuration

Cross-Origin Resource Sharing is configured for the platform:

| Origin | Allowed |
|--------|---------|
| `http://localhost:*` | Development |
| `https://*.fooddelivery.uz` | Production |

Allowed methods: GET, POST, PUT, PATCH, DELETE, OPTIONS

Allowed headers: Authorization, Content-Type, X-Requested-With

---

## WebSocket Endpoints

Real-time communication uses STOMP over WebSocket:

| Endpoint | Description |
|----------|-------------|
| `/ws` | WebSocket connection endpoint |
| `/topic/orders/{orderId}` | Order status updates |
| `/topic/couriers/{courierId}/location` | Courier location updates |
| `/queue/notifications` | User notifications |

### Connection

```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({
  'Authorization': 'Bearer ' + accessToken
}, function(frame) {
  // Connected
});
```
