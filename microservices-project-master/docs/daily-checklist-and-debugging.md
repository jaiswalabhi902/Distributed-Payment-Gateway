# Daily Implementation Checklist & Debugging Guide

---

## **DAY 1: Project Setup & Infrastructure**

### ✅ Daily Checklist

- [ ] Create directory structure
  ```bash
  mkdir -p microservices-project/{auth-service,payment-service,common,docker}
  cd microservices-project
  git init
  ```

- [ ] Create docker-compose.yml with MySQL, Kafka, Zookeeper, Redis
- [ ] Create root pom.xml (parent POM)
- [ ] Create auth-service/pom.xml (Spring Web, Security, JWT deps)
- [ ] Create payment-service/pom.xml (Spring WebFlux, R2DBC deps)
- [ ] Start Docker Compose:
  ```bash
  docker-compose up -d
  ```

- [ ] Verify MySQL connectivity
  ```bash
  docker exec mysql-container mysql -u root -proot -e "SELECT 1;"
  ```

- [ ] Verify Kafka connectivity
  ```bash
  docker exec kafka-container kafka-topics --list --bootstrap-server localhost:9092
  ```

- [ ] Verify Redis connectivity
  ```bash
  docker exec redis-container redis-cli PING
  # Should return: PONG
  ```

### 🔧 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `Cannot connect to MySQL:3306` | Port conflict or container not running | `docker ps` to verify, change port in docker-compose.yml |
| `Kafka broker unreachable` | Zookeeper not ready | Wait 10-15s after docker-compose up |
| `redis-cli: command not found` | redis-tools not installed locally | Use docker exec as shown above |
| `docker-compose: command not found` | Docker Compose not installed | Install Docker Desktop or `docker compose` (v2) |

### 📝 Notes
- Keep docker-compose running throughout the 12 days
- Use `docker-compose logs -f` to monitor startup
- Create a `.env` file for sensitive configs (optional for local dev)

---

## **DAY 2: JWT RS256 & Key Generation**

### ✅ Daily Checklist

- [ ] Generate RSA keys
  ```bash
  cd auth-service
  mkdir -p src/main/resources/keys
  openssl genrsa -out src/main/resources/keys/private_key.pem 2048
  openssl rsa -in src/main/resources/keys/private_key.pem \
              -pubout -out src/main/resources/keys/public_key.pem
  ```

- [ ] Create JwtProperties configuration class
- [ ] Create JwtTokenProvider with token generation
- [ ] Create token validation method
- [ ] Add JWT dependency to pom.xml:
  ```xml
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
  </dependency>
  ```

- [ ] Write unit tests for JWT generation & validation
  ```java
  @Test
  public void testGenerateToken() {
      String token = jwtTokenProvider.generateAccessToken("user123", Set.of("USER"));
      assertNotNull(token);
      assertTrue(jwtTokenProvider.validateToken(token));
  }
  ```

- [ ] Run tests: `mvn test -f auth-service/pom.xml`

### 🔧 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `IOException reading private key` | File not found or wrong path | Check path is relative to classpath (src/main/resources/keys/) |
| `Invalid key format` | Openssl generated wrong format | Use `-pkcs8` flag: `openssl pkcs8 -topk8 -in private_key.pem -out private_key_pkcs8.pem -nocrypt` |
| `JwtException: JWT signature does not match` | Using wrong key for validation | Ensure public key matches the private key used for signing |
| `Token expired immediately` | Expiration time is 0 | Check JwtProperties.accessTokenExpirationMs > 0 |

### 📝 Notes
- Keys are sensitive — never commit private_key.pem to Git
- Add to .gitignore: `src/main/resources/keys/private_key.pem`
- Token format: `header.payload.signature` (3 Base64-encoded parts)
- RS256 is asymmetric: private key signs, public key verifies

---

## **DAY 3: Database Schema & Entities**

### ✅ Daily Checklist

- [ ] Create Flyway migration directory
  ```bash
  mkdir -p auth-service/src/main/resources/db/migration
  ```

