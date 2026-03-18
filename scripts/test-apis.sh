#!/usr/bin/env bash
# VycePay API test script - exercises Auth, KYC, Wallet, Transaction APIs
# Requires: vycepay container running, curl
# Usage: ./scripts/test-apis.sh [--otp <6-digit-code>]
set -e

BASE_URL="http://localhost"
AUTH_URL="${BASE_URL}:8082"
KYC_URL="${BASE_URL}:8083"
WALLET_URL="${BASE_URL}:8084"
TX_URL="${BASE_URL}:8085"
CONTAINER="vycepay"
MOBILE_COUNTRY="254"
MOBILE="799000001"
# Minimal 1x1 transparent PNG (base64)
BASE64_PNG="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="

OTP_ARG=""
while [[ $# -gt 0 ]]; do
  case $1 in
    --otp)
      OTP_ARG="$2"
      shift 2
      ;;
    *)
      shift
      ;;
  esac
done

echo "=== VycePay API Test ==="
echo ""

# Check container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  echo "Error: Container '${CONTAINER}' is not running. Start with: make docker-up-vycepay"
  exit 1
fi

# Phase 1: Auth flow
echo "[1/7] Register..."
curl -s -X POST "${AUTH_URL}/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"mobileCountryCode\":\"${MOBILE_COUNTRY}\",\"mobile\":\"${MOBILE}\"}" \
  -o /dev/null -w "HTTP %{http_code}\n"

sleep 2
echo "[2/7] Extract OTP from logs..."
OTP=$(docker logs "${CONTAINER}" 2>&1 | grep -oE "OTP sent to ${MOBILE_COUNTRY} [^:]+: [0-9]{6}" | tail -1 | grep -oE "[0-9]{6}$")
if [ -z "$OTP" ]; then
  echo "Error: Could not extract OTP from container logs."
  exit 1
fi
echo "OTP found: ****${OTP:4:2}"

echo "[3/7] Verify OTP..."
VERIFY_RESP=$(curl -s -X POST "${AUTH_URL}/api/v1/auth/verify-otp" \
  -H "Content-Type: application/json" \
  -d "{\"mobileCountryCode\":\"${MOBILE_COUNTRY}\",\"mobile\":\"${MOBILE}\",\"otpCode\":\"${OTP}\"}")

if command -v jq &>/dev/null; then
  JWT=$(echo "$VERIFY_RESP" | jq -r '.token')
  EXTERNAL_ID=$(echo "$VERIFY_RESP" | jq -r '.externalId')
