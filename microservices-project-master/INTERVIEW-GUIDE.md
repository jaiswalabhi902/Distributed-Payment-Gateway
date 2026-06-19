# Interview Guide — Distributed Payment Gateway

A study sheet to explain this project in interviews: what it does, how it's built, the
decisions, the problems solved, and the numbers. Everything here reflects what was
actually built and measured — interviewers probe, so don't overclaim.

---

## 1. The 30-second pitch

> "I built a distributed payment gateway as a set of Spring Boot microservices behind an
> API gateway. It has a JWT-secured auth service, a **reactive** payment service that
> processes payments and emits events to Kafka, Redis-based rate limiting, and a
> merchant-facing API (API keys + HMAC-signed payments) that a merchant's website can
> integrate with. It's fully containerized with Docker Compose, observable via Prometheus
> + Grafana, has a React + TypeScript dashboard, and ships through a GitHub Actions
> pipeline. I load-tested it to zero errors with no event loss and ~37 ms server-side
> latency."

---

## 2. What it does (the use case)

It's a **payment platform** with two sides:

- **Internal / admin side** — users authenticate (JWT), create and manage payments, issue
  refunds, and view a dashboard. RBAC controls who can do what.
- **Merchant side** — a business onboards, gets an `key_id` / `key_secret`, and integrates
  the gateway into their website: create an order, the customer pays, the gateway returns
  an **HMAC-signed** payment result the merchant verifies server-side. This is the
  Razorpay/Stripe integration model (INR, amounts in paise).

The actual money movement is behind a swappable `PaymentProvider` interface — today a mock
provider (so the whole flow runs end to end), and a real PSP (Razorpay/Cashfree) drops in
without touching the order/signature logic.

**Why it's a good portfolio project:** it demonstrates microservices, two HTTP paradigms
(blocking MVC + reactive WebFlux), event-driven design, security done correctly, full
observability, and a complete CI/CD + container story.

---

## 3. Architecture (one breath)

```
Client / React SPA
        │  HTTPS
   API Gateway (Spring Cloud Gateway, :8090)   ← validates JWT, injects identity headers
        ├── Auth Service   (Spring MVC + JPA, :8080)     → MySQL
        └── Payment Service(Spring WebFlux + R2DBC, :8081) → MySQL, Kafka, Redis
                                                         ↑ Prometheus scrapes → Grafana
```

- **Gateway** = single entry point. It authenticates **once** at the edge and forwards a
  trusted identity, so downstream services stay simple and can't be reached directly.
- Services share one MySQL (separate Flyway history tables) for the demo; in real life each
  would own its database.
- See `ARCHITECTURE.md` for full diagrams (system, sequence flows, ER, deployment).

---

## 4. Tech stack & *why each*

| Area | Choice | Why this one |
|---|---|---|
| Language/Framework | Java 17, Spring Boot 3.1 | Mature ecosystem, the de-facto microservices stack |
| Gateway | Spring Cloud Gateway | Reactive, integrates with Spring; one place for auth + routing + CORS |
| Auth | Spring Security + JJWT (RS256) | Asymmetric JWT lets the gateway verify tokens without the signing key |
| Payment | Spring WebFlux + R2DBC | Non-blocking — the hot path scales on a few threads, not thread-per-request |
| Messaging | Apache Kafka | Decouple side-effects (audit/settlement/notifications) from the request path |
| Cache / limiting | Redis (reactive Lettuce) | Fast, shared rate-limit state across instances |
| DB migrations | Flyway | Versioned, reviewable schema; `ddl-auto: none` so Hibernate never alters prod |
| Metrics | Micrometer + Prometheus + Grafana | Standard, low-friction observability |
| Frontend | React 18 + Vite + TS + Tailwind + shadcn/ui | Fast DX, type safety, polished components |
| Containers | Docker (multi-stage) + Compose | Reproducible builds, one-command stack |
| CI/CD | GitHub Actions + GHCR | Test + build images on every push |

---

## 5. Deep dive — Authentication (how it works)

