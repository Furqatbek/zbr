# Authentication & User Management API Documentation

Authentication, authorization, and user management endpoints for the Food Delivery Platform.

## Table of Contents

1. [Authentication](#authentication)
2. [Phone Authentication (OTP)](#phone-authentication-otp)
3. [User Management](#user-management)
4. [Consumer Profile](#consumer-profile)
5. [Security Features](#security-features)

---

## Authentication

Base URL: `/api/v1/auth`

### 1. Register

**POST** `/register`

Register a new user account.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe",
  "phone": "+1234567890",
  "role": "CONSUMER"
}
```

**Available Roles:**
- `CONSUMER` - Customer ordering food
- `RESTAURANT_OWNER` - Restaurant manager
- `COURIER` - Delivery driver

**Response (201):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "fullName": "John Doe",
      "roles": ["CONSUMER"]
    }
  }
}
```

**Rate Limit:** 10 requests/minute per IP

---

### 2. Login

**POST** `/login`

Authenticate with email/phone and password.

**Request Body:**
```json
{
  "emailOrPhone": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "fullName": "John Doe",
      "roles": ["CONSUMER"]
    }
  }
}
```

**Rate Limit:** 20 requests/minute per IP

---

### 3. Refresh Token

**POST** `/refresh`

Get a new access token using refresh token.

**Request Body:**
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g..."
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5...",
    "refreshToken": "bmV3IHJlZnJlc2ggdG9rZW4...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

---

### 4. Logout

**POST** `/logout`

Revoke refresh token.

**Request Body:**
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g..."
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Logout successful"
}
```

---

### 5. Password Reset

**POST** `/password-reset`

Request password reset email.

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "If an account exists with this email, a password reset link will be sent"
}
```

**Rate Limit:** 5 requests/minute per IP

---

### 6. Confirm Password Reset

**POST** `/password-reset/confirm`

Reset password using token from email.

**Request Body:**
```json
{
  "token": "reset-token-from-email",
  "newPassword": "NewSecurePass123!"
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Password reset successful"
}
```

**Token Expiry:** 1 hour

---

## Phone Authentication (OTP)

Base URL: `/api/v1/auth/phone`

Phone-based authentication for consumers using SMS OTP.

### 1. Send OTP

**POST** `/send-otp`

Send verification code to phone number.

**Request Body:**
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
    "expiresInSeconds": 300,
    "isNewUser": false
  }
}
```

**Rate Limit:** 5 OTPs per hour per phone number

---

### 2. Verify OTP

**POST** `/verify`

Verify OTP and authenticate. Creates account if new user.

**Request Body:**
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
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "isNewUser": true
  }
}
```

**Max Attempts:** 3 per OTP

---

### 3. Resend OTP

**POST** `/resend-otp`

Resend verification code. Invalidates previous OTP.

**Request Body:**
```json
{
  "phone": "998901234567"
}
```

---

## User Management

Base URL: `/api/v1/users`

### Current User Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/me` | Get current user profile |
| PUT | `/me` | Update current user profile |
| POST | `/me/change-password` | Change password |
| POST | `/me/logout-all` | Logout from all devices |

### Admin Endpoints (ADMIN/PLATFORM roles)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/{id}` | Get user by ID |
| GET | `/` | List all users (paginated) |
| GET | `/search?q=` | Search users by name/email |
| GET | `/role/{role}` | Get users by role |
| GET | `/status/{status}` | Get users by status |
| PATCH | `/{id}/status?status=` | Update user status |
| POST | `/{id}/roles/{role}` | Add role (ADMIN only) |
| DELETE | `/{id}/roles/{role}` | Remove role (ADMIN only) |
| DELETE | `/{id}` | Soft delete user (ADMIN only) |

### User Roles

| Role | Description |
|------|-------------|
| `CONSUMER` | Customer ordering food |
| `COURIER` | Delivery driver |
| `RESTAURANT_OWNER` | Restaurant manager |
| `ADMIN` | Platform administrator |
| `PLATFORM` | System-level access |

### User Statuses

| Status | Description |
|--------|-------------|
| `ACTIVE` | Normal active account |
| `INACTIVE` | Account deactivated by user |
| `SUSPENDED` | Account suspended by admin |
| `DELETED` | Soft-deleted account |

---

## Consumer Profile

Base URL: `/api/v1/consumers`

### Endpoints

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| GET | `/profile` | Get consumer profile | CONSUMER |
| PUT | `/profile` | Update consumer profile | CONSUMER |
| GET | `/{id}` | Get consumer by ID | ADMIN, RESTAURANT_OWNER |

### Update Profile Request

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "address": "123 Main Street, Tashkent",
  "latitude": 41.2995,
  "longitude": 69.2401
}
```

---

## Security Features

### Password Requirements

- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character (!@#$%^&*)

### Token Configuration

| Token Type | Expiry | Storage |
|------------|--------|---------|
| Access Token | 1 hour | Memory/Local Storage |
| Refresh Token | 7 days | Secure HTTP-only cookie |
| Password Reset | 1 hour | Database |
| OTP Code | 5 minutes | Redis |

### Rate Limiting

| Endpoint | Limit | Key |
|----------|-------|-----|
| `/register` | 10/min | IP |
| `/login` | 20/min | IP |
| `/password-reset` | 5/min | IP |
| `/password-reset/confirm` | 5/min | IP |
| `/phone/send-otp` | 5/hour | Phone |

### Account Lockout

- **Failed Logins:** Account locked after 5 failed attempts
- **Lockout Duration:** 15 minutes
- **Unlock:** Automatic after duration or via password reset

### JWT Token Structure

```json
{
  "sub": "1",
  "email": "user@example.com",
  "roles": ["CONSUMER"],
  "iat": 1704067200,
  "exp": 1704070800
}
```

### Authorization Header

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5...
```

---

## Error Responses

### 401 Unauthorized

```json
{
  "success": false,
  "message": "Invalid credentials",
  "error": {
    "code": "AUTH_001",
    "details": "Email or password is incorrect"
  }
}
```

### 423 Account Locked

```json
{
  "success": false,
  "message": "Account locked",
  "error": {
    "code": "AUTH_002",
    "details": "Too many failed login attempts. Try again in 15 minutes."
  }
}
```

### 409 Conflict

```json
{
  "success": false,
  "message": "User already exists",
  "error": {
    "code": "AUTH_003",
    "details": "An account with this email already exists"
  }
}
```
