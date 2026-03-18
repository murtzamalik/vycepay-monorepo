#!/usr/bin/env bash
# getBankCodes - one curl for Postman (copy the output into Postman Import > Raw text).
# Signing: locale=en_ke (lowercase in flat string per Choice Bank doc).
set -e
BASE_URL="https://baas-pilot.choicebankapi.com"
SENDER_ID="VYCEIN"
PRIVATE_KEY="9x9euCAYsXxBaw02jzzrBiHGisDVVtf8dMNfhpd17BcZxLK13G56KvjFY3bx69j"
REQUEST_ID="VYCEIN$(uuidgen 2>/dev/null | tr -d '-' || echo "$(date +%s)-$(od -An -N4 -tx1 /dev/urandom | tr -d ' ' | head -c 8)")"
TIMESTAMP=$(($(date +%s) * 1000))
SALT=$(od -An -N6 -tx1 /dev/urandom | tr -d ' ' | head -c 12)
# String to sign: locale=en_ke (lowercase per doc), no leading/trailing space
STRING_TO_SIGN="locale=en_ke&params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"
SIGNATURE=$(echo -n "$STRING_TO_SIGN" | openssl dgst -sha256 -hex | awk '{print $2}')
BODY="{\"requestId\":\"${REQUEST_ID}\",\"sender\":\"${SENDER_ID}\",\"locale\":\"en_KE\",\"timestamp\":${TIMESTAMP},\"salt\":\"${SALT}\",\"signature\":\"${SIGNATURE}\",\"params\":{}}"
echo "=== Postman: Method POST, URL below, Body raw JSON ==="
echo "URL: ${BASE_URL}/staticData/getBankCodes"
echo ""
echo "Body (raw JSON):"
echo "$BODY"
echo ""
echo "=== Curl (paste in terminal or Postman Import > Raw text) ==="
# Single-quote the URL and -d so the JSON body is passed as-is
printf '%s' "$BODY" > /tmp/vycepay-body.json
echo "curl -X POST \"${BASE_URL}/staticData/getBankCodes\" -H \"Content-Type: application/json\" -d @/tmp/vycepay-body.json"
echo ""
echo "Or inline (if your shell supports):"
echo "curl -X POST \"${BASE_URL}/staticData/getBankCodes\" -H \"Content-Type: application/json\" -d '$BODY'"
