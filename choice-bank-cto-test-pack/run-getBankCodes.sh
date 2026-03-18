#!/usr/bin/env bash
# Choice Bank getBankCodes – generate signed request and optionally call API.
# Usage: ./run-getBankCodes.sh [--no-call]
#   Default: prints curl + body and calls API. Use --no-call to only print.
# Env: CHOICE_BANK_SENDER_ID (default VYCEIN), CHOICE_BANK_PRIVATE_KEY (required for real call)
set -e

# Load .env from this folder or parent if present
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
for f in "$SCRIPT_DIR/.env" "$SCRIPT_DIR/../.env"; do
  if [ -f "$f" ]; then
    set -a
    source "$f" 2>/dev/null || true
    set +a
    break
  fi
done

BASE_URL="${CHOICE_BANK_BASE_URL:-https://baas-pilot.choicebankapi.com}"
SENDER_ID="${CHOICE_BANK_SENDER_ID:-VYCEIN}"
PRIVATE_KEY="${CHOICE_BANK_PRIVATE_KEY:-}"
ENDPOINT="staticData/getBankCodes"
DO_CALL=true
[ "${1:-}" = "--no-call" ] && DO_CALL=false

if [ -z "$PRIVATE_KEY" ]; then
  echo "WARNING: CHOICE_BANK_PRIVATE_KEY not set. Using a placeholder; API call will fail."
  PRIVATE_KEY="YOUR_PRIVATE_KEY_HERE"
fi

# Build request (same logic as main project)
REQUEST_ID="${SENDER_ID}$(uuidgen 2>/dev/null | tr 'A-F' 'a-f' || echo "$(date +%s)-$(od -An -N4 -tx1 /dev/urandom | tr -d ' ' | head -c 8)")"
TIMESTAMP=$(($(date +%s) * 1000))
SALT=$(od -An -N6 -tx1 /dev/urandom | tr -d ' ' | head -c 12)

STRING_TO_SIGN="locale=en_KE&params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"
STRING_TO_SIGN=$(printf '%s' "$STRING_TO_SIGN" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
SIGNATURE=$(echo -n "$STRING_TO_SIGN" | openssl dgst -sha256 -hex | awk '{print $2}')

BODY="{\"requestId\":\"${REQUEST_ID}\",\"sender\":\"${SENDER_ID}\",\"locale\":\"en_KE\",\"timestamp\":${TIMESTAMP},\"salt\":\"${SALT}\",\"signature\":\"${SIGNATURE}\",\"params\":{}}"
URL="${BASE_URL}/${ENDPOINT}"

# Redacted string for display
FLAT_DISPLAY=$(echo "$STRING_TO_SIGN" | sed "s/senderKey=${PRIVATE_KEY}/senderKey=***REDACTED***/")

echo "=============================================="
echo "Choice Bank getBankCodes – CTO Test Pack"
echo "=============================================="
echo ""
echo "Flattened string used for signature (senderKey redacted):"
echo "$FLAT_DISPLAY"
echo ""
echo "URL: POST $URL"
echo ""
echo "Request Body (raw JSON):"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo ""
echo "=============================================="
echo "cURL (copy to Postman: Import → Raw text)"
echo "=============================================="
printf 'curl -X POST "%s" \\\n  -H "Content-Type: application/json" \\\n  -d '\''%s'\''\n' "$URL" "$BODY"
echo ""

if [ "$DO_CALL" = true ]; then
  echo "=============================================="
  echo "API Response"
  echo "=============================================="
  RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$URL" -H "Content-Type: application/json" -d "$BODY")
  HTTP_CODE=$(echo "$RESPONSE" | grep -o 'HTTP_CODE:[0-9]*' | cut -d: -f2)
  BODY_RESP=$(echo "$RESPONSE" | sed '/HTTP_CODE:/d')
  echo "HTTP Status: $HTTP_CODE"
  echo "$BODY_RESP" | python3 -m json.tool 2>/dev/null || echo "$BODY_RESP"
  echo ""
  if echo "$BODY_RESP" | grep -q '"code":"00000"'; then
    echo "Result: SUCCESS (code 00000)"
  elif echo "$BODY_RESP" | grep -q '"code":"12004"'; then
    echo "Result: INVALID SIGNATURE (code 12004)"
  else
    echo "Result: See response body above"
  fi
else
  echo "Skipping API call (--no-call). Run without --no-call to hit the API."
fi
