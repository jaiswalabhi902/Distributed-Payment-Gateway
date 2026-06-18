# Architecture

This document explains **what the system is, what every technology is used for, and how the
pieces interact**. All diagrams are Mermaid and render directly on GitHub.

---

## 1. System overview

A small but production-shaped payment platform built as independent Spring Boot services
behind a single API gateway, with event streaming, caching/rate-limiting, and a metrics stack.

```mermaid
flowchart TB
    client["Client<br/>(browser / curl / frontend)"]

    subgraph edge["Edge"]
        gw["API Gateway :8090<br/>Spring Cloud Gateway"]
    end

    subgraph svc["Application services"]
        auth["Auth Service :8080<br/>Spring MVC · Security · JPA"]
        pay["Payment Service :8081<br/>WebFlux · R2DBC"]
    end

    subgraph infra["Infrastructure"]
        mysql[("MySQL 8<br/>Flyway-managed")]
        kafka[("Kafka + Zookeeper")]
        redis[("Redis 7")]
    end

    subgraph obs["Observability"]
        prom["Prometheus :9090"]
        graf["Grafana :3000"]
    end

    client -->|HTTPS| gw
    gw -->|"/api/auth/**"| auth
    gw -->|"/api/payments/**<br/>+ verified X-User-* headers"| pay

    auth -->|JDBC| mysql
    pay -->|R2DBC| mysql
    pay -->|"publish + consume events"| kafka
    pay -->|"rate-limit counters"| redis

    prom -.->|"scrape /actuator/prometheus"| auth
    prom -.-> pay
    prom -.-> gw
    graf -->|queries| prom
```

**Why this shape**

- The **gateway** is the only entry point — it authenticates requests once at the edge and
  forwards a trusted identity, so downstream services stay simple.
- **Auth** is a classic blocking stack (JPA) because identity data is relational and low-volume.
- **Payment** is **reactive** (WebFlux/R2DBC) because payment traffic is the hot path and
  benefits from non-blocking I/O and backpressure.
- **Kafka** decouples side effects (settlement, notifications, audit) from the request path.
- **Redis** provides fast, shared rate-limit state across payment instances.

---

## 2. Technology stack — what each piece does and how it is used

| Technology | Where | What it is used for / how |
|---|---|---|
| **Java 17, Spring Boot 3.1.5** | all services | Base runtime and DI/auto-config. |
| **Maven (multi-module)** | repo | One reactor: `common`, `auth-service`, `payment-service`, `api-gateway`. |
| **Lombok** | all | Boilerplate (`@Data`, `@Builder`); pinned + declared on the annotation-processor path for JDK 23+. |
| **Spring MVC** | auth | Blocking REST controllers. |
| **Spring WebFlux + Netty** | payment, gateway | Non-blocking reactive HTTP. |
| **Spring Security** | auth | Authentication provider, `BCrypt`, method security, stateless filter chain. |
| **JJWT (`io.jsonwebtoken`)** | auth, gateway | Sign (auth) and verify (gateway) **RS256** JWTs. |
| **RSA keypair** | auth → gateway | Auth signs with the private key; gateway verifies with the public key. Generated on first start and shared via a Docker volume. |
| **Spring Data JPA / Hibernate** | auth | Relational mapping for users/roles/permissions. |
| **Spring Data R2DBC** (`io.asyncer:r2dbc-mysql`) | payment | Reactive DB access; custom converters store enums as `VARCHAR`. |
| **Flyway** | auth, payment | Versioned SQL migrations own the schema (`ddl-auto: none`); separate history tables share one DB. |
| **MySQL 8** | both | Primary datastore. |
| **Spring Kafka** | payment | Publish `payment.created/updated/refunded`; a consumer logs/audits them. Publishing is offloaded so a broker outage never blocks a request. |
| **Spring Data Redis Reactive (Lettuce)** | payment | Fixed-window rate-limit counters; **fails open** if Redis is down. |
| **Spring Cloud Gateway** | gateway | Routing, CORS, and a global JWT/identity filter. |
| **Micrometer + Prometheus** | all | `/actuator/prometheus` metrics endpoint. |
| **Prometheus / Grafana** | monitoring | Scrape + dashboards (provisioned datasource + "Microservices Overview"). |
| **Spring Boot Actuator** | all | Health, info, metrics; `liveness`/`readiness` probe groups. |
| **Logback + logstash-encoder** | common | Human-readable logs in dev, **single-line JSON** in the `prod` profile. |
| **Docker (multi-stage) + Compose** | deploy | Build each service with JDK 17 inside the image; one compose file runs the whole stack. |
| **GitHub Actions** | CI | Build + test against a MySQL service, then build/push images to GHCR on `main`. |

---

## 3. The `common` module

Shared library depended on by every service so contracts stay consistent:

- `dto/ApiResponse`, `dto/ErrorResponse` — uniform success/error envelopes.
- `exception/ResourceNotFoundException`, `BusinessException` — mapped to HTTP status by each service's `@RestControllerAdvice`.
- `constant/Constants` — the propagation header names (`X-User-Id`, …), Kafka topic names, and role names, so producers/consumers and the gateway/services agree.
- `logback-spring.xml` — the shared logging policy.

---

## 4. Request flows

