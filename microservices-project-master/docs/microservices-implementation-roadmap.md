# Microservices Implementation Roadmap (Day-wise)

## Project Overview
- **Auth Service**: JWT RS256 + RBAC
- **Payment Service**: WebFlux Reactive APIs
- **Database**: Aurora MySQL
- **Message Queue**: Kafka
- **Caching**: Redis Rate Limiting
- **Local Dev**: Docker Compose

---

## **DAY 1: Project Setup & Infrastructure**

### 1.1 Create Project Structure
- [ ] Create root directory: `microservices-project/`
- [ ] Create subdirectories:
  - `auth-service/`
  - `payment-service/`
  - `common/` (shared utilities)
  - `docker/` (Docker configs)
  - `k8s/` (optional Kubernetes configs)

### 1.2 Initialize Git Repository
- [ ] Initialize Git repo
- [ ] Create `.gitignore` (Java/Spring Boot specific)
- [ ] Create `README.md` with project overview

### 1.3 Create Docker Compose File
- [ ] Setup MySQL (Aurora compatible)
- [ ] Setup Kafka (Zookeeper + Broker)
- [ ] Setup Redis
- [ ] Create `docker-compose.yml`

### 1.4 Create Parent POM (Maven)
- [ ] Create root `pom.xml`
- [ ] Define Spring Boot version (3.x)
- [ ] Define common dependencies
- [ ] Setup module references

### 1.5 Create Auth Service Module
- [ ] Create auth-service `pom.xml`
- [ ] Add Spring Security, Spring Web, JWT dependencies
- [ ] Create basic project structure (controller, service, repository)

### 1.6 Create Payment Service Module
- [ ] Create payment-service `pom.xml`
- [ ] Add Spring WebFlux, Reactor dependencies
- [ ] Create basic project structure

### 1.7 Test Docker Environment
- [ ] Start Docker Compose
- [ ] Verify MySQL connectivity
- [ ] Verify Kafka connectivity
- [ ] Verify Redis connectivity

---

## **DAY 2: Authentication Service - JWT & Key Generation**

### 2.1 Generate RSA Key Pair
- [ ] Create RSA key pair generator utility
- [ ] Generate public key (public_key.pem)
- [ ] Generate private key (private_key.pem)
- [ ] Store keys in `src/main/resources/keys/`

### 2.2 JWT Provider Configuration
- [ ] Create `JwtProperties` configuration class
- [ ] Load keys from properties/resources
- [ ] Setup algorithm (RS256)
- [ ] Setup token expiration times (access: 15min, refresh: 7 days)

### 2.3 JWT Token Generation Service
- [ ] Create `JwtTokenProvider` class
- [ ] Implement token generation method
- [ ] Add claims (userId, roles, permissions)
- [ ] Implement token signing with RS256

### 2.4 JWT Token Validation Service
- [ ] Create token validation method
- [ ] Verify signature with public key
- [ ] Check expiration
- [ ] Extract claims

### 2.5 Test JWT Functionality
- [ ] Unit test token generation
- [ ] Unit test token validation
- [ ] Unit test signature verification
- [ ] Test with different claims

---

## **DAY 3: Authentication Service - User & Role Management**

### 3.1 Create Database Schema
- [ ] Create `users` table
  - `id, username, email, password_hash, is_active, created_at, updated_at`
- [ ] Create `roles` table
  - `id, name, description`
- [ ] Create `permissions` table
  - `id, name, description`
- [ ] Create `user_roles` junction table
- [ ] Create `role_permissions` junction table

### 3.2 Create Entity Classes
- [ ] Create `User` JPA entity
- [ ] Create `Role` JPA entity
- [ ] Create `Permission` JPA entity
- [ ] Setup relationships (OneToMany, ManyToMany)

### 3.3 Create Repository Interfaces
- [ ] Create `UserRepository` (JpaRepository)
- [ ] Add custom method: `findByUsername()`
- [ ] Add custom method: `findByEmail()`
- [ ] Create `RoleRepository`
- [ ] Create `PermissionRepository`

