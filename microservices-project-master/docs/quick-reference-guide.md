# Microservices Implementation - Quick Reference Guide

---

## **DAY 1: Docker Compose Setup**

### docker-compose.yml
```yaml
version: '3.8'

services:
  # MySQL Database
  mysql:
    image: mysql:8.0
    container_name: mysql-container
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: microservices_db
      MYSQL_USER: devuser
      MYSQL_PASSWORD: devpass
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - microservices-net

  # Kafka Zookeeper
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: zookeeper-container
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"
    networks:
      - microservices-net

  # Kafka Broker
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka-container
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
    networks:
      - microservices-net

  # Redis
  redis:
    image: redis:7-alpine
    container_name: redis-container
    ports:
      - "6379:6379"
    networks:
      - microservices-net

volumes:
  mysql-data:

networks:
  microservices-net:
    driver: bridge
```

### Parent pom.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.microservices</groupId>
    <artifactId>microservices-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <name>Microservices Parent</name>
    <description>Parent POM for microservices</description>

    <modules>
        <module>auth-service</module>
        <module>payment-service</module>
        <module>api-gateway</module>
    </modules>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.1.5</version>
        <relativePath/>
    </parent>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <spring-cloud.version>2022.0.4</spring-cloud.version>
        <jjwt.version>0.12.3</jjwt.version>
    </properties>

    <dependencies>
        <!-- Common for all services -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

---

## **DAY 2: JWT RS256 Setup**

### Generate RSA Keys (Bash)
```bash
#!/bin/bash
mkdir -p src/main/resources/keys

# Generate private key
openssl genrsa -out src/main/resources/keys/private_key.pem 2048

# Generate public key from private key
openssl rsa -in src/main/resources/keys/private_key.pem \
            -pubout -out src/main/resources/keys/public_key.pem

echo "Keys generated successfully!"
```

### JwtProperties.java
```java
package com.microservices.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
    private String privateKeyPath = "classpath:keys/private_key.pem";
    private String publicKeyPath = "classpath:keys/public_key.pem";
    private long accessTokenExpirationMs = 900000; // 15 minutes
    private long refreshTokenExpirationMs = 604800000; // 7 days
    private String issuer = "microservices-auth";
    private String audience = "microservices-api";
}
```

### JwtTokenProvider.java
```java
package com.microservices.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Slf4j
@Component
public class JwtTokenProvider {
    
    private final JwtProperties jwtProperties;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    @Autowired
    public JwtTokenProvider(JwtProperties jwtProperties, ResourceLoader resourceLoader) {
        this.jwtProperties = jwtProperties;
        loadKeys(resourceLoader);
    }

    private void loadKeys(ResourceLoader resourceLoader) {
        try {
            // Load private key
            Resource privateKeyResource = resourceLoader.getResource(jwtProperties.getPrivateKeyPath());
            byte[] privateKeyBytes = Files.readAllBytes(Paths.get(privateKeyResource.getURI()));
            String privateKeyPEM = new String(privateKeyBytes)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            
            byte[] decodedPrivateKey = Base64.getDecoder().decode(privateKeyPEM);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedPrivateKey);
            this.privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);

            // Load public key
            Resource publicKeyResource = resourceLoader.getResource(jwtProperties.getPublicKeyPath());
            byte[] publicKeyBytes = Files.readAllBytes(Paths.get(publicKeyResource.getURI()));
            String publicKeyPEM = new String(publicKeyBytes)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            
            byte[] decodedPublicKey = Base64.getDecoder().decode(publicKeyPEM);
            X509EncodedKeySpec keySpec2 = new X509EncodedKeySpec(decodedPublicKey);
            this.publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec2);
            
            log.info("RSA keys loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load RSA keys", e);
            throw new RuntimeException("Failed to load RSA keys", e);
        }
    }

    public String generateAccessToken(String userId, Set<String> roles) {
        return buildToken(userId, roles, jwtProperties.getAccessTokenExpirationMs());
    }

    public String generateRefreshToken(String userId) {
        return buildToken(userId, new HashSet<>(), jwtProperties.getRefreshTokenExpirationMs());
    }

    private String buildToken(String userId, Set<String> roles, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String getUserIdFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        List<String> roles = claims.get("roles", List.class);
        return new HashSet<>(roles != null ? roles : new ArrayList<>());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getPayload();
    }
}
```

---

## **DAY 3: Database Schema**

### Flyway Migration: V1__Initial_Schema.sql
```sql
-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Permissions table
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User-Roles junction table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Role-Permissions junction table
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### V2__Insert_Default_Data.sql
```sql
-- Insert default roles
INSERT INTO roles (name, description) VALUES
('ADMIN', 'Administrator role'),
('USER', 'Regular user role'),
('VENDOR', 'Vendor role');