else
  JWT=$(echo "$VERIFY_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
  EXTERNAL_ID=$(echo "$VERIFY_RESP" | grep -o '"externalId":"[^"]*"' | cut -d'"' -f4)
fi

if [ -z "$JWT" ] || [ "$JWT" = "null" ]; then
  echo "Error: Verify OTP failed. Response: $VERIFY_RESP"
  exit 1
fi
echo "JWT obtained, externalId=${EXTERNAL_ID}"

AUTH_HEADER="Authorization: Bearer ${JWT}"
CUSTOMER_HEADER="X-Customer-Id: ${EXTERNAL_ID}"

# Phase 2: Protected API smoke tests
echo "[4/7] GET /kyc/status..."
STATUS_RESP=$(curl -s -X GET "${KYC_URL}/api/v1/kyc/status" -H "$AUTH_HEADER" -H "$CUSTOMER_HEADER")
echo "Response: $STATUS_RESP"

echo "GET /wallets/me (expect 404 if no wallet)..."
WALLET_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${WALLET_URL}/api/v1/wallets/me" -H "$AUTH_HEADER" -H "$CUSTOMER_HEADER")
echo "HTTP $WALLET_STATUS"

echo "GET /transactions/bank-codes..."
BANK_CODES_RESP=$(curl -s -X GET "${TX_URL}/api/v1/transactions/bank-codes" -H "$AUTH_HEADER" -H "$CUSTOMER_HEADER")
if echo "$BANK_CODES_RESP" | grep -q "code\|data\|banks"; then
  echo "Bank codes received (length: ${#BANK_CODES_RESP})"
else
  echo "Response: $BANK_CODES_RESP"
fi

# Phase 3: KYC submit (skip if --otp and we already have onboarding from status)
if [ -z "$OTP_ARG" ]; then
echo "[5/7] POST /kyc/submit..."
KYC_BODY=$(cat <<EOF
{
  "firstName": "Test",
  "lastName": "User",
  "birthday": "1990-01-15",
  "gender": 1,
  "countryCode": "254",
  "mobile": "799000001",
  "idType": "101",
  "idNumber": "12345678",
  "frontSidePhoto": "${BASE64_PNG}",
  "selfiePhoto": "${BASE64_PNG}"
}
EOF
)
KYC_RESP=$(curl -s -X POST "${KYC_URL}/api/v1/kyc/submit" \
  -H "$AUTH_HEADER" -H "$CUSTOMER_HEADER" \
  -H "Content-Type: application/json" \
  -d "$KYC_BODY")

if command -v jq &>/dev/null; then
  ONBOARDING_ID=$(echo "$KYC_RESP" | jq -r '.choiceOnboardingRequestId // .onboardingRequestId')
  KYC_STATUS=$(echo "$KYC_RESP" | jq -r '.status')
else
  ONBOARDING_ID=$(echo "$KYC_RESP" | grep -oE '"choiceOnboardingRequestId":"[^"]*"|"onboardingRequestId":"[^"]*"' | cut -d'"' -f4)
  KYC_STATUS=$(echo "$KYC_RESP" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
fi

if [ -z "$ONBOARDING_ID" ] || [ "$ONBOARDING_ID" = "null" ]; then
  echo "Warning: KYC submit may have failed. Response: $KYC_RESP"
  ONBOARDING_ID=""
else
  echo "onboardingRequestId=${ONBOARDING_ID}"
fi
else
  # --otp flow: get onboardingRequestId from kyc/status
  echo "[5/7] GET /kyc/status (to get onboardingRequestId)..."
  STATUS_RESP=$(curl -s -X GET "${KYC_URL}/api/v1/kyc/status" -H "$AUTH_HEADER" -H "$CUSTOMER_HEADER")
  if command -v jq &>/dev/null; then
    ONBOARDING_ID=$(echo "$STATUS_RESP" | jq -r '.choiceOnboardingRequestId // .onboardingRequestId')
  else
    ONBOARDING_ID=$(echo "$STATUS_RESP" | grep -oE '"choiceOnboardingRequestId":"[^"]*"|"onboardingRequestId":"[^"]*"' | cut -d'"' -f4)
  fi
  if [ -z "$ONBOARDING_ID" ] || [ "$ONBOARDING_ID" = "null" ]; then
    echo "Error: No onboardingRequestId found. Run without --otp first to submit KYC."
    exit 1
  fi
  echo "onboardingRequestId=${ONBOARDING_ID}"
fi

# Phase 4: KYC send-otp (skip when --otp, user already received it; skip if no valid onboarding ID)
if [ -z "$OTP_ARG" ] && [ -n "$ONBOARDING_ID" ]; then
echo "[6/7] POST /kyc/send-otp..."
curl -s -X POST "${KYC_URL}/api/v1/kyc/send-otp?onboardingRequestId=${ONBOARDING_ID}" \
  -H "$AUTH_HEADER" -H "$CUSTOMER_HEADER" \
  -o /dev/null -w "HTTP %{http_code}\n"
elif [ -n "$OTP_ARG" ]; then
echo "[6/7] Skipping send-otp (using --otp from previous run)"
else
echo "[6/7] Skipping send-otp (no valid onboardingRequestId from KYC submit)"
fi

# Phase 5: KYC confirm-otp (if OTP provided)
if [ -n "$OTP_ARG" ]; then
  echo "[7/7] POST /kyc/confirm-otp (with provided OTP)..."
  CONFIRM_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    "${KYC_URL}/api/v1/kyc/confirm-otp?onboardingRequestId=${ONBOARDING_ID}&otpCode=${OTP_ARG}" \
    -H "$AUTH_HEADER" -H "$CUSTOMER_HEADER")
  echo "HTTP $CONFIRM_STATUS"

  if [ "$CONFIRM_STATUS" = "200" ]; then
    echo "Waiting for callback (5s)..."
    sleep 5
    echo "GET /wallets/me..."
    WALLET_RESP=$(curl -s -X GET "${WALLET_URL}/api/v1/wallets/me" -H "$AUTH_HEADER" -H "$CUSTOMER_HEADER")
    echo "$WALLET_RESP" | head -c 200
    echo ""
  fi
else
  echo "[7/7] Skipping confirm-otp (no OTP provided)."
  echo ""
  echo "To complete KYC and verify wallet, run with OTP received on mobile:"
  echo "  ./scripts/test-apis.sh --otp <6-digit-code>"
  echo ""
  echo "Note: Choice Bank sends OTP via SMS to ${MOBILE_COUNTRY}${MOBILE}."
  echo "For local callback testing, ensure Choice Bank has your webhook URL."
fi

echo ""
echo "=== Test complete ==="