### 3.4 Create User Service
- [ ] Create `UserService` class
- [ ] Implement `findByUsername()`
- [ ] Implement `getUserWithRoles()`
- [ ] Implement `getUserPermissions()`

### 3.5 Create Role Service
- [ ] Create `RoleService` class
- [ ] Implement role creation
- [ ] Implement role assignment to user
- [ ] Implement permission assignment to role

### 3.6 Initialize Default Roles & Permissions
- [ ] Create Flyway migration script
- [ ] Insert default roles: `ADMIN`, `USER`, `VENDOR`
- [ ] Insert default permissions: `READ_PAYMENT`, `CREATE_PAYMENT`, `APPROVE_PAYMENT`

---

## **DAY 4: Authentication Service - REST API & Security**

### 4.1 Create Login Request/Response DTOs
- [ ] Create `LoginRequest` DTO (username, password)
- [ ] Create `LoginResponse` DTO (accessToken, refreshToken, expiresIn)
- [ ] Create `TokenRefreshRequest` DTO
- [ ] Create `TokenRefreshResponse` DTO

### 4.2 Create Authentication Controller
- [ ] Create `AuthController` class
- [ ] Implement `POST /api/auth/login` endpoint
- [ ] Implement `POST /api/auth/refresh` endpoint
- [ ] Add request validation

### 4.3 Create Authentication Service Logic
- [ ] Create `AuthenticationService` class
- [ ] Implement login logic (find user, verify password)
- [ ] Implement token generation
- [ ] Implement refresh token logic

### 4.4 Setup Spring Security Configuration
- [ ] Create `SecurityConfig` class
- [ ] Configure password encoder (BCrypt)
- [ ] Setup CORS configuration
- [ ] Disable CSRF for APIs

### 4.5 Create JWT Authentication Filter
- [ ] Create `JwtAuthenticationFilter` class
- [ ] Extract JWT from Authorization header
- [ ] Validate token
- [ ] Load user details into SecurityContext

### 4.6 Create Access Control (RBAC)
- [ ] Create `@RoleRequired` annotation
- [ ] Create aspect for role validation
- [ ] Setup method-level security

### 4.7 Add Exception Handling
- [ ] Create custom exceptions: `UnauthorizedException`, `ForbiddenException`
- [ ] Create global exception handler
- [ ] Return proper error responses

### 4.8 Test Auth Endpoints
- [ ] Test login endpoint
- [ ] Test token refresh
- [ ] Test access with valid token
- [ ] Test access with invalid token

---

## **DAY 5: Payment Service - Domain & Database**

### 5.1 Create Payment Database Schema
- [ ] Create `payments` table
  - `id, order_id, user_id, amount, currency, status, payment_method, created_at, updated_at`
- [ ] Create `payment_status_history` table
  - `id, payment_id, status, reason, created_at`
- [ ] Create indexes on frequently queried columns

### 5.2 Create Payment Entities
- [ ] Create `Payment` JPA entity
- [ ] Create `PaymentStatusHistory` JPA entity
- [ ] Setup relationships
- [ ] Add validation constraints

### 5.3 Create Payment Enums
- [ ] Create `PaymentStatus` enum: `PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED`
- [ ] Create `PaymentMethod` enum: `CREDIT_CARD, DEBIT_CARD, UPI, WALLET`
- [ ] Create `Currency` enum: `INR, USD, EUR`

### 5.4 Create Payment Repository
- [ ] Create `PaymentRepository` (ReactiveCrudRepository)
- [ ] Add method: `findByOrderId()`
- [ ] Add method: `findByUserId()`
- [ ] Add method: `findByStatus()`

### 5.5 Create DTOs
- [ ] Create `CreatePaymentRequest` DTO
- [ ] Create `PaymentResponse` DTO
- [ ] Create `PaymentStatusUpdate` DTO
- [ ] Add validation annotations

---

## **DAY 6: Payment Service - WebFlux Reactive APIs**

### 6.1 Setup WebFlux Configuration
- [ ] Add Spring WebFlux dependency
- [ ] Configure embedded Netty server
- [ ] Setup reactive datasource (r2dbc-mysql)
- [ ] Create `R2dbcConfiguration` class