- [ ] Create V1__Initial_Schema.sql (users, roles, permissions, junctions)
- [ ] Create V2__Insert_Default_Data.sql (ADMIN, USER, VENDOR roles)
- [ ] Create User JPA entity with relationships
- [ ] Create Role JPA entity
- [ ] Create Permission JPA entity
- [ ] Create repositories: UserRepository, RoleRepository, PermissionRepository
- [ ] Create services: UserService, RoleService
- [ ] Add Flyway dependency to pom.xml:
  ```xml
  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
  </dependency>
  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
  </dependency>
  ```

- [ ] Add application.yml
  ```yaml
  spring:
    datasource:
      url: jdbc:mysql://localhost:3306/microservices_db
      username: root
      password: root
    jpa:
      hibernate.ddl-auto: validate
      show-sql: false
    flyway:
      enabled: true
  ```

- [ ] Run Spring Boot app — migrations should auto-run
  ```bash
  mvn spring-boot:run -f auth-service/pom.xml
  ```

- [ ] Verify schema in MySQL
  ```bash
  docker exec mysql-container mysql -u root -proot microservices_db -e "SHOW TABLES;"
  ```

### 🔧 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `Flyway migration failed: syntax error` | SQL has typo (missing semicolon, wrong column type) | Check V1__*.sql carefully; compare with schema in guide |
| `Column 'id' doesn't exist` | Entity and table column names don't match | Use `@Column("id")` on entity field |
| `Could not write JSON: Lazy initialization exception` | ManyToMany fetch type is LAZY | Use `@JsonIgnore` or `@JsonManagedReference` / `@JsonBackReference` |
| `No identifier specified` | @Id missing on entity | Add `@Id @GeneratedValue` to id field |

### 📝 Notes
- Flyway versions are immutable — never edit V1__Initial_Schema.sql after running
- If you need to change schema, create a new migration V3__Changes.sql
- Always set `spring.jpa.hibernate.ddl-auto: validate` in production
- @ManyToMany requires a junction table — Flyway handles this if JPA is configured

---

## **DAY 4: Auth REST API & Security**

### ✅ Daily Checklist

- [ ] Create LoginRequest & LoginResponse DTOs
- [ ] Create AuthenticationService with login logic
- [ ] Create AuthController with /login and /refresh endpoints
- [ ] Create SecurityConfig (Spring Security configuration)
- [ ] Create JwtAuthenticationFilter
- [ ] Create exception handler (UnauthorizedException, ForbiddenException)
- [ ] Test login endpoint:
  ```bash
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "username": "admin",
      "password": "password"
    }'
  # Expected: access token + refresh token
  ```

- [ ] Test access with token:
  ```bash
  curl -X GET http://localhost:8080/api/auth/profile \
    -H "Authorization: Bearer <TOKEN>"
  ```

- [ ] Test with invalid token (should get 401)
- [ ] Test CORS (should allow cross-origin requests from browser)
- [ ] Write integration tests

### 🔧 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `Unsupported token type` | Authorization header format wrong | Use `Authorization: Bearer <token>`, not `Bearer:<token>` |
| `No authentication principal found` | Token not extracted from request | Check JwtAuthenticationFilter extracts from `Authorization` header |
| `CORS error in browser` | CORS not configured | Add `@CrossOrigin(origins = "*", maxAge = 3600)` to controller |
| `No user found with username` | User not in database | Create user in V2__Insert_Default_Data.sql or via endpoint |
| `Password mismatch` | Password encoder not matching | Use BCryptPasswordEncoder consistently |

### 📝 Notes
- Store passwords as hashed (BCrypt), never plaintext
- Token should have userId and roles as claims
- Access token lifetime: 15 min, Refresh token: 7 days
- Filter order matters: Security filter chain must run before Controllers

---

## **DAY 5: Payment Domain & Database**

### ✅ Daily Checklist

- [ ] Create Payment database migration (V3__Payments_Schema.sql in payment-service)
  ```sql
  CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(100) UNIQUE,
    user_id BIGINT,
    amount DECIMAL(18,2),
    currency VARCHAR(3),
    status VARCHAR(20),
    payment_method VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );
  ```

