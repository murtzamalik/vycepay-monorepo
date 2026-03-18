#!/usr/bin/env bash
# Choice Bank API - single script to run any endpoint for the call.
# Usage: ./scripts/choice-bank-call.sh <api-name>
# APIs: getBankCodes | getOperationalAccounts | submitOnboarding
# Output: URL, Request Body, Response (ready to share on call)
set -e

BASE_URL="https://baas-pilot.choicebankapi.com"
SENDER_ID="VYCEIN"
PRIVATE_KEY="9x9euCAYsXxBaw02jzzrBiHGisDVVtf8dMNfhpd17BcZxLK13G56KvjFY3bx69j"
BASE64_PNG="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="

# Postman: requestId = sender + UUID with hyphens (lowercase hex)
REQUEST_ID="${SENDER_ID}$(uuidgen 2>/dev/null | tr 'A-F' 'a-f' || echo "$(date +%s)-$(od -An -N4 -tx1 /dev/urandom | tr -d ' ' | head -c 8)")"
# API responses use 13-digit timestamps (ms). Postman uses seconds; we use ms for API compatibility.
TIMESTAMP=$(($(date +%s) * 1000))
SALT=$(od -An -N6 -tx1 /dev/urandom | tr -d ' ' | head -c 12)

api="${1:-getBankCodes}"

case "$api" in
  getBankCodes)
    ENDPOINT="staticData/getBankCodes"
    # Postman collection: flatten uses body as-is → locale=en_KE (not en_ke)
    STRING_TO_SIGN="locale=en_KE&params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"
    BODY=$(cat <<EOF
{"requestId":"${REQUEST_ID}","sender":"${SENDER_ID}","locale":"en_KE","timestamp":${TIMESTAMP},"salt":"${SALT}","signature":"PLACEHOLDER","params":{}}
EOF
)
    ;;
  getOperationalAccounts)
    ENDPOINT="staticData/getOperationalAccounts"
    STRING_TO_SIGN="locale=en_KE&params.type=0&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"
    SIGNATURE=$(echo -n "$STRING_TO_SIGN" | openssl dgst -sha256 -hex | awk '{print $2}')
    BODY=$(cat <<EOF
{"requestId":"${REQUEST_ID}","sender":"${SENDER_ID}","locale":"en_KE","timestamp":${TIMESTAMP},"salt":"${SALT}","signature":"${SIGNATURE}","params":{"type":0}}
EOF
)
    ;;
  submitOnboarding)
    ENDPOINT="onboarding/v3/submitEasyOnboardingRequest"
    USER_ID="test-user-$(date +%s)"
    PARAM_PAIRS="params.birthday=1990-01-15&params.countryCode=254&params.firstName=Test&params.frontSidePhoto=${BASE64_PNG}&params.gender=1&params.idNumber=12345678&params.idType=101&params.lastName=User&params.mobile=799000001&params.selfiePhoto=${BASE64_PNG}&params.userId=${USER_ID}"
    STRING_TO_SIGN="locale=en_KE&${PARAM_PAIRS}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"
    SIGNATURE=$(echo -n "$STRING_TO_SIGN" | openssl dgst -sha256 -hex | awk '{print $2}')
    BODY=$(cat <<EOF
{"requestId":"${REQUEST_ID}","sender":"${SENDER_ID}","locale":"en_KE","timestamp":${TIMESTAMP},"salt":"${SALT}","signature":"${SIGNATURE}","params":{"userId":"${USER_ID}","firstName":"Test","lastName":"User","birthday":"1990-01-15","gender":1,"countryCode":"254","mobile":"799000001","idType":"101","idNumber":"12345678","frontSidePhoto":"${BASE64_PNG}","selfiePhoto":"${BASE64_PNG}"}}
EOF
)
    ;;
  *)
    echo "Usage: $0 getBankCodes|getOperationalAccounts|submitOnboarding"
    exit 1
    ;;
esac

# Choice Bank: no leading/trailing space in string to sign
STRING_TO_SIGN=$(printf '%s' "$STRING_TO_SIGN" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

if [ "$api" = "getBankCodes" ]; then
  SIGNATURE=$(echo -n "$STRING_TO_SIGN" | openssl dgst -sha256 -hex | awk '{print $2}')
  BODY=$(echo "$BODY" | sed "s/PLACEHOLDER/${SIGNATURE}/")
fi

RESPONSE=$(curl -s -X POST "${BASE_URL}/${ENDPOINT}" -H "Content-Type: application/json" -d "$BODY")

# Show flattened string for sharing (no leading space - copy exactly as used for signature)
FLAT_DISPLAY=$(echo "$STRING_TO_SIGN" | sed "s/senderKey=${PRIVATE_KEY}/senderKey=***REDACTED***/")
echo "=============================================="
echo "API: $api"
echo "=============================================="
echo ""
echo "Flattened string (for signature) - no leading/trailing space:"
echo "$FLAT_DISPLAY"
echo ""
echo "URL:"
echo "  POST ${BASE_URL}/${ENDPOINT}"
echo ""
echo "Request Body:"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""
echo "Response:"
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
echo ""
echo "Postman: Method POST | URL: ${BASE_URL}/${ENDPOINT} | Body: raw JSON (see Request Body above)"
echo ""