### 6.2 Create Payment Controller (Reactive)
- [ ] Create `PaymentController` class
- [ ] Implement `POST /api/payments/create` - returns `Mono<PaymentResponse>`
- [ ] Implement `GET /api/payments/{id}` - returns `Mono<PaymentResponse>`
- [ ] Implement `GET /api/payments/user/{userId}` - returns `Flux<PaymentResponse>`

### 6.3 Create Payment Service (Reactive)
- [ ] Create `PaymentService` class
- [ ] Implement `createPayment()` - returns `Mono<Payment>`
- [ ] Implement `getPayment()` - returns `Mono<Payment>`
- [ ] Implement `getUserPayments()` - returns `Flux<Payment>`

### 6.4 Implement Payment Processing Logic
- [ ] Create payment processing business logic
- [ ] Validate payment request
- [ ] Save payment with status `PENDING`
- [ ] Create payment status history entry

### 6.5 Add Input Validation
- [ ] Create `CreatePaymentValidator`
- [ ] Validate amount > 0
- [ ] Validate currency
- [ ] Validate payment method

### 6.6 Add Error Handling
- [ ] Create custom exceptions: `PaymentException`, `InvalidPaymentException`
- [ ] Create global exception handler for WebFlux
- [ ] Return proper Mono error responses

### 6.7 Test Reactive Endpoints
- [ ] Test payment creation (Mono)
- [ ] Test payment retrieval (Mono)
- [ ] Test user payments list (Flux)
- [ ] Test error handling with reactive flows

---

## **DAY 7: Kafka Integration - Event Publishing**

### 7.1 Setup Kafka Configuration
- [ ] Add Spring Kafka dependency
- [ ] Create `KafkaProperties` configuration class
- [ ] Configure producer settings (bootstrap servers, serializers)
- [ ] Configure consumer settings

### 7.2 Create Kafka Topics
- [ ] Create topic: `payment.created`
- [ ] Create topic: `payment.completed`
- [ ] Create topic: `payment.failed`
- [ ] Set replication factor: 1 (local dev)
- [ ] Set partitions: 3

### 7.3 Create Event DTOs
- [ ] Create `PaymentCreatedEvent` class
- [ ] Create `PaymentCompletedEvent` class
- [ ] Create `PaymentFailedEvent` class
- [ ] Add serialization annotations (Jackson)

### 7.4 Create Kafka Producer Service
- [ ] Create `PaymentEventPublisher` class
- [ ] Implement `publishPaymentCreated()` method
- [ ] Implement `publishPaymentCompleted()` method
- [ ] Implement `publishPaymentFailed()` method
- [ ] Handle producer exceptions

### 7.5 Publish Events on Payment Actions
- [ ] Publish event when payment is created
- [ ] Publish event when payment status changes
- [ ] Add event publishing to payment service
- [ ] Log published events

### 7.6 Create Kafka Consumer (Payment Service)
- [ ] Create `PaymentEventListener` class
- [ ] Implement listener for `payment.created` topic
- [ ] Implement business logic (optional processing)
- [ ] Add error handling

### 7.7 Test Kafka Integration
- [ ] Publish test events
- [ ] Verify events in Kafka topic
- [ ] Test consumer listeners
- [ ] Verify event serialization/deserialization

---

## **DAY 8: Redis Integration - Rate Limiting**

### 8.1 Setup Redis Configuration
- [ ] Add Spring Data Redis dependency
- [ ] Create `RedisProperties` configuration class
- [ ] Configure connection pool
- [ ] Configure serialization (Jackson)

### 8.2 Create Rate Limiting Utility
- [ ] Create `RateLimitKey` enum: `LOGIN_ATTEMPT, PAYMENT_CREATE, API_GENERAL`
- [ ] Create `RateLimitConfig` class (limits per key type)
- [ ] Create `RedisRateLimiter` service