- [ ] Create Payment JPA entity
- [ ] Create PaymentStatus enum (PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED)
- [ ] Create PaymentMethod enum
- [ ] Create PaymentRepository (R2dbcRepository for reactive)
- [ ] Create PaymentResponse & CreatePaymentRequest DTOs
- [ ] Add R2DBC dependency:
  ```xml
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
  </dependency>
  <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
  </dependency>
  <dependency>
    <groupId>io.asyncer</groupId>
    <artifactId>r2dbc-mysql</artifactId>
  </dependency>
  ```

- [ ] Create application.yml for payment-service
- [ ] Run migrations: `mvn spring-boot:run -f payment-service/pom.xml`
- [ ] Verify schema:
  ```bash
  docker exec mysql-container mysql -u root -proot microservices_db \
    -e "DESC payments;"
  ```

### 🔧 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `table payments doesn't exist` | Migration not run | Check flyway is enabled in application.yml |
| `r2dbc connection failed` | R2DBC driver misconfigured | Use `r2dbc:mysql://host:port/db` format, not JDBC |
| `Cannot find reactive repository` | Missing @EnableR2dbcRepositories | Add to main application class |
| `Column type mismatch` | Decimal precision wrong | Use `DECIMAL(18,2)` for money |

### 📝 Notes
- R2DBC is async/non-blocking, unlike JPA (blocking)
- @Repository annotation auto-registers with R2DBC
- Primary keys: @Id (no @GeneratedValue with R2DBC — use strategy in database)
- Always create separate migrations per service

---

## **DAY 6: WebFlux Reactive APIs**

### ✅ Daily Checklist

- [ ] Create R2DBC configuration class
  ```java
  @Configuration
  public class R2dbcConfig {
      // Connection pool settings, etc.
  }
  ```

- [ ] Create PaymentController with Mono/Flux endpoints
  - [ ] POST /api/payments/create → Mono<PaymentResponse>
  - [ ] GET /api/payments/{id} → Mono<PaymentResponse>
  - [ ] GET /api/payments/user/{userId} → Flux<PaymentResponse>

- [ ] Create PaymentService with reactive methods
- [ ] Implement payment creation logic
- [ ] Add request validation
- [ ] Create global exception handler for reactive:
  ```java
  @RestControllerAdvice
  public class GlobalExceptionHandler {
      @ExceptionHandler(Throwable.class)
      public Mono<ResponseEntity<?>> handle(Throwable e) {
          return Mono.just(ResponseEntity.status(400).body(e.getMessage()));
      }
  }
  ```

- [ ] Test endpoints:
  ```bash
  # Create payment
  curl -X POST http://localhost:8081/api/payments/create \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <TOKEN>" \
    -H "X-User-Id: 1" \
    -d '{
      "amount": 1000,
      "currency": "INR",
      "paymentMethod": "CREDIT_CARD"
    }'
  
  # Get payment
  curl -X GET http://localhost:8081/api/payments/1 \
    -H "Authorization: Bearer <TOKEN>"
  
  # List user payments
  curl -X GET http://localhost:8081/api/payments/user/1 \
    -H "Authorization: Bearer <TOKEN>"
  ```

- [ ] Write reactive tests using StepVerifier

### 🔧 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `Netty port 8081 already in use` | Another service on same port | Change port in application.yml |
| `Mono/Flux returns empty immediately` | Not subscribing or subscriber completes early | Check flatMap, map, doOnNext chains |
| `NullPointerException in reactive chain` | Mono/Flux returns empty, code doesn't handle it | Use `defaultIfEmpty()`, `onErrorResume()` |
| `Request body not deserialized` | Missing @RequestBody or wrong content-type | Add @RequestBody, ensure client sends application/json |
| `CORS error on payment endpoint` | CORS not configured for payment service | Add @CrossOrigin to PaymentController |

