#!/usr/bin/env bash
# Choice Bank API with params - staticData/getOperationalAccounts (type=0 for Foreign Currency)
# Tests signing with actual params in the request
set -e

BASE_URL="https://baas-pilot.choicebankapi.com"
SENDER_ID="vycein"
PRIVATE_KEY="9x9euCAYsXxBaw02jzzrBiHGisDVVtf8dMNfhpd17BcZxLK13G56KvjFY3bx69j"
ENDPOINT="staticData/getOperationalAccounts"

REQUEST_ID="CURL-$(date +%s)-$(od -An -N4 -tx1 /dev/urandom | tr -d ' ')"
TIMESTAMP=$(($(date +%s) * 1000))
SALT=$(od -An -N6 -tx1 /dev/urandom | tr -d ' ' | head -c 12)

# Params: type=0 (Foreign Currency), 1=RTGS, 2=EFT
# Flattened: params.type=0 (dot notation for nested)
# Order: locale, params.type, requestId, salt, sender, senderKey, timestamp
STRING_TO_SIGN="locale=en_KE&params.type=0&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"
SIGNATURE=$(echo -n "$STRING_TO_SIGN" | openssl dgst -sha256 -hex | awk '{print $2}')

STRING_TO_SIGN_REDACTED="locale=en_KE&params.type=0&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=***REDACTED***&timestamp=${TIMESTAMP}"

BODY=$(cat <<EOF
{
  "requestId": "${REQUEST_ID}",
  "sender": "${SENDER_ID}",
  "locale": "en_KE",
  "timestamp": ${TIMESTAMP},
  "salt": "${SALT}",
  "signature": "${SIGNATURE}",
  "params": {"type": 0}
}
EOF
)

RESPONSE=$(curl -s -X POST "${BASE_URL}/${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -d "$BODY")

echo "========== FLATTENED STRING (privateKey redacted) =========="
echo "$STRING_TO_SIGN_REDACTED"
echo ""
echo "========== REQUEST =========="
echo "URL: POST ${BASE_URL}/${ENDPOINT}"
echo "Body:"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""
echo "========== RESPONSE =========="
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
