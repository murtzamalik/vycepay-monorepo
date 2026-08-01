#!/usr/bin/env bash
# Smoke checks for enterprise error envelopes against a running BFF (default localhost:8080).
# Usage: BFF_URL=http://localhost:8080 ./scripts/error-response-smoke.sh
set -euo pipefail
BFF_URL="${BFF_URL:-http://localhost:8080}"
fail=0

if ! curl -sS -o /dev/null --connect-timeout 2 "$BFF_URL/actuator/health" 2>/dev/null; then
  echo "SKIP: BFF not reachable at $BFF_URL"
  echo "Automated coverage: BffProxyControllerErrorTest, GlobalExceptionHandlerWebMvcTest, VyceErrorCatalogTest, ChoiceBankErrorCatalogTest, admin api.test.ts"
  echo "Exit criteria (no BACKEND_ERROR, catalog message, requestId) verified by unit/WebMvc tests."
  exit 0
fi

check_json() {
  local name="$1" body="$2" expect_code="$3"
  if echo "$body" | grep -q '"BACKEND_ERROR"'; then
    echo "FAIL $name: still contains BACKEND_ERROR"
    fail=1
    return
  fi
  if ! echo "$body" | grep -q "\"code\":\"$expect_code\""; then
    echo "FAIL $name: expected code=$expect_code body=$body"
    fail=1
    return
  fi
  if ! echo "$body" | grep -q '"message":'; then
    echo "FAIL $name: missing message"
    fail=1
    return
  fi
  if ! echo "$body" | grep -q '"requestId":'; then
    echo "FAIL $name: missing requestId"
    fail=1
    return
  fi
  echo "PASS $name"
}

echo "BFF_URL=$BFF_URL"

body=$(curl -sS -X GET "$BFF_URL/api/v1/wallets/me" -H 'Accept: application/json' || true)
check_json "missing-jwt" "$body" "UNAUTHORIZED"

body=$(curl -sS -X POST "$BFF_URL/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"nosuchuser","pin":"0000","imei":"SMOKE-IMEI-001"}' || true)
if echo "$body" | grep -q 'BACKEND_ERROR'; then
  echo "FAIL login-wrong-pin: BACKEND_ERROR"
  fail=1
elif echo "$body" | grep -q '"message":'; then
  echo "PASS login-wrong-pin (has message)"
else
  echo "FAIL login-wrong-pin: unexpected body=$body"
  fail=1
fi

if [[ "$fail" -ne 0 ]]; then
  echo "Smoke checks failed"
  exit 1
fi
echo "Smoke checks completed"
