# Food Delivery Platform Backend

A production-ready food delivery aggregator backend service built with Java 17 and Spring Boot. This platform enables restaurants, couriers, and consumers to interact through a unified API for food ordering and delivery.

## Features

- **Multi-tenant Support**: Restaurant owners, couriers, consumers, and platform admins
- **JWT Authentication**: Secure access/refresh token-based authentication with role-based authorization
- **Phone-based OTP Authentication**: SMS verification for consumer login/signup with rate limiting
- **SMS Integration**: Eskiz.uz SMS broker integration via RabbitMQ message queue
- **Order Management**: Complete order lifecycle with state machine for status transitions
- **Restaurant Management**: Onboarding, menu management, and operating hours
- **Courier System**: Real-time location tracking and order assignment
- **Payment Integration**: Payment intent creation, confirmation, and refunds (Stripe-ready)
- **Kitchen Display System**: Real-time WebSocket notifications for kitchen orders
- **Referral Program**: User referral tracking with rewards
- **Business Analytics**: DAU/WAU/MAU, conversion funnel, AOV, churn metrics with Redis caching
- **Operations Analytics**: Order fulfillment time, restaurant/courier performance, ETA accuracy
- **Financial Analytics**: GMV, commission revenue, delivery fees, promotions, payouts, contribution margin
- **Technical Analytics**: API performance, backend resources, database health, WebSocket metrics, storage monitoring
- **Observability**: Prometheus metrics, alerting rules, and Grafana dashboards
- **API Documentation**: OpenAPI/Swagger documentation

## Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Database**: PostgreSQL 15 with Flyway migrations
- **Cache**: Redis 7
- **Message Broker**: RabbitMQ 3.12
- **Containerization**: Docker & Docker Compose
- **Build Tool**: Maven
- **Documentation**: OpenAPI 3.0 (Springdoc)
- **Testing**: JUnit 5, Mockito, Testcontainers

## Architecture

The application follows a modular monolith architecture with package-by-feature organization:

```
src/main/java/com/fooddelivery/
├── auth/           # Authentication & authorization (JWT + OTP)
├── restaurant/     # Restaurant management
├── order/          # Order processing
├── courier/        # Courier management
├── kitchen/        # Kitchen display system
├── notification/   # Notifications
├── sms/            # SMS integration (Eskiz.uz)
├── analytics/      # Business analytics and metrics
├── platform/       # Platform features (referrals)
├── webhook/        # External integrations
└── common/         # Shared utilities, config, exceptions
```

### SMS Message Flow

```
[Application] → [RabbitMQ Queue] → [SMS Consumer] → [Eskiz.uz API] → [User Phone]
```

The SMS integration uses RabbitMQ for async message delivery with retry and dead-letter queue support.

## Getting Started

> **Deploying?** → **[Deployment guide](docs/DEPLOYMENT.md)** (secrets, bring-up,
> verification, production steps).
> **Running it day to day?** → **[Operations runbook](docs/OPERATIONS.md)**
> (backups, alerting, incident response).
> The quick start below is the bare minimum for a local spin-up.

### Prerequisites

- Docker and Docker Compose
- Java 17+ (for local development)
- Maven 3.9+ (for local development)

### Quick Start with Docker

1. Clone the repository:
```bash
git clone https://github.com/your-org/food-delivery-backend.git
cd food-delivery-backend
```

2. Start all services:
```bash
docker-compose up -d
```

3. The application will be available at:
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - RabbitMQ Management: http://localhost:15672 (guest/guest)
   - Prometheus: http://localhost:9090
   - Grafana: http://localhost:3000 (admin/admin)

### Local Development

1. Start infrastructure services:
```bash
docker-compose up -d postgres redis rabbitmq
```

2. Run the application:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

3. Run tests:
```bash
./mvnw test
```

## API Documentation

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user with email/password |
| POST | `/api/v1/auth/login` | Login with email/phone and password |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Logout (revoke tokens) |
| POST | `/api/v1/auth/password-reset` | Request password reset |
| POST | `/api/v1/auth/password-reset/confirm` | Confirm password reset |

### Phone Authentication Endpoints (OTP)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/phone/send-otp` | Send OTP to phone number |
| POST | `/api/v1/auth/phone/verify` | Verify OTP and authenticate |
| POST | `/api/v1/auth/phone/resend-otp` | Resend OTP code |

### Consumer Profile Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/consumers/profile` | Get current consumer profile |
| PUT | `/api/v1/consumers/profile` | Update consumer profile |
| GET | `/api/v1/consumers/{id}` | Get consumer by ID (Admin/Restaurant) |