-- Insert default permissions
INSERT INTO permissions (name, description) VALUES
('READ_PAYMENT', 'Can read payment details'),
('CREATE_PAYMENT', 'Can create new payments'),
('APPROVE_PAYMENT', 'Can approve pending payments'),
('REFUND_PAYMENT', 'Can refund completed payments');
```

---

## **DAY 4: Authentication Controller**

### LoginRequest & LoginResponse DTOs
```java
package com.microservices.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String tokenType = "Bearer";
}
```

### AuthController.java
```java
package com.microservices.auth.controller;

import com.microservices.auth.dto.LoginRequest;
import com.microservices.auth.dto.LoginResponse;
import com.microservices.auth.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthenticationService authenticationService;

    @Autowired
    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        LoginResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@RequestParam String refreshToken) {
        log.info("Token refresh requested");
        LoginResponse response = authenticationService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
}
```

---

## **DAY 5-6: Payment Service WebFlux**

### application.yml (Payment Service)
```yaml
spring:
  application:
    name: payment-service
  
  r2dbc:
    url: r2dbc:mysql://root:root@localhost:3306/microservices_db
    username: root
    password: root
    pool:
      max-acquire-time: 2000
      max-idle-time: 30m
  
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      bootstrap-servers: localhost:9092
      group-id: payment-service-group
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
  
  data:
    redis:
      host: localhost
      port: 6379

server:
  port: 8081
```

### Payment Entity (R2DBC)
```java
package com.microservices.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("payments")
public class Payment {
    @Id
    private Long id;
    
    @Column("order_id")
    private String orderId;
    
    @Column("user_id")
    private Long userId;
    
    @Column("amount")
    private BigDecimal amount;
    
    @Column("currency")
    private String currency;
    
    @Column("status")
    private String status; // PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED
    
