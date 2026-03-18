#!/usr/bin/env bash
# Direct curl call to Choice Bank API - staticData/getBankCodes
# Uses: vyceinkey, private key from .env
set -e

BASE_URL="https://baas-pilot.choicebankapi.com"
SENDER_ID="vycein"
PRIVATE_KEY="9x9euCAYsXxBaw02jzzrBiHGisDVVtf8dMNfhpd17BcZxLK13G56KvjFY3bx69j"

REQUEST_ID="CURL-$(date +%s)-$(od -An -N4 -tx1 /dev/urandom | tr -d ' ')"
TIMESTAMP=$(($(date +%s) * 1000))
SALT=$(od -An -N6 -tx1 /dev/urandom | tr -d ' ' | head -c 12)

# Build string to sign per Choice Bank docs:
# - Flatten JSON: nested keys use dot notation (params.name), empty params as params={}
# - Alphabetical (ASCII) sort, key=value joined with &
# - Include senderKey when signing; remove from final request
# Order: locale, params, requestId, salt, sender, senderKey, timestamp
# Per Choice Bank: params must be in flattened string. Empty params = params={}
# Order: locale, params, requestId, salt, sender, senderKey, timestamp
STRING_TO_SIGN="locale=en_KE&params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"
SIGNATURE=$(echo -n "$STRING_TO_SIGN" | openssl dgst -sha256 -hex | awk '{print $2}')

# Redacted version for sharing with Choice Bank (private key hidden)
STRING_TO_SIGN_REDACTED="locale=en_KE&params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=***REDACTED***&timestamp=${TIMESTAMP}"

BODY=$(cat <<EOF
{
  "requestId": "${REQUEST_ID}",
  "sender": "${SENDER_ID}",
  "locale": "en_KE",
  "timestamp": ${TIMESTAMP},
  "salt": "${SALT}",
  "signature": "${SIGNATURE}",
  "params": {}
}
EOF
)

RESPONSE=$(curl -s -X POST "${BASE_URL}/staticData/getBankCodes" \
  -H "Content-Type: application/json" \
  -d "$BODY")

echo "========== FLATTENED STRING FOR SIGNATURE (privateKey redacted) =========="
echo "$STRING_TO_SIGN_REDACTED"
echo ""
echo "========== REQUEST =========="
echo "URL: POST ${BASE_URL}/staticData/getBankCodes"
echo "Headers: Content-Type: application/json"
echo "Body:"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""
echo "========== RESPONSE =========="
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