1. **Login** → auth-service verifies the password with **BCrypt**, then issues:
   - an **access token** (JWT, RS256, 15-min expiry) signed with an **RSA private key**, and
   - a **refresh token** (opaque, 7-day, stored in DB, **rotated** on each use).
2. **Every protected request** goes through the gateway, which **verifies the JWT with the
   RSA public key** (it never holds the private key). On success it:
   - **strips any client-supplied `X-User-*` headers** (anti-spoofing), and
   - **injects the verified identity** (`X-User-Id`, `X-User-Roles`) for downstream services.
3. Downstream services trust those headers (and are only reachable through the gateway).
4. **RBAC**: roles (`ROLE_ADMIN/USER/MERCHANT`) and granular permissions seeded via Flyway;
   the access token carries roles; method/route checks enforce them.

**Key talking point — why asymmetric (RS256) not HMAC (HS256)?** With RS256 only the auth
service can *mint* tokens (private key), while any number of services can *verify* them with
the public key. No shared secret to leak across services. I proved the anti-spoofing: I sent
`X-User-Id: 999` with a valid admin token and the created payment still recorded `userId: 1`
(from the token, not the header).

---

## 6. Deep dive — Payment processing (how it works)

- **Reactive end to end**: WebFlux controllers → `PaymentService` (Reactor `Mono`/`Flux`) →
  R2DBC repositories. No blocking calls on the event loop.
- **Lifecycle**: create (PENDING) → status updates → refund (PARTIAL_REFUND/REFUNDED).
  Refunds validate against the refundable balance; **optimistic locking** (`@Version`)
  guards concurrent updates.
- **Rate limited** per user/IP via a Redis fixed-window counter (WebFilter). It **fails
  open** — if Redis is down, requests are allowed (cache is not a hard dependency).
- **Emits a Kafka event** after each create/update/refund (non-blocking — see §7).
- **Merchant flow** adds orders + API-key auth + HMAC signatures on top (see §10 of
  ARCHITECTURE / the merchant section).

---

## 7. Deep dive — Kafka (producers / consumers / topics)

- **Topics (3):** `payment.created`, `payment.updated`, `payment.refunded`.
- **Producer (1):** `PaymentEventPublisher` in the payment service — one producer publishing
  to all three topics, keyed by order id. It builds a `PaymentEvent` and sends it.
- **Consumer (1 group):** `PaymentEventConsumer` — a single `@KafkaListener` subscribed to
  all three topics under the consumer group `payment-service-group`. Today it audits/logs;
  in production a **settlement** or **notification** service subscribes to the same topics.
- **Serialization:** JSON (`JsonSerializer`/`JsonDeserializer`) with trusted-package config.

