#!/usr/bin/env bash
# Choice Bank API - submitEasyOnboardingRequest (wallet account creation)
# Uses dummy data for testing - params with actual values
set -e

BASE_URL="https://baas-pilot.choicebankapi.com"
SENDER_ID="vycein"
PRIVATE_KEY="9x9euCAYsXxBaw02jzzrBiHGisDVVtf8dMNfhpd17BcZxLK13G56KvjFY3bx69j"
ENDPOINT="onboarding/v3/submitEasyOnboardingRequest"

# Minimal base64 placeholder (1x1 transparent PNG)
BASE64_PNG="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="

REQUEST_ID="CURL-$(date +%s)-$(od -An -N4 -tx1 /dev/urandom | tr -d ' ')"
TIMESTAMP=$(($(date +%s) * 1000))
SALT=$(od -An -N6 -tx1 /dev/urandom | tr -d ' ' | head -c 12)
USER_ID="test-user-$(date +%s)"

# Params for onboarding - flattened for signing (dot notation)
# Keys: birthday, countryCode, firstName, frontSidePhoto, gender, idNumber, idType, lastName, mobile, selfiePhoto, userId
# All in params.* - sort all keys alphabetically
# Full flat keys: locale, params.birthday, params.countryCode, params.firstName, params.frontSidePhoto, params.gender, params.idNumber, params.idType, params.lastName, params.mobile, params.selfiePhoto, params.userId, requestId, salt, sender, senderKey, timestamp

# Build sorted key=value pairs for params
PARAM_PAIRS="params.birthday=1990-01-15&params.countryCode=254&params.firstName=Test&params.frontSidePhoto=${BASE64_PNG}&params.gender=1&params.idNumber=12345678&params.idType=101&params.lastName=User&params.mobile=799000001&params.selfiePhoto=${BASE64_PNG}&params.userId=${USER_ID}"
STRING_TO_SIGN="locale=en_KE&${PARAM_PAIRS}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"
SIGNATURE=$(echo -n "$STRING_TO_SIGN" | openssl dgst -sha256 -hex | awk '{print $2}')

# Redacted (base64 photos shortened for display)
PARAM_PAIRS_REDACTED="params.birthday=1990-01-15&params.countryCode=254&params.firstName=Test&params.frontSidePhoto=<BASE64>&params.gender=1&params.idNumber=12345678&params.idType=101&params.lastName=User&params.mobile=799000001&params.selfiePhoto=<BASE64>&params.userId=${USER_ID}"
STRING_TO_SIGN_REDACTED="locale=en_KE&${PARAM_PAIRS_REDACTED}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=***REDACTED***&timestamp=${TIMESTAMP}"

BODY=$(cat <<BODYEOF
{
  "requestId": "${REQUEST_ID}",
  "sender": "${SENDER_ID}",
  "locale": "en_KE",
  "timestamp": ${TIMESTAMP},
  "salt": "${SALT}",
  "signature": "${SIGNATURE}",
  "params": {
    "userId": "${USER_ID}",
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
}
BODYEOF
)

RESPONSE=$(curl -s -X POST "${BASE_URL}/${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -d "$BODY")

echo "========== FLATTENED STRING (privateKey + base64 redacted) =========="
echo "$STRING_TO_SIGN_REDACTED"
echo ""
echo "========== REQUEST =========="
echo "URL: POST ${BASE_URL}/${ENDPOINT}"
echo "Body (truncated):"
echo "$BODY" | python3 -c "import json,sys; d=json.load(sys.stdin); d['params']['frontSidePhoto']='<BASE64>'; d['params']['selfiePhoto']='<BASE64>'; print(json.dumps(d,indent=2))" 2>/dev/null || echo "$BODY" | head -c 500
echo ""
echo ""
echo "========== RESPONSE =========="
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
