#!/usr/bin/env bash
#
# Deploy the microservices stack on a Linux VM.
# Usage: ./deploy/deploy.sh
#
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo "ERROR: .env not found. Copy .env.example to .env and set real secrets first." >&2
  exit 1
fi

echo "==> Building images"
docker compose build

echo "==> Starting stack"
docker compose up -d

echo "==> Waiting for services to become healthy"
docker compose ps

echo
echo "Gateway:    http://localhost:8090"
echo "Grafana:    http://localhost:3000"
echo "Prometheus: http://localhost:9090"