### 📝 Notes
- Reactive means non-blocking: Mono (0-1 item), Flux (0-N items)
- Always return Mono/Flux from service, not blocking objects
- flatMap for dependent async operations, map for sync transforms
- WebFlux uses Netty (not Tomcat) — different threading model

---

## **DAY 7: Kafka Event Publishing**

### ✅ Daily Checklist

- [ ] Create Kafka configuration class with topics
- [ ] Create PaymentCreatedEvent, PaymentCompletedEvent, PaymentFailedEvent DTOs
- [ ] Create PaymentEventPublisher service
- [ ] Add Kafka dependencies:
  ```xml
  <dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
  </dependency>
  ```

- [ ] Configure Kafka in application.yml:
  ```yaml
  spring:
    kafka:
      bootstrap-servers: localhost:9092
      producer:
        value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  ```

- [ ] Call publishPaymentCreated() when payment is created
- [ ] Create Kafka consumer listener
- [ ] Publish test event:
  ```bash
  docker exec kafka-container kafka-console-consumer \
    --topic payment.created \
    --bootstrap-server localhost:9092 \
    --from-beginning
  ```

- [ ] Create payment via API and watch event in Kafka
- [ ] Test consumer processes events

### 🔧 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `Cannot connect to Kafka broker` | Zookeeper not ready or broker not advertised | Wait 30s, check `KAFKA_ADVERTISED_LISTENERS` in docker-compose.yml |
| `Topic not found` | Topic auto-creation disabled | Either enable `auto.create.topics.enable=true` or create via Kafka CLI |
| `Serialization error` | Event DTO not serializable | Add Jackson serialization annotations, ensure all fields have getters/setters |
| `Consumer doesn't receive messages` | Consumer group offset at end | Use `--from-beginning` flag or reset consumer group offset |
| `KafkaTemplate not injected` | Missing @EnableKafka or Spring Kafka not on classpath | Add @EnableKafka to main class |

### 📝 Notes
- Kafka topic naming: `service.event-type` (e.g., payment.created)
- Always use 3 partitions, 1 replica for local dev
- Consumer group allows multiple consumers to share messages
- Events should include minimal data (ID, timestamp) — fetch full details separately

---

## **DAY 8: Redis Rate Limiting**

### ✅ Daily Checklist

- [ ] Add Redis dependencies:
  ```xml
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
  </dependency>
  <dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
  </dependency>
  ```

- [ ] Create RedisRateLimiter service (token bucket algorithm)
- [ ] Create @RateLimit annotation
- [ ] Create RateLimitAspect (AOP)
- [ ] Configure Redis in application.yml:
  ```yaml
  spring:
    redis:
      host: localhost
      port: 6379
      timeout: 2000
  ```

- [ ] Apply @RateLimit to login endpoint (5/min per IP)
- [ ] Apply @RateLimit to payment creation (10/min per user)
- [ ] Test rate limiting:
  ```bash
  # Make 6 requests in quick succession
  for i in {1..6}; do
    curl -X POST http://localhost:8080/api/auth/login ...
  done
  # 6th request should get 429 (Too Many Requests)
  ```

- [ ] Check Redis keys:
  ```bash
  docker exec redis-container redis-cli KEYS "rate-limit:*"
  ```

- [ ] Monitor TTL (time-to-live) on keys
  ```bash
  docker exec redis-container redis-cli TTL "rate-limit:123.456.789.0"
  ```

### 🔧 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `Cannot connect to Redis` | Redis not running or port wrong | `docker-compose logs redis-container` |
| `@RateLimit not triggered` | Aspect not enabled or @RateLimit not imported | Ensure @EnableAspectJAutoProxy on config class |
| `Rate limit never resets` | TTL not set on Redis key | Check `redisTemplate.expire()` is called |
| `Always returns 429` | Limit too low or counter not resetting | Check limit value matches requirement |
| `Cannot get client IP behind proxy` | X-Forwarded-For header not sent | Use `request.getHeader("X-Forwarded-For")` or gateway sets it |