### Restaurant Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/restaurants` | List all restaurants |
| GET | `/api/v1/restaurants/{id}` | Get restaurant by ID |
| GET | `/api/v1/restaurants/slug/{slug}` | Get restaurant by slug |
| GET | `/api/v1/restaurants/search` | Search restaurants |
| GET | `/api/v1/restaurants/nearby` | Find nearby restaurants |
| POST | `/api/v1/restaurants` | Create restaurant (Owner) |
| PUT | `/api/v1/restaurants/{id}` | Update restaurant |
| GET | `/api/v1/restaurants/{id}/menu` | Get restaurant menu |
| POST | `/api/v1/restaurants/{id}/menu/categories` | Create category |
| POST | `/api/v1/restaurants/{id}/menu/items` | Create menu item |

### Order Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/orders` | Create new order |
| GET | `/api/v1/orders/{id}` | Get order by ID |
| GET | `/api/v1/orders/my` | Get my orders |
| PUT | `/api/v1/orders/{id}/status` | Update order status |
| PUT | `/api/v1/orders/{id}/cancel` | Cancel order |

### Courier Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/couriers/register` | Register as courier |
| GET | `/api/v1/couriers/me` | Get courier profile |
| PUT | `/api/v1/couriers/status` | Update online status |
| PUT | `/api/v1/couriers/location` | Update location |
| GET | `/api/v1/couriers/orders/available` | Get available orders |
| POST | `/api/v1/couriers/orders/{id}/accept` | Accept order |

### Analytics Endpoints (Admin/Platform only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/analytics/activity` | User activity metrics (DAU/WAU/MAU) |
| GET | `/api/v1/analytics/orders` | Order volume metrics |
| GET | `/api/v1/analytics/conversion` | Conversion funnel metrics |
| GET | `/api/v1/analytics/aov` | Average Order Value metrics |
| GET | `/api/v1/analytics/activation` | User activation metrics |
| GET | `/api/v1/analytics/churn` | Churn metrics (users, restaurants, couriers) |
| GET | `/api/v1/analytics/summary` | Summary of all key metrics |
| POST | `/api/v1/analytics/cache/refresh` | Refresh all analytics caches |
| POST | `/api/v1/analytics/cache/refresh/{name}` | Refresh specific cache |

### Operations Analytics Endpoints (Admin/Platform only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/analytics/operations/order/{orderId}` | Order fulfillment metrics |
| GET | `/api/v1/analytics/operations/restaurant/{id}` | Restaurant performance metrics |
| GET | `/api/v1/analytics/operations/courier/{id}` | Courier performance metrics |
| GET | `/api/v1/analytics/operations/delivery-success` | Delivery success rate |
| GET | `/api/v1/analytics/operations/eta-accuracy` | ETA accuracy metrics |
| GET | `/api/v1/analytics/operations/summary` | Operations summary |
| POST | `/api/v1/analytics/operations/cache/refresh` | Refresh operations caches |

### Financial Analytics Endpoints (Admin/Platform only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/analytics/financial/gmv` | GMV (Gross Merchandise Value) metrics |
| POST | `/api/v1/analytics/financial/commission` | Commission revenue metrics |
| POST | `/api/v1/analytics/financial/delivery-fees` | Delivery fee metrics |
| POST | `/api/v1/analytics/financial/promotions` | Promotion/discount metrics |
| POST | `/api/v1/analytics/financial/restaurant-payouts` | Restaurant payout metrics |
| POST | `/api/v1/analytics/financial/courier-payouts` | Courier payout metrics |
| POST | `/api/v1/analytics/financial/contribution-margin` | Contribution margin & unit economics |
| GET | `/api/v1/analytics/financial/summary` | Financial summary |
| GET | `/api/v1/analytics/financial/restaurants/{id}/payouts` | Restaurant payout details |
| GET | `/api/v1/analytics/financial/couriers/{id}/payouts` | Courier payout details |

### Technical Analytics Endpoints (Admin/Platform only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/analytics/technical/summary` | Technical health summary with scores |
| POST | `/api/v1/analytics/technical/api-performance` | API performance metrics (latency, error rates) |
| GET | `/api/v1/analytics/technical/api-performance/realtime` | Real-time API metrics from Micrometer |
| GET | `/api/v1/analytics/technical/backend-resources` | Backend resource usage (CPU, memory, JVM) |
| GET | `/api/v1/analytics/technical/backend-resources/historical` | Historical backend metrics |
| POST | `/api/v1/analytics/technical/database-performance` | Database performance (slow queries, index usage) |
| POST | `/api/v1/analytics/technical/websocket` | WebSocket connection & message metrics |
| GET | `/api/v1/analytics/technical/websocket/realtime` | Real-time WebSocket connection status |
| POST | `/api/v1/analytics/technical/storage` | Storage and upload metrics |
| GET | `/api/v1/analytics/technical/storage/summary` | Current storage summary |
| GET | `/api/v1/analytics/technical/health-scores` | Health scores for all subsystems (0-100) |
| POST | `/api/v1/analytics/technical/refresh` | Refresh all technical metrics caches |

