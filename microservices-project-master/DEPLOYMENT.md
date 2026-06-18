# Deployment Guide (Docker Compose on a VM)

This deploys the full stack (gateway, auth, payment, MySQL, Kafka, Redis, Prometheus,
Grafana) to a single Linux VM using Docker Compose.

## 1. Provision a VM

- Ubuntu 22.04+ (or any Linux), 2 vCPU / 4 GB RAM minimum (8 GB recommended).
- Open inbound ports: **8090** (gateway), **3000** (Grafana), and **9090** (Prometheus,
  optional / restrict to your IP). Everything else stays on the internal Docker network.

## 2. Install Docker

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER"   # re-login after this
```

## 3. Get the code and configure secrets

```bash
git clone <your-repo-url> microservices && cd microservices
cp .env.example .env
# Edit .env and set strong values for every password.
nano .env
```

## 4. Deploy

```bash
./deploy/deploy.sh
```

This builds the service images, starts the stack, and prints the URLs. First boot takes a
few minutes (Maven builds run inside the images and infra containers initialize).

## 5. Verify

```bash
# Health through the gateway
curl -s http://localhost:8090/api/auth/health

# Login and create a payment
TOKEN=$(curl -s -X POST http://localhost:8090/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<ADMIN_PASSWORD from .env>"}' | jq -r .data.accessToken)

curl -X POST http://localhost:8090/api/payments/create \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"orderId":"ORD-1","amount":99.99,"currency":"USD","paymentMethod":"CREDIT_CARD"}'
```

## 6. Operations

```bash
docker compose ps                 # status
docker compose logs -f api-gateway
docker compose pull && docker compose up -d   # update
docker compose down               # stop (keeps volumes/data)
docker compose down -v            # stop and wipe data
```

## Production hardening checklist

- [ ] Put a TLS-terminating reverse proxy (Caddy/Nginx/Traefik) in front of the gateway.
- [ ] Restrict Prometheus/Grafana to a private network or behind auth.
- [ ] Use a managed MySQL (RDS/Cloud SQL) instead of the in-compose MySQL for durability.
- [ ] Mount stable RSA keys into the `jwt-keys` volume rather than auto-generating.
- [ ] Set strong unique secrets in `.env`; never commit it.
- [ ] Configure log shipping from the JSON stdout (prod profile) to your aggregator.