### 📝 Notes
- Token bucket: count increments per request, resets after window
- Redis keys format: `rate-limit:{key}` (e.g., `rate-limit:192.168.1.1`)
- TTL should match window (60s for 1-minute windows)
- Return 429 with `Retry-After` header for user experience

---

## **DAY 9: API Gateway (Optional)**

### ✅ Daily Checklist

- [ ] Create api-gateway module
- [ ] Add Spring Cloud Gateway dependency:
  ```xml
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
  </dependency>
  ```

- [ ] Create gateway configuration:
  ```yaml
  spring:
    cloud:
      gateway:
        routes:
          - id: auth-service
            uri: http://localhost:8080
            predicates:
              - Path=/api/auth/**
          - id: payment-service
            uri: http://localhost:8081
            predicates:
              - Path=/api/payments/**
  ```

- [ ] Create JWT validation filter for gateway
- [ ] Create global request logging filter
- [ ] Start gateway on port 9090
- [ ] Test routing:
  ```bash
  # Route to auth-service
  curl -X POST http://localhost:9090/api/auth/login ...
  
  # Route to payment-service
  curl -X GET http://localhost:9090/api/payments/1 ...
  ```

- [ ] Test gateway-level authentication

### 📝 Notes
- Gateway is single entry point for all APIs
- Centralized authentication at gateway level
- Optional: Use Netflix Hystrix for circuit breaking

---

## **DAY 10: Integration & E2E Testing**

### ✅ Daily Checklist

- [ ] Add Testcontainers dependency:
  ```xml
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
  </dependency>
  ```

- [ ] Create integration tests:
  ```java
  @SpringBootTest
  @Testcontainers
  class AuthIntegrationTest {
      @Container
      static MySQLContainer<?> mysql = new MySQLContainer<>()
          .withDatabaseName("test_db");
      
      @Test
      void testLoginFlow() {
          // Test: POST /login → verify token returned
      }
  }
  ```

- [ ] Test complete flows:
  - [ ] Login → get token → access protected endpoint
  - [ ] Create payment → Kafka event published → consumer processes
  - [ ] Exceed rate limit → receive 429

- [ ] Run load testing:
  ```bash
  # Install Apache JMeter (if not installed)
  # Create test plan with N concurrent users
  # Run for 5 minutes
  ```

- [ ] Create Swagger/OpenAPI documentation
- [ ] Run all tests: `mvn test`
- [ ] Check test coverage

### 🔧 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `Testcontainer timeout` | Docker image slow to download | Pre-pull image: `docker pull mysql:8.0` |
| `Test database doesn't exist` | Flyway migrations not run in test | Ensure flyway.enabled=true in test application.yml |
| `Tests fail intermittently` | Race conditions or timing issues | Add proper waits, use TestWaitStrategy |
| `Cannot access database in test` | Testcontainer network isolation | Use `@DynamicPropertySource` to set datasource URL |

### 📝 Notes
- Integration tests are slow — run separately: `mvn test -Pintegration`
- Testcontainers start new containers per test class (heavy)
- Use static containers when possible to reuse
- Load testing identifies bottlenecks before production

---

## **DAY 11-12: Monitoring, Security, Documentation**

### ✅ Daily Checklist (Combined)

- [ ] Add Spring Boot Actuator
- [ ] Expose `/actuator/health` and `/actuator/metrics`
- [ ] Create Swagger documentation
  ```xml
  <dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.0.0</version>
  </dependency>
  ```

- [ ] Add logging:
  ```yaml
  logging:
    level:
      root: INFO
      com.microservices: DEBUG
    pattern: "[%d{yyyy-MM-dd HH:mm:ss}] %-5p %logger{36} - %msg%n"
  ```

- [ ] Add MDC (Mapped Diagnostic Context) for correlation IDs
- [ ] Create Dockerfile for each service:
  ```dockerfile
  FROM eclipse-temurin:17-jre-alpine
  COPY target/*.jar app.jar
  ENTRYPOINT ["java","-jar","/app.jar"]
  ```

- [ ] Create health check endpoints
- [ ] Review security: password hashing, JWT secrets, CORS
- [ ] Create deployment guide
- [ ] Create API documentation
- [ ] Final full system test

