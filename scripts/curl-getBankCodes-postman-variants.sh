#!/usr/bin/env bash
# getBankCodes – two variants for Postman to try (see CHOICE_BANK_SIGNATURE_DEEP_ANALYSIS.md).
# Variant A: current (timestamp ms, locale en_KE in string)
# Variant B: Postman-style (timestamp SECONDS, locale en_ke in string – doc table + Postman $timestamp)
set -e

BASE_URL="https://baas-pilot.choicebankapi.com"
SENDER_ID="VYCEIN"
PRIVATE_KEY="9x9euCAYsXxBaw02jzzrBiHGisDVVtf8dMNfhpd17BcZxLK13G56KvjFY3bx69j"
ENDPOINT="staticData/getBankCodes"

REQUEST_ID="${SENDER_ID}$(uuidgen 2>/dev/null | tr 'A-F' 'a-f' || echo "$(date +%s)-$(od -An -N4 -tx1 /dev/urandom | tr -d ' ' | head -c 8)")"
SALT=$(od -An -N6 -tx1 /dev/urandom | tr -d ' ' | head -c 12)

# Variant A: milliseconds (13 digits), locale en_KE in string
TIMESTAMP_MS=$(($(date +%s) * 1000))
STR_A="locale=en_KE&params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP_MS}"
STR_A=$(printf '%s' "$STR_A" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
SIG_A=$(echo -n "$STR_A" | openssl dgst -sha256 -hex | awk '{print $2}')
BODY_A="{\"requestId\":\"${REQUEST_ID}\",\"sender\":\"${SENDER_ID}\",\"locale\":\"en_KE\",\"timestamp\":${TIMESTAMP_MS},\"salt\":\"${SALT}\",\"signature\":\"${SIG_A}\",\"params\":{}}"

# Variant B: seconds (10 digits, like Postman $timestamp), locale en_ke in string (doc table)
TIMESTAMP_SEC=$(date +%s)
STR_B="locale=en_ke&params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP_SEC}"
STR_B=$(printf '%s' "$STR_B" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
SIG_B=$(echo -n "$STR_B" | openssl dgst -sha256 -hex | awk '{print $2}')
BODY_B="{\"requestId\":\"${REQUEST_ID}\",\"sender\":\"${SENDER_ID}\",\"locale\":\"en_KE\",\"timestamp\":${TIMESTAMP_SEC},\"salt\":\"${SALT}\",\"signature\":\"${SIG_B}\",\"params\":{}}"

URL="${BASE_URL}/${ENDPOINT}"

echo "=============================================="
echo "Variant A: timestamp MS (13 digits), locale en_KE in string"
echo "=============================================="
echo "String to sign (redacted): $(echo "$STR_A" | sed "s/senderKey=${PRIVATE_KEY}/senderKey=***/")"
echo ""
printf 'curl -X POST "%s" -H "Content-Type: application/json" -d '\''%s'\''\n' "$URL" "$BODY_A"
echo ""
echo "=============================================="
echo "Variant B: timestamp SECONDS (10 digits), locale en_ke in string (Postman-style)"
echo "=============================================="
echo "String to sign (redacted): $(echo "$STR_B" | sed "s/senderKey=${PRIVATE_KEY}/senderKey=***/")"
echo ""
printf 'curl -X POST "%s" -H "Content-Type: application/json" -d '\''%s'\''\n' "$URL" "$BODY_B"
echo ""