### 4.1 Login (token issuance)

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant A as Auth Service
    participant DB as MySQL

    C->>G: POST /api/auth/login {username, password}
    Note over G: /api/auth/login is a public route — no JWT required
    G->>A: forward
    A->>DB: load user + roles + permissions
    A->>A: BCrypt verify password
    A->>A: sign RS256 access token (15m)
    A->>DB: persist rotating refresh token (7d)
    A-->>G: {accessToken, refreshToken, roles}
    G-->>C: 200 OK
```

### 4.2 Create payment (authenticated, event-driven)

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant P as Payment Service
    participant R as Redis
    participant DB as MySQL
    participant K as Kafka

    C->>G: POST /api/payments/create (Authorization: Bearer JWT)
    G->>G: verify RS256 with public key
    G->>G: strip client X-User-* headers, inject X-User-Id from JWT claims
    G->>P: forward + trusted X-User-Id
    P->>R: INCR rate-limit window (fail-open on error)
    alt within limit
        P->>DB: INSERT payment (R2DBC)
        P-->>G: 201 PaymentResponse
        G-->>C: 201 Created
        P--)K: publish payment.created (async, off the event loop)
        K--)P: consume payment.created (audit log)
    else over limit
        P-->>G: 429 Too Many Requests
        G-->>C: 429
    end
```

**Key security property:** the gateway always removes any client-supplied `X-User-*` headers
and re-injects them from the verified token, so a caller cannot spoof another user's identity.

---

## 5. Data model

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ USER_ROLES : "assigned in"
    ROLES ||--o{ ROLE_PERMISSIONS : grants
    PERMISSIONS ||--o{ ROLE_PERMISSIONS : "granted in"
    USERS ||--o{ REFRESH_TOKENS : owns
    PAYMENTS ||--o{ PAYMENT_REFUNDS : "refunded by"

    USERS {
        bigint id PK
        string username
        string email
        string password
        bool enabled
    }
    ROLES {
        bigint id PK
        string name
    }
    PERMISSIONS {
        bigint id PK
        string name
    }
    REFRESH_TOKENS {
        bigint id PK
        string token
        datetime expiry_date
        bool revoked
    }
    PAYMENTS {
        bigint id PK
        string order_id
        bigint user_id
        decimal amount
        string status
        string payment_method
        decimal refunded_amount
    }
    PAYMENT_REFUNDS {
        bigint id PK
        bigint payment_id FK
        decimal refund_amount
        string status
    }
```

- **Auth schema** (`V1__auth_schema.sql`): users/roles/permissions with join tables and refresh tokens. Roles and permissions are seeded; the admin user is bootstrapped on first run.
- **Payment schema** (`V1__payment_schema.sql`): payments + refunds, with optimistic-locking `version` and indexed `user_id`/`status`/`order_id`.

---

## 6. Event-driven flow

```mermaid
flowchart LR
    P["Payment Service<br/>(producer)"]
    subgraph topics["Kafka topics"]
        t1["payment.created"]
        t2["payment.updated"]
        t3["payment.refunded"]
    end
    P -->|"create / update / refund"| t1 & t2 & t3
    t1 & t2 & t3 -->|"@KafkaListener"| C["Audit consumer<br/>(in payment-service today;<br/>settlement / notifications next)"]
```

Publishing is **best-effort and non-blocking**: the send runs on a worker thread with a short
`max.block.ms`, so a Kafka outage logs an error but never fails or slows the payment request.

---

## 7. Deployment topology (Docker Compose)

```mermaid
flowchart TB
    subgraph host["Docker host / VM"]
        subgraph net["microservices-net (bridge)"]
            gw["api-gateway<br/>:8090 published"]
            auth["auth-service<br/>internal only"]
            pay["payment-service<br/>internal only"]
            mysql[("mysql")]
            kafka[("kafka")]
            zk[("zookeeper")]
            redis[("redis")]
            prom["prometheus<br/>:9090 published"]
            graf["grafana<br/>:3000 published"]
        end
        vol[["jwt-keys volume<br/>(auth writes, gateway reads)"]]
    end

    gw --> auth
    gw --> pay
    auth --> mysql
    pay --> mysql
    pay --> kafka
    pay --> redis
    kafka --> zk
    auth -. writes .-> vol
    gw -. reads .-> vol
    prom --> auth
    prom --> pay
    prom --> gw
    graf --> prom
```

- Only **gateway**, **Prometheus**, and **Grafana** publish host ports; application services are
  reachable only inside the Docker network (enforcing "everything goes through the gateway").
- Auth and gateway share the RSA keys through the `jwt-keys` volume.
- Published ports are env-overridable (`GATEWAY_PORT`, `PROMETHEUS_PORT`, `GRAFANA_PORT`).

See [DEPLOYMENT.md](DEPLOYMENT.md) for VM setup steps and [README.md](README.md) for quick start.

---

## 8. Build & CI/CD

```mermaid
flowchart LR
    dev["push / PR"] --> ci["GitHub Actions"]
    ci --> test["mvn verify<br/>(MySQL service container)"]
    test --> img["build images<br/>(multi-stage, JDK 17)"]
    img --> ghcr["push to GHCR<br/>(on main)"]
    ghcr --> deploy["docker compose up<br/>on the VM"]
```

Each service image is a multi-stage build: Maven compiles the module (and `common`) with a
JDK-17 toolchain, then the jar is copied onto a slim JRE image that runs as a non-root user.