## Order State Machine

Orders follow a strict state machine for status transitions:

```
CREATED → ACCEPTED → PREPARING → READY → PICKED_UP → IN_TRANSIT → DELIVERED → COMPLETED
    ↓         ↓           ↓
CANCELLED CANCELLED   CANCELLED
```

## User Roles

| Role | Description |
|------|-------------|
| ADMIN | Platform administrator |
| PLATFORM | Platform operations team |
| RESTAURANT_OWNER | Restaurant owner/manager |
| RESTAURANT_STAFF | Restaurant employee |
| COURIER | Delivery driver |
| CONSUMER | End customer |

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` |
| `SPRING_DATASOURCE_URL` | PostgreSQL URL | `jdbc:postgresql://localhost:5432/fooddelivery` |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |
| `SPRING_RABBITMQ_HOST` | RabbitMQ host | `localhost` |
| `JWT_SECRET` | JWT signing key | - |
| `JWT_EXPIRATION` | Access token TTL (ms) | `3600000` |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL (ms) | `604800000` |
| `SMS_ENABLED` | Enable SMS sending | `true` |
| `SMS_EMAIL` | Eskiz.uz account email | - |
| `SMS_PASSWORD` | Eskiz.uz account password | - |
| `SMS_FROM` | SMS sender ID | `4546` |
| `OTP_EXPIRY_MINUTES` | OTP expiration time | `5` |
| `OTP_MAX_ATTEMPTS` | Max OTP verification attempts | `3` |
| `OTP_RATE_LIMIT` | Max OTPs per hour per phone | `5` |

## Testing

### Run Unit Tests
```bash
./mvnw test
```

### Run Integration Tests
```bash
./mvnw verify -P integration-tests
```

### Test Coverage
```bash
./mvnw jacoco:report
# Report available at target/site/jacoco/index.html
```

## Postman Collection

Import the Postman collection from `postman/Food_Delivery_API.postman_collection.json` for easy API testing.

## Database Migrations

Flyway handles database migrations automatically. Migration files are located in:
```
src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__seed_data.sql
├── V3__add_menu_item_image.sql
├── V4__add_otp_and_consumer_fields.sql
├── V5__add_activity_logs_table.sql
├── V6__add_operations_analytics_tables.sql
└── V7__add_financial_analytics_tables.sql
```

### OTP Table Schema

```sql
CREATE TABLE otp_codes (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    attempts INTEGER DEFAULT 0,
    max_attempts INTEGER DEFAULT 3,
    is_used BOOLEAN DEFAULT FALSE,
    purpose VARCHAR(20) DEFAULT 'LOGIN',  -- LOGIN, SIGNUP, PASSWORD_RESET, PHONE_VERIFICATION
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Demo Users

After starting the application with seed data:

| Email | Password | Role |
|-------|----------|------|
| admin@fooddelivery.com | Admin@123 | ADMIN |
| platform@fooddelivery.com | Platform@123 | PLATFORM |
| owner@pizzapalace.com | Owner@123 | RESTAURANT_OWNER |
| john.doe@example.com | Consumer@123 | CONSUMER |
| courier@fooddelivery.com | Courier@123 | COURIER |

### Phone-based Consumer Login

Consumers can also login/signup using phone number with OTP verification:
1. Send OTP to phone: `POST /api/v1/auth/phone/send-otp` with `{"phone": "998901234567"}`
2. Verify OTP: `POST /api/v1/auth/phone/verify` with `{"phone": "998901234567", "code": "123456"}`
3. New users are automatically created with CONSUMER role

## Monitoring

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Prometheus Metrics
```bash
curl http://localhost:8080/actuator/prometheus
```

### Available Metrics
- `http_server_requests_seconds` - HTTP request latency
- `jvm_memory_used_bytes` - JVM memory usage
- `hikaricp_connections_active` - Database connection pool
- `rabbitmq_consumed_total` - RabbitMQ message consumption
- Custom business metrics (orders, payments, etc.)

## Project Structure

```
food-delivery-backend/
├── src/
│   ├── main/
│   │   ├── java/com/fooddelivery/
│   │   │   ├── auth/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   ├── security/
│   │   │   │   └── service/
│   │   │   ├── restaurant/
│   │   │   ├── order/
│   │   │   ├── courier/
│   │   │   ├── kitchen/
│   │   │   ├── analytics/
│   │   │   ├── notification/
│   │   │   ├── platform/
│   │   │   ├── webhook/
│   │   │   └── common/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   └── test/
├── docker/
│   ├── prometheus/
│   └── grafana/
├── postman/
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