### 8.3 Implement Token Bucket Algorithm
- [ ] Create token bucket rate limiter
- [ ] Define limits:
  - Login attempts: 5 per minute per IP
  - Payment creation: 10 per minute per user
  - General API: 100 per minute per user
- [ ] Implement `isAllowed()` method using Redis

### 8.4 Create Rate Limiting Annotation
- [ ] Create `@RateLimit` annotation
- [ ] Define parameters: key type, limit, window
- [ ] Create aspect to intercept annotated methods
- [ ] Return 429 (Too Many Requests) when limit exceeded

### 8.5 Apply Rate Limiting to Auth Service
- [ ] Add `@RateLimit` to login endpoint
- [ ] Limit: 5 attempts per minute per IP
- [ ] Store attempt count in Redis
- [ ] Block after limit exceeded

### 8.6 Apply Rate Limiting to Payment Service
- [ ] Add `@RateLimit` to payment creation endpoint
- [ ] Limit: 10 payments per minute per user
- [ ] Rate limit key: `user_id`

### 8.7 Test Rate Limiting
- [ ] Make requests exceeding limit
- [ ] Verify 429 response
- [ ] Verify response headers (Retry-After, etc.)
- [ ] Check Redis keys expiration

---

## **DAY 9: API Gateway (Optional but Recommended)**

### 9.1 Create API Gateway Module
- [ ] Create `api-gateway` directory
- [ ] Create `pom.xml` with Spring Cloud Gateway dependency
- [ ] Setup basic Spring Boot application

### 9.2 Configure Gateway Routes
- [ ] Route `/api/auth/**` to auth-service
- [ ] Route `/api/payments/**` to payment-service
- [ ] Configure load balancer strategy

### 9.3 Add Authentication to Gateway
- [ ] Create gateway filter for JWT validation
- [ ] Extract token from request
- [ ] Validate token with auth-service
- [ ] Pass user info downstream

### 9.4 Add Rate Limiting to Gateway
- [ ] Configure global rate limiter
- [ ] Apply per-user rate limiting
- [ ] Return proper error responses

### 9.5 Add Request/Response Logging
- [ ] Log all incoming requests
- [ ] Log all responses
- [ ] Include correlation IDs

### 9.6 Test Gateway
- [ ] Test routing to auth service
- [ ] Test routing to payment service
- [ ] Test authentication flow through gateway

---

## **DAY 10: Integration & End-to-End Testing**

### 10.1 Create Integration Tests
- [ ] Test complete auth flow (signup → login → get token)
- [ ] Test payment creation with valid token
- [ ] Test payment retrieval
- [ ] Test error scenarios

### 10.2 Setup Testcontainers
- [ ] Add testcontainers dependency
- [ ] Create MySQL container for tests
- [ ] Create Kafka container for tests
- [ ] Create Redis container for tests

### 10.3 Create End-to-End Tests
- [ ] Test: User login → Token generation
- [ ] Test: Create payment → Event published → Event consumed
- [ ] Test: Rate limiting behavior
- [ ] Test: Error handling across services

### 10.4 Load Testing
- [ ] Setup JMeter or similar tool
- [ ] Test payment endpoint under load
- [ ] Monitor Redis, Kafka, MySQL performance
- [ ] Identify bottlenecks

### 10.5 Documentation
- [ ] Create API documentation (Swagger/OpenAPI)
- [ ] Document all endpoints
- [ ] Document error codes
- [ ] Create setup guide

### 10.6 Docker Compose Verification
- [ ] Verify all services start correctly
- [ ] Verify service connectivity
- [ ] Test complete flow via API calls

---

## **DAY 11: Monitoring & Observability**

### 11.1 Add Actuator Endpoints
- [ ] Add Spring Boot Actuator
- [ ] Expose `/actuator/health` endpoint
- [ ] Expose `/actuator/metrics` endpoint

### 11.2 Setup Application Logging
- [ ] Configure Logback
- [ ] Setup log levels
- [ ] Add structured logging (MDC)
- [ ] Add correlation IDs

