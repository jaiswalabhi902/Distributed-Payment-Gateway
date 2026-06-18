# Microservices Platform

A production-oriented Spring Boot microservices system: JWT-secured auth, a reactive
payment service with event publishing and rate limiting, an API gateway, and a full
metrics stack.

## Architecture

```
                    ┌────────────────────┐
   client  ───────▶ │  API Gateway :8090 │  validates JWT, relays X-User-* headers
                    └─────────┬──────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                                ▼
   ┌────────────────────┐         ┌────────────────────────┐
   │  Auth Service :8080│         │  Payment Service :8081 │
   │  Spring MVC + JPA  │         │  WebFlux + R2DBC       │
   │  JWT RS256, RBAC   │         │  Kafka, Redis ratelimit│
   └─────────┬──────────┘         └───────────┬────────────┘
             │                                 │
             ▼                                 ▼
        MySQL (Flyway)   Kafka   Redis    Prometheus ─▶ Grafana
```

| Service         | Port | Stack                              |
|-----------------|------|------------------------------------|
| api-gateway     | 8090 | Spring Cloud Gateway (reactive)    |
| auth-service    | 8080 | Spring MVC, Spring Security, JPA   |
| payment-service | 8081 | Spring WebFlux, R2DBC, Kafka, Redis|
| MySQL           | 3306 | Database (Flyway-managed schema)   |
| Kafka / Redis   | 9092 / 6379 | Events / cache + rate limiting |
| Prometheus      | 9090 | Metrics scraping                   |
| Grafana         | 3000 | Dashboards (admin/admin)           |

`common` is a shared library (DTOs, exceptions, constants, logging config).

> 📐 **See [ARCHITECTURE.md](ARCHITECTURE.md)** for the full design: component breakdown,
> request/event flow sequence diagrams, the data model, and the deployment topology
> (all diagrams render on GitHub).

## Run the full stack (Docker)

Requires Docker Desktop running.

```bash
docker compose up --build
```

Then:
- Gateway: http://localhost:8090
- Grafana: http://localhost:3000 (admin / admin)
- Prometheus: http://localhost:9090

### Smoke test

```bash
# Login (public route)
TOKEN=$(curl -s -X POST http://localhost:8090/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .data.accessToken)

# Create a payment (identity taken from the JWT)
curl -X POST http://localhost:8090/api/payments/create \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"orderId":"ORD-1","amount":99.99,"currency":"USD","paymentMethod":"CREDIT_CARD"}'
```

## Run a single service locally

The repo builds with Java 17 (Lombok is pinned for newer JDK compatibility). Services
read connection settings from env vars and default to a local MySQL on **port 3307**.

```bash
# Build everything
./auth-service/mvnw -DskipTests install

# Run one service (from its module directory)
cd auth-service && ./mvnw spring-boot:run
```

Override config via env vars, e.g. `DB_USERNAME`, `DB_PASSWORD`, `SPRING_DATASOURCE_URL`,
`SPRING_R2DBC_URL`, `KAFKA_BOOTSTRAP_SERVERS`, `REDIS_HOST`, `JWT_KEY_DIR`.

## Security model

- Auth-service signs RS256 JWTs with an RSA key pair (generated on first start into
  `JWT_KEY_DIR`, or mounted in production via the shared `jwt-keys` volume).
- The gateway verifies the JWT with the public key, **strips any client-supplied
  `X-User-*` headers**, and injects the verified identity downstream.
- Downstream services trust the gateway's identity headers, so they must only be
  reachable through the gateway (network isolation in deployment).

## CI/CD

`.github/workflows/ci.yml` builds and tests against a MySQL service container, then on
`main` builds and pushes images for each service to GHCR.
