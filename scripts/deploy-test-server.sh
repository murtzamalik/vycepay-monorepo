#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

COMPOSE_FILE="docker-compose.vycepay.server.yml"
PROJECT_NAME="vycepay-server"

echo "==> Stopping legacy standalone admin-service container if present (replaced by compose service)"
docker rm -f vycepay-admin-service 2>/dev/null || true

echo "==> Updating nginx BFF upstream (127.0.0.1:9090)"
if [ -f /etc/nginx/sites-available/vycepay.conf ]; then
  sed -i "s|server 127.0.0.1:9090;|server 127.0.0.1:9090;|g" /etc/nginx/sites-available/vycepay.conf
fi

echo "==> Updating nginx callback upstream (host 8081 -> docker ${CALLBACK_HOST_PORT:-18081})"
CALLBACK_PORT="${CALLBACK_HOST_PORT:-18081}"
if [ -f /etc/nginx/sites-available/vycepay.conf ]; then
  sed -i "s|server 127.0.0.1:8081;|server 127.0.0.1:${CALLBACK_PORT};|g" /etc/nginx/sites-available/vycepay.conf
  nginx -t && systemctl reload nginx
fi

echo "==> Building and starting VycePay server stack + admin portal"
docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" up -d --build

echo "==> Waiting for admin API"
for i in $(seq 1 90); do
  if curl -fsS http://127.0.0.1:8090/actuator/health >/dev/null 2>&1; then
    echo "Admin API is up"
    break
  fi
  if [ "$i" -eq 90 ]; then
    echo "Admin API did not become healthy in time" >&2
    docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" logs --tail=80 vycepay admin-portal || true
    exit 1
  fi
  sleep 5
done

echo "==> Deployment complete"
docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" ps