### 11.3 Add Metrics Collection
- [ ] Add Micrometer dependency
- [ ] Expose Prometheus metrics endpoint
- [ ] Add custom metrics for payments
- [ ] Track API response times

### 11.4 Setup Health Checks
- [ ] Create custom health indicator for database
- [ ] Create custom health indicator for Kafka
- [ ] Create custom health indicator for Redis

### 11.5 Add Distributed Tracing (Optional)
- [ ] Add Spring Cloud Sleuth
- [ ] Propagate trace IDs across services
- [ ] Log trace IDs in all services

---

## **DAY 12: Security Hardening & Deployment Prep**

### 12.1 Security Review
- [ ] Review password hashing (BCrypt)
- [ ] Verify JWT secret management
- [ ] Check CORS configuration
- [ ] Review SQL injection vulnerabilities

### 12.2 Environment Configuration
- [ ] Create `application-dev.properties`
- [ ] Create `application-prod.properties`
- [ ] Externalize sensitive configs (secrets)
- [ ] Use environment variables

### 12.3 Database Migrations
- [ ] Setup Flyway migrations
- [ ] Create versioned SQL scripts
- [ ] Test migration rollback

### 12.4 Production Dockerfile
- [ ] Create Dockerfile for auth service
- [ ] Create Dockerfile for payment service
- [ ] Use multi-stage builds
- [ ] Minimize image size

### 12.5 Kubernetes Manifests (Optional)
- [ ] Create Deployment manifests
- [ ] Create Service manifests
- [ ] Create ConfigMap for configs
- [ ] Create Secret for sensitive data

### 12.6 Final Testing
- [ ] Run all unit tests
- [ ] Run all integration tests
- [ ] Test graceful shutdown
- [ ] Test database connectivity

---

## **Implementation Checklist Summary**

### Auth Service Checklist
- [ ] RSA key generation (Day 2)
- [ ] JWT token provider (Day 2)
- [ ] User & role entities (Day 3)
- [ ] Auth controller & endpoints (Day 4)
- [ ] Spring Security configuration (Day 4)
- [ ] Rate limiting on login (Day 8)

### Payment Service Checklist
- [ ] Payment entities (Day 5)
- [ ] WebFlux reactive controller (Day 6)
- [ ] Payment service logic (Day 6)
- [ ] Kafka event publishing (Day 7)
- [ ] Rate limiting on creation (Day 8)

### Infrastructure Checklist
- [ ] Docker Compose setup (Day 1)
- [ ] MySQL database & schema (Days 2-5)
- [ ] Kafka topics & producers/consumers (Day 7)
- [ ] Redis rate limiting (Day 8)

### Quality Assurance
- [ ] Unit tests (Each day)
- [ ] Integration tests (Day 10)
- [ ] End-to-end tests (Day 10)
- [ ] Load tests (Day 10)
- [ ] Documentation (Day 10)

---

## **Technology Stack Summary**

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.x |
| Web | Spring WebFlux | Latest |
| Security | Spring Security + JWT | Latest |
| Database | Aurora MySQL (r2dbc) | 5.7+ |
| Message Queue | Kafka | 3.x |
| Caching | Redis | 7.x |
| Auth | JWT RS256 | jjwt |
| Reactive | Project Reactor | Latest |
| Container | Docker | Latest |
| Build | Maven | 3.8+ |

---

## **Key Considerations**

1. **JWT RS256**: Private key for signing, public key for verification
2. **WebFlux**: Non-blocking I/O for payment service
3. **RBAC**: Users → Roles → Permissions hierarchy
4. **Rate Limiting**: Token bucket algorithm in Redis
5. **Kafka**: Async event processing for payment pipeline
6. **Docker Compose**: Local development environment with all services

---

## **Common Commands**

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Maven build
mvn clean install

# Run tests
mvn test

# Start auth service
mvn spring-boot:run -f auth-service/pom.xml

# Start payment service
mvn spring-boot:run -f payment-service/pom.xml
```

---

## **Next Steps After Day 12**

1. Deploy to staging environment
2. Load testing in staging
3. Security audit
4. Performance optimization
5. User acceptance testing
6. Production deployment

