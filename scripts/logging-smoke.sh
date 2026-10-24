#!/usr/bin/env bash
# Smoke: confirm JSON logs include service.name (and optionally requestId).
# Usage:
#   CONTAINER=vycepay-server ./scripts/logging-smoke.sh
#   ./scripts/logging-smoke.sh   # defaults to vycepay-server
set -euo pipefail

CONTAINER="${CONTAINER:-vycepay-server}"
BFF_HEALTH="${BFF_HEALTH:-http://127.0.0.1:9090/actuator/health}"

echo "==> Hitting health: ${BFF_HEALTH}"
curl -sS -o /dev/null -w "HTTP %{http_code}\n" "${BFF_HEALTH}" || true

echo "==> Sampling recent docker logs from ${CONTAINER} for JSON service.name"
if ! docker ps --format '{{.Names}}' | grep -qx "${CONTAINER}"; then
  echo "Container ${CONTAINER} is not running. Start the app stack first."
  exit 1
fi

# Look for JSON lines with service.name in the last ~200 lines
sample="$(docker logs --tail 200 "${CONTAINER}" 2>&1 || true)"
if echo "${sample}" | grep -q '"service.name"'; then
  echo "OK: found service.name in container logs"
  echo "${sample}" | grep '"service.name"' | tail -n 3
else
  echo "FAIL: no service.name JSON field found. Is logback-spring.xml on the classpath?"
  echo "--- last 20 log lines ---"
  echo "${sample}" | tail -n 20
  exit 1
fi

if echo "${sample}" | grep -q '"requestId"'; then
  echo "OK: found requestId in recent logs"
else
  echo "NOTE: no requestId in sample (normal if only boot/health logs)."
fi

echo "Smoke complete."