    @Column("payment_method")
    private String paymentMethod;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
```

### PaymentRepository.java
```java
package com.microservices.payment.repository;

import com.microservices.payment.entity.Payment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PaymentRepository extends R2dbcRepository<Payment, Long> {
    Mono<Payment> findByOrderId(String orderId);
    
    Flux<Payment> findByUserId(Long userId);
    
    Flux<Payment> findByStatus(String status);
    
    @Query("SELECT * FROM payments WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    Flux<Payment> findLatestPaymentsByUser(Long userId, int limit);
}
```

### PaymentController.java
```java
package com.microservices.payment.controller;

import com.microservices.payment.dto.CreatePaymentRequest;
import com.microservices.payment.dto.PaymentResponse;
import com.microservices.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public Mono<ResponseEntity<PaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("Creating payment for user: {}", userId);
        return paymentService.createPayment(request, userId)
                .map(payment -> ResponseEntity.status(HttpStatus.CREATED).body(payment))
                .onErrorResume(error -> {
                    log.error("Payment creation failed: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
                });
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<PaymentResponse>> getPayment(@PathVariable Long id) {
        return paymentService.getPayment(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public Flux<PaymentResponse> getUserPayments(@PathVariable Long userId) {
        return paymentService.getUserPayments(userId);
    }
}
```

### PaymentService.java
```java
package com.microservices.payment.service;

import com.microservices.payment.dto.CreatePaymentRequest;
import com.microservices.payment.dto.PaymentResponse;
import com.microservices.payment.entity.Payment;
import com.microservices.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, 
                         PaymentEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    public Mono<PaymentResponse> createPayment(CreatePaymentRequest request, Long userId) {
        return Mono.fromCallable(() -> {
            // Validate request
            if (request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero");
            }
            
            Payment payment = Payment.builder()
                    .orderId(UUID.randomUUID().toString())
                    .userId(userId)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status("PENDING")
                    .paymentMethod(request.getPaymentMethod())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            return payment;
        })
        .flatMap(payment -> paymentRepository.save(payment))
        .doOnNext(payment -> {
            log.info("Payment created: {}", payment.getId());
            eventPublisher.publishPaymentCreated(payment);
        })
        .map(this::mapToResponse);
    }

    public Mono<PaymentResponse> getPayment(Long id) {
        return paymentRepository.findById(id)
                .map(this::mapToResponse);
    }

    public Flux<PaymentResponse> getUserPayments(Long userId) {
        return paymentRepository.findByUserId(userId)
                .map(this::mapToResponse);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
```

---

## **DAY 7: Kafka Integration**

### PaymentCreatedEvent.java
```java
package com.microservices.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreatedEvent {
    private Long paymentId;
    private String orderId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private LocalDateTime timestamp;
}
```

### PaymentEventPublisher.java
```java
package com.microservices.payment.service;

import com.microservices.payment.entity.Payment;
import com.microservices.payment.event.PaymentCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentCreated(Payment payment) {
        PaymentCreatedEvent event = PaymentCreatedEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send("payment.created", payment.getId().toString(), event);
        log.info("Payment created event published for payment: {}", payment.getId());
    }

    public void publishPaymentCompleted(Payment payment) {
        kafkaTemplate.send("payment.completed", payment.getId().toString(), payment);
        log.info("Payment completed event published for payment: {}", payment.getId());
    }

    public void publishPaymentFailed(Payment payment, String reason) {
        kafkaTemplate.send("payment.failed", payment.getId().toString(), 
                Map.of("paymentId", payment.getId(), "reason", reason));
        log.info("Payment failed event published for payment: {}", payment.getId());
    }
}
```

### KafkaConfig.java
```java
package com.microservices.payment.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin admin() {
        return new KafkaAdmin(new org.springframework.kafka.core.KafkaAdmin.AdminFactory()
                .withBootstrapServers(bootstrapServers)
                .withAdminProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                .adminProperties());
    }

    @Bean
    public NewTopic paymentCreatedTopic() {
        return TopicBuilder.name("payment.created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name("payment.completed")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name("payment.failed")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
```

---

## **DAY 8: Redis Rate Limiting**

### RedisRateLimiter.java
```java
package com.microservices.common.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisRateLimiter {

    private final RedisTemplate<String, Integer> redisTemplate;

    @Autowired
    public RedisRateLimiter(RedisTemplate<String, Integer> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String key, int maxRequests, long windowSeconds) {
        String redisKey = "rate-limit:" + key;
        
        Long currentCount = redisTemplate.opsForValue().increment(redisKey);
        
        if (currentCount == 1) {
            redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }
        
        boolean allowed = currentCount <= maxRequests;
        
        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}", key);
        }
        
        return allowed;
    }

    public long getRemainingRequests(String key, int maxRequests) {
        String redisKey = "rate-limit:" + key;
        Long currentCount = redisTemplate.opsForValue().get(redisKey);
        return Math.max(0, maxRequests - (currentCount != null ? currentCount : 0));
    }
}
```

### RateLimitAspect.java
```java
package com.microservices.common.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private final RedisRateLimiter rateLimiter;

    @Autowired
    public RateLimitAspect(RedisRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = 
            ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        
        String clientIdentifier = getClientIdentifier(request, rateLimit.keyType());
        
        if (!rateLimiter.isAllowed(clientIdentifier, rateLimit.limit(), rateLimit.windowSeconds())) {
            log.warn("Rate limit exceeded for client: {}", clientIdentifier);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Try again later.");
        }
        
        return joinPoint.proceed();
    }

    private String getClientIdentifier(HttpServletRequest request, RateLimit.KeyType keyType) {
        return switch (keyType) {
            case IP -> getClientIp(request);
            case USER_ID -> request.getHeader("X-User-Id");
            default -> getClientIp(request);
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}
```

### RateLimit Annotation
```java
package com.microservices.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit() default 10;
    long windowSeconds() default 60;
    KeyType keyType() default KeyType.IP;

    enum KeyType {
        IP, USER_ID
    }
}
```

### Usage in Controller
```java
@PostMapping("/login")
@RateLimit(limit = 5, windowSeconds = 60, keyType = RateLimit.KeyType.IP)
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    // ... implementation
}

@PostMapping("/create")
@RateLimit(limit = 10, windowSeconds = 60, keyType = RateLimit.KeyType.USER_ID)
public Mono<ResponseEntity<PaymentResponse>> createPayment(
        @Valid @RequestBody CreatePaymentRequest request,
        @RequestHeader("X-User-Id") Long userId) {
    // ... implementation
}
```

---

## **Quick Commands Reference**

```bash
# Start Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f

# Create Kafka topics
docker exec kafka-container kafka-topics --create \
  --topic payment.created \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

# Check Kafka topics
docker exec kafka-container kafka-topics \
  --list \
  --bootstrap-server localhost:9092

# View Kafka messages
docker exec kafka-container kafka-console-consumer \
  --topic payment.created \
  --bootstrap-server localhost:9092 \
  --from-beginning

# Check Redis
docker exec redis-container redis-cli PING

# Get Redis keys
docker exec redis-container redis-cli KEYS "rate-limit:*"

# Maven build
mvn clean install -DskipTests

# Run specific service
mvn spring-boot:run -f auth-service/pom.xml
```

---

## **Testing Endpoints**

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password"
  }'
```

### Create Payment
```bash
curl -X POST http://localhost:8081/api/payments/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{
    "amount": 1000,
    "currency": "INR",
    "paymentMethod": "CREDIT_CARD"
  }'
```

### Get Payment
```bash
curl -X GET http://localhost:8081/api/payments/1 \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

---

## **File Structure**
```
microservices-project/
├── docker-compose.yml
├── pom.xml (parent)
├── auth-service/
│   ├── pom.xml
│   ├── src/main/
│   │   ├── java/com/microservices/auth/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── AuthApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── keys/
│   │       │   ├── private_key.pem
│   │       │   └── public_key.pem
│   │       └── db/migration/
├── payment-service/
│   ├── pom.xml
│   ├── src/main/
│   │   ├── java/com/microservices/payment/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── event/
│   │   │   ├── dto/
│   │   │   └── PaymentApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
└── common/
    └── src/main/java/com/microservices/common/
        ├── ratelimit/
        ├── exception/
        └── util/
```