### 🔧 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `Logs too verbose` | Logging level too high | Set to INFO in production |
| `Cannot find metrics` | Actuator not enabled | Set `management.endpoints.web.exposure.include=*` |
| `Swagger UI blank` | OpenAPI dependency not added | Add springdoc-openapi dependency |

### 📝 Notes
- Health checks verify external dependencies (DB, Kafka, Redis)
- Logs should be structured (JSON format for easy parsing)
- Correlation IDs help trace requests across services

---

## **Final Verification Checklist**

Before considering "complete":

- [ ] All services start without errors
- [ ] Docker Compose fully operational (all containers healthy)
- [ ] Auth service: login/refresh working
- [ ] Payment service: CRUD operations working
- [ ] Rate limiting: 429 responses when limits exceeded
- [ ] Kafka: events published and consumed
- [ ] Redis: rate limit keys stored and expire correctly
- [ ] Database: migrations ran, schema correct
- [ ] Tests: all passing (unit + integration)
- [ ] Documentation: API docs complete
- [ ] No hardcoded secrets (keys, passwords)
- [ ] CORS configured appropriately
- [ ] Error handling consistent across services
- [ ] Logging enabled and tested
- [ ] Monitoring endpoints accessible

---

## **Quick Debug Commands**

### Docker
```bash
# View all running containers
docker ps

# View logs for a service
docker-compose logs -f mysql

# Stop all services
docker-compose down

# Clean and restart
docker-compose down -v && docker-compose up -d
```

### MySQL
```bash
# Connect to MySQL
docker exec -it mysql-container mysql -u root -proot

# List databases
SHOW DATABASES;

# Use database
USE microservices_db;

# List tables
SHOW TABLES;

# View schema
DESC users;

# Check row count
SELECT COUNT(*) FROM users;
```

### Kafka
```bash
# List topics
docker exec kafka-container kafka-topics --list --bootstrap-server localhost:9092

# Create topic
docker exec kafka-container kafka-topics --create \
  --topic payment.created \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

# View messages
docker exec kafka-container kafka-console-consumer \
  --topic payment.created \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

### Redis
```bash
# Connect to Redis
docker exec -it redis-container redis-cli

# Get all keys
KEYS *

# Get specific key
GET rate-limit:192.168.1.1

# Check TTL
TTL rate-limit:192.168.1.1

# Clear all data
FLUSHALL
```

### Maven
```bash
# Clean build
mvn clean install

# Run tests
mvn test

# Run specific test
mvn test -Dtest=AuthControllerTest

# Skip tests
mvn clean install -DskipTests

# Build single module
mvn clean install -f auth-service/pom.xml
```

### Spring Boot
```bash
# Run application
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Check active profile
curl http://localhost:8080/actuator/env | grep active
```

---

## **Testing with cURL**

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }' | jq .
```

### Store token in variable
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}' | jq -r '.accessToken')

echo $TOKEN
```

### Create payment with token
```bash
curl -X POST http://localhost:8081/api/payments/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-User-Id: 1" \
  -d '{
    "amount": 1000,
    "currency": "INR",
    "paymentMethod": "CREDIT_CARD"
  }' | jq .
```

### Get payment
```bash
curl -X GET http://localhost:8081/api/payments/1 \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### Rate limiting test
```bash
# Make 6 requests (limit is 5 per minute)
for i in {1..6}; do
  curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"password123"}' | jq '.message'
done
# 6th request should show rate limit error
```

---

## **Post-Implementation Actions**

1. **Code Review**: Peer review all code (security, best practices)
2. **Performance Tuning**: Profile under load, optimize slow queries
3. **Security Hardening**: Penetration testing, dependency scanning
4. **Deployment Preparation**: Create Kubernetes manifests, CI/CD pipeline
5. **Documentation**: Keep design docs updated
6. **Monitoring**: Set up alerting for errors/anomalies
7. **Backup Strategy**: Database backup plan
8. **Disaster Recovery**: Failover procedures

