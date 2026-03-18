#!/usr/bin/env bash
# Outputs a ready-to-paste curl for getBankCodes (BaaS signing) for Postman.
# Postman: Import → Raw text → paste the curl below.
set -e

BASE_URL="https://baas-pilot.choicebankapi.com"
SENDER_ID="VYCEIN"
PRIVATE_KEY="9x9euCAYsXxBaw02jzzrBiHGisDVVtf8dMNfhpd17BcZxLK13G56KvjFY3bx69j"
ENDPOINT="staticData/getBankCodes"

REQUEST_ID="${SENDER_ID}$(uuidgen 2>/dev/null | tr 'A-F' 'a-f' || echo "$(date +%s)-$(od -An -N4 -tx1 /dev/urandom | tr -d ' ' | head -c 8)")"
TIMESTAMP=$(($(date +%s) * 1000))
SALT=$(od -An -N6 -tx1 /dev/urandom | tr -d ' ' | head -c 12)

STRING_TO_SIGN="locale=en_KE&params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"
STRING_TO_SIGN=$(printf '%s' "$STRING_TO_SIGN" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
SIGNATURE=$(echo -n "$STRING_TO_SIGN" | openssl dgst -sha256 -hex | awk '{print $2}')

BODY="{\"requestId\":\"${REQUEST_ID}\",\"sender\":\"${SENDER_ID}\",\"locale\":\"en_KE\",\"timestamp\":${TIMESTAMP},\"salt\":\"${SALT}\",\"signature\":\"${SIGNATURE}\",\"params\":{}}"
URL="${BASE_URL}/${ENDPOINT}"

echo "=============================================="
echo "getBankCodes – copy below into Postman (Import → Raw text)"
echo "=============================================="
echo ""
printf 'curl -X POST "%s" \\\n  -H "Content-Type: application/json" \\\n  -d '\''%s'\''\n' "$URL" "$BODY"
echo ""
echo "=============================================="
echo "Or set in Postman manually:"
echo "=============================================="
echo "Method: POST"
echo "URL: $URL"
echo "Headers: Content-Type: application/json"
echo "Body (raw JSON):"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""
