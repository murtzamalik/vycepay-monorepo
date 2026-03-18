#!/usr/bin/env bash
# Prints two curl commands for getBankCodes (Variant A: ms + en_KE, Variant B: seconds + en_ke).
# Use these in Postman to see which variant the server accepts.
# Env: CHOICE_BANK_SENDER_ID, CHOICE_BANK_PRIVATE_KEY (or source .env)
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
for f in "$SCRIPT_DIR/.env" "$SCRIPT_DIR/../.env"; do
  if [ -f "$f" ]; then
    set -a; source "$f" 2>/dev/null || true; set +a
    break
  fi
done

SENDER_ID="${CHOICE_BANK_SENDER_ID:-VYCEIN}"
PRIVATE_KEY="${CHOICE_BANK_PRIVATE_KEY:-}"
BASE_URL="${CHOICE_BANK_BASE_URL:-https://baas-pilot.choicebankapi.com}"
ENDPOINT="staticData/getBankCodes"
URL="${BASE_URL}/${ENDPOINT}"

if [ -z "$PRIVATE_KEY" ]; then
  echo "WARNING: CHOICE_BANK_PRIVATE_KEY not set. Set it or copy .env into this folder."
  PRIVATE_KEY="YOUR_PRIVATE_KEY_HERE"
fi

REQUEST_ID="${SENDER_ID}$(uuidgen 2>/dev/null | tr 'A-F' 'a-f' || echo "$(date +%s)-$(od -An -N4 -tx1 /dev/urandom | tr -d ' ' | head -c 8)")"
SALT=$(od -An -N6 -tx1 /dev/urandom | tr -d ' ' | head -c 12)

# Variant A: timestamp milliseconds (13 digits), locale en_KE in string
TIMESTAMP_MS=$(($(date +%s) * 1000))
STR_A="locale=en_KE&params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP_MS}"
STR_A=$(printf '%s' "$STR_A" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
SIG_A=$(echo -n "$STR_A" | openssl dgst -sha256 -hex | awk '{print $2}')
BODY_A="{\"requestId\":\"${REQUEST_ID}\",\"sender\":\"${SENDER_ID}\",\"locale\":\"en_KE\",\"timestamp\":${TIMESTAMP_MS},\"salt\":\"${SALT}\",\"signature\":\"${SIG_A}\",\"params\":{}}"

# Variant B: timestamp seconds (10 digits), locale en_ke in string (Postman-style)
TIMESTAMP_SEC=$(date +%s)
STR_B="locale=en_ke&params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP_SEC}"
STR_B=$(printf '%s' "$STR_B" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
SIG_B=$(echo -n "$STR_B" | openssl dgst -sha256 -hex | awk '{print $2}')
BODY_B="{\"requestId\":\"${REQUEST_ID}\",\"sender\":\"${SENDER_ID}\",\"locale\":\"en_KE\",\"timestamp\":${TIMESTAMP_SEC},\"salt\":\"${SALT}\",\"signature\":\"${SIG_B}\",\"params\":{}}"

echo "=============================================="
echo "Variant A: timestamp=milliseconds (13 digits), locale en_KE in string"
echo "=============================================="
printf 'curl -X POST "%s" -H "Content-Type: application/json" -d '\''%s'\''\n' "$URL" "$BODY_A"
echo ""
echo "=============================================="
echo "Variant B: timestamp=seconds (10 digits), locale en_ke in string"
echo "=============================================="
printf 'curl -X POST "%s" -H "Content-Type: application/json" -d '\''%s'\''\n' "$URL" "$BODY_B"
echo ""
echo "Paste each curl into Postman (Import → Raw text). If one returns code 00000, we know the server's expected format."