**Best war story here:** publishing was **blocking the reactive event loop for 60 seconds**
when Kafka was down (Kafka's `max.block.ms` default while it fetches metadata). I made the
send fire-and-forget on a **bounded-elastic scheduler** and set `max.block.ms=5000`, so a
broker outage logs a warning but never stalls or fails a payment. In the load test, **140/140
events were produced and consumed with zero loss.**

---

## 8. Deep dive — "How many requests can it handle?" (be honest + architectural)

What I actually measured (load test of **140 operations**: 100 creates + 25 status updates +
15 refunds through the gateway):
- **0 errors, 0 event loss**, 100/100 payments persisted, DB + Kafka reconciled exactly.
- **Server-side latency ~37 ms** average on the create endpoint (from Prometheus
  `http_server_requests`); client-side ~58 ms including curl/network overhead.
- The test was **sequential** (a shell loop), so throughput was client-bound, not a true
  concurrency benchmark — say this plainly.

How I talk about capacity:
- The payment service is **reactive (Netty + WebFlux)**: a small fixed number of event-loop
  threads handle thousands of concurrent connections via non-blocking I/O, versus a
  thread-per-request model that would need a thread per in-flight request.
- A **rate limiter** caps abuse at 100 req/min per user (configurable; I demonstrated it —
  a burst of 20 at limit=10 gave exactly 10×200 and 10×429).
- "To get a real number I'd run **k6 / Gatling / JMeter** with N concurrent virtual users and
  report p95/p99 and throughput; the bottleneck would be MySQL/R2DBC connections, which I'd
  tune via the pool and read replicas."

---

## 9. Deep dive — Frontend (React)

- **Stack:** React 18 + **Vite** (fast builds) + **TypeScript** + **Tailwind** +
  **shadcn/ui** (Radix-based components). Charts with **Recharts**.
- **Data layer:** **TanStack Query** for server state (caching, refetch), **Axios** with
  interceptors that attach the JWT and **auto-refresh** on a 401, and **Zustand** (persisted)
  for auth state.
- **Pages:** login, dashboard (stat cards + status/volume charts), payments table
  (search + filter), create-payment dialog, payment detail (status update + refund).
- It's a **pure API client** — it only talks to the gateway (`VITE_API_URL`), which keeps the
  frontend deployable anywhere and the backend the single source of truth.

---

## 10. Deep dive — Observability (Grafana / Prometheus, *how integrated*)

- Each service exposes **Micrometer** metrics at `/actuator/prometheus` (added the
  `micrometer-registry-prometheus` dependency + exposed the endpoint).
- **Prometheus** scrapes all three services every 15 s (`prometheus.yml` lists them by
  service name on the Docker network). I verified all targets `up`.
- **Grafana** is **provisioned as code** — a datasource pointing at Prometheus and a
  "Microservices Overview" dashboard (service-up, request rate, JVM heap, 5xx rate) shipped
  in the repo, so it works on first boot with no manual clicks.
- **Health probes:** Actuator `liveness`/`readiness` groups; readiness depends only on the DB
  (not Redis/Kafka) so optional infra being down doesn't mark a service unhealthy. Docker
  health checks use these.

---

## 11. Deep dive — Docker & containerization (*how integrated*)

- **Multi-stage Dockerfile per service:** stage 1 builds the module with a **Maven + JDK 17**
  image (also sidesteps a host JDK issue); stage 2 copies just the jar onto a **slim JRE**
  image and runs as a **non-root** user.
- **One `docker-compose.yml`** runs everything: MySQL, Zookeeper, Kafka, Redis, the three
  services, Prometheus, Grafana — with **health checks**, **`depends_on` conditions**, a
  **shared `jwt-keys` volume** (auth writes the RSA keys, gateway reads them), and
  **env-driven** secrets/URLs/ports.
- Only the gateway + monitoring publish host ports; app services are **internal-only**
  (`expose`) — enforcing "all traffic goes through the gateway."

---

## 12. Deep dive — CI/CD & deployment (GitHub → images → Cloudflare / VM)

**CI/CD (GitHub Actions):** on push/PR it spins up a **MySQL service container**, runs
`mvn verify`, and on `main` builds each service image and **pushes to GHCR**
(GitHub Container Registry).

**Backend deployment (Docker Compose on a VM):** copy `.env`, run `docker compose up` on any
Linux VM (AWS EC2, etc.). `DEPLOYMENT.md` has the steps. A reverse proxy (Caddy/Nginx) adds
TLS in front of the gateway.

**Frontend deployment (Cloudflare Pages — "fetching code from GitHub"):** this is the part to
explain clearly:
1. You connect the GitHub repo to **Cloudflare Pages** once (OAuth authorization).
2. On **every push to the branch**, Cloudflare **pulls the code from GitHub**, runs the build
   command (`npm run build`, output `frontend/dist`) in its own build container, and
   **deploys the static files to Cloudflare's global CDN edge**. No server to manage.
3. The SPA calls the backend via `VITE_API_URL` (set as a Pages env var). To reach a backend
   running on your machine/VM you'd expose it with **Cloudflare Tunnel** (cloudflared), which
   gives a public HTTPS URL without opening ports.

So the flow is: **git push → GitHub → Cloudflare Pages builds from the repo → served at the
edge**; the backend runs separately (VM or tunnel) and the frontend hits it over HTTPS.

---

## 13. Problems I faced (coding → testing → deploying) — the war stories

These are the best interview material — concrete problems with root causes and fixes.

**Coding / build**
- **JDK 23+ stopped running Lombok.** Annotation processors on the classpath aren't run
  implicitly anymore, so Lombok generated nothing → "cannot find symbol." Fixed by declaring
  Lombok in the compiler plugin's `annotationProcessorPaths` and pinning its version.
- **Reactive + Flyway clash.** With an R2DBC `ConnectionFactory` present, Spring Boot backs
  off the JDBC `DataSource` auto-config, so **Flyway had no DataSource and silently skipped**.
  Fixed by giving Flyway its own explicit JDBC URL.
- **Flyway baselined over my migration.** In a shared, non-empty DB, `baseline-on-migrate`
  baselined at v1 and **skipped my V1**. Fixed with `baseline-version: 0`.
- **R2DBC enums.** Driver didn't map enums to VARCHAR; wrote custom read/write converters.

**Testing / runtime**
- **Kafka blocked the event loop 60 s** when the broker was down (see §7) — offloaded the
  send to a worker thread + `max.block.ms`.
- **Legacy DB schema mismatch.** The dev DB had an older, incompatible schema; I reset it and
  made **Flyway the single source of truth** (`ddl-auto: none`).

**Deploying**
- **JWT key volume permissions.** The container runs as non-root, but the shared `jwt-keys`
  volume mounted as root, so the app couldn't write the generated RSA key
  (`AccessDeniedException`). Fixed by creating the dir owned by the app user in the image so
  Docker seeds the volume's ownership.
- **Port conflicts** with a leftover stack squatting on 8080/8090/9090/3000 → made published
  ports **env-configurable** and kept app services internal.
- **Disk full.** The host hit ~96 MB free; Docker's WSL virtual disk had grown to 16 GB and
  doesn't auto-shrink. I pruned unused images/volumes and **compacted the WSL vhdx**
  (16.2 → 12.9 GB) to reclaim space — a real ops lesson about WSL2 disk behavior.

---

## 14. Numbers to quote

- **3 microservices** + gateway + `common` lib; **MySQL, Kafka, Redis, Prometheus, Grafana**.
- **Load test:** 140 operations, **0 errors**, **140/140 Kafka events consumed**, ~**37 ms**
  server latency, 100 % data reconciliation.
- **Rate limiter:** 100 req/min/user (configurable); demonstrated 10 pass / 10 × HTTP 429.
- **Auth:** RS256 JWT, 15-min access + 7-day rotating refresh, BCrypt, RBAC.
- **Kafka:** 3 topics, 1 producer, 1 consumer group.
- **Frontend bundle:** ~250 KB gzipped, type-checked build (0 TS errors).

---

## 15. Likely questions & crisp answers

- **"Why microservices over a monolith?"** Independent scaling/deploy of the hot payment
  path; clear ownership boundaries; I can use the right tool per service (reactive for
  payments, blocking for auth). For this scale a monolith would be simpler — I'd say that too.
- **"How do services talk?"** Sync via the gateway (HTTP); async via Kafka for events.
- **"How is the JWT secret protected?"** Asymmetric — private key only on auth, mounted via a
  secret/volume, never in the image or git (it's gitignored).
- **"What if Redis/Kafka go down?"** Rate limiter fails open; event publish is best-effort and
  non-blocking. Neither takes down a payment.
- **"How do you prevent double refunds / races?"** Refundable-balance check + optimistic
  locking (`@Version`).
- **"How would you scale to 10k rps?"** Horizontal-scale the stateless services behind the
  gateway; MySQL read replicas + bigger R2DBC pool; partition Kafka topics; move rate-limit
  to a token-bucket Lua script; add caching; benchmark with k6 to find the real ceiling.
- **"What's not production-ready?"** Single shared DB, in-cluster MySQL, mock PSP, no
  distributed tracing yet, secrets via env not a vault — I can list the gaps honestly.

---

## 16. What I'd do next (shows growth)

Distributed tracing (OpenTelemetry), per-service databases, a real PSP integration, signed
outbound webhooks with retries, a token-bucket limiter, Kubernetes + Helm, and a proper
secrets manager.
