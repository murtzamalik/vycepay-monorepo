#!/usr/bin/env bash
# Test all signature possibilities for getBankCodes; one of these may return 00000.
# Usage: ./scripts/test-choice-bank-signature-variants.sh
set -e

BASE_URL="https://baas-pilot.choicebankapi.com"
ENDPOINT="staticData/getBankCodes"
SENDER_ID="VYCEIN"
PRIVATE_KEY="9x9euCAYsXxBaw02jzzrBiHGisDVVtf8dMNfhpd17BcZxLK13G56KvjFY3bx69j"

# Fixed request for all tests (only signature method changes)
REQUEST_ID="VYCEIN-test-$(date +%s)"
TIMESTAMP=$(($(date +%s) * 1000))
SALT="a1b2c3d4e5f6"

# Flat string WITH senderKey (locale lowercase per doc)
STR_WITH_KEY="locale=en_ke&params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"
# Flat string WITHOUT senderKey
STR_NO_KEY="locale=en_ke&params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&timestamp=${TIMESTAMP}"
# Empty params OMITTED from string (some docs say omit when empty)
STR_NO_PARAMS="locale=en_ke&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"
# All values lowercased (sender=vycein)
STR_LOWER_VALUES="locale=en_ke&params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=vycein&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"
# locale=en_KE (uppercase) in string
STR_LOCALE_KE="locale=en_KE&params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"

sign_sha() { echo -n "$1" | openssl dgst -sha256 -hex | awk '{print $2}'; }
sign_hmac() { echo -n "$1" | openssl dgst -sha256 -hmac "$PRIVATE_KEY" -hex | awk '{print $2}'; }
sign_double_sha() { local h; h=$(echo -n "$1" | openssl dgst -sha256 -hex | awk '{print $2}'); echo -n "$h" | xxd -r -p | openssl dgst -sha256 -hex | awk '{print $2}'; }

run_test() {
  local name="$1"
  local sig="$2"
  local body="{\"requestId\":\"${REQUEST_ID}\",\"sender\":\"${SENDER_ID}\",\"locale\":\"en_KE\",\"timestamp\":${TIMESTAMP},\"salt\":\"${SALT}\",\"signature\":\"${sig}\",\"params\":{}}"
  local code
  code=$(curl -s -X POST "${BASE_URL}/${ENDPOINT}" -H "Content-Type: application/json" -d "$body" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))")
  printf "  %-50s => code=%s\n" "$name" "$code"
  if [ "$code" = "00000" ]; then
    echo ""
    echo "  *** SUCCESS: Use this variant ***"
    echo "  Signature used: $sig"
    echo "  Body (for Postman): $body"
    echo ""
  fi
}

echo "=============================================="
echo "Choice Bank getBankCodes – signature variants"
echo "=============================================="
echo ""

run_test "1. SHA256(string WITH senderKey, locale=en_ke)"                    "$(sign_sha "$STR_WITH_KEY")"
run_test "2. HMAC-SHA256(key, string WITH senderKey)"                        "$(sign_hmac "$STR_WITH_KEY")"
run_test "3. HMAC-SHA256(key, string WITHOUT senderKey)"                    "$(sign_hmac "$STR_NO_KEY")"
run_test "4. SHA256(string WITHOUT senderKey)"                              "$(sign_sha "$STR_NO_KEY")"
run_test "5. SHA256(key + string with senderKey)"                          "$(sign_sha "${PRIVATE_KEY}${STR_WITH_KEY}")"
run_test "6. SHA256(string with senderKey + key)"                           "$(sign_sha "${STR_WITH_KEY}${PRIVATE_KEY}")"
run_test "7. SHA256(string, params OMITTED when empty)"                     "$(sign_sha "$STR_NO_PARAMS")"
run_test "8. HMAC-SHA256(key, string, params omitted)"                      "$(sign_hmac "$STR_NO_PARAMS")"
run_test "9. SHA256(string, all values lowercased sender=vycein)"           "$(sign_sha "$STR_LOWER_VALUES")"
run_test "10. SHA256(string, locale=en_KE uppercase)"                      "$(sign_sha "$STR_LOCALE_KE")"
run_test "11. Double SHA256(string with senderKey)"                         "$(sign_double_sha "$STR_WITH_KEY")"
run_test "12. SHA256(string, params omitted, sender lowercased)"             "$(sign_sha "locale=en_ke&requestId=${REQUEST_ID}&salt=${SALT}&sender=vycein&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}")"
# Doc step order: locale, params, requestId, salt, sender, senderKey, timestamp (already alphabetical)
# 13: params= empty value (no braces)
STR_PARAMS_EMPTY="locale=en_ke&params=&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"
run_test "13. SHA256(string, params= empty no braces)"                      "$(sign_sha "$STR_PARAMS_EMPTY")"
# 14: Key order as in doc example (locale, params.name, requestId, salt, sender, senderKey, timestamp) - same as alpha
# 15: senderKey value different format - try with literal "yourkey" to see if server uses same key for all (would be wrong)
# 16: SHA256 of UTF-16 or Latin1 - try UTF-16LE
sign_sha_utf16() { echo -n "$1" | iconv -f UTF-8 -t UTF-16LE | openssl dgst -sha256 -hex | awk '{print $2}'; }
run_test "14. SHA256(UTF-16LE string with senderKey)"                      "$(sign_sha_utf16 "$STR_WITH_KEY")"
# 15: No locale in string (omit locale)
STR_NO_LOCALE="params={}&requestId=${REQUEST_ID}&salt=${SALT}&sender=${SENDER_ID}&senderKey=${PRIVATE_KEY}&timestamp=${TIMESTAMP}"
run_test "15. SHA256(string without locale key)"                            "$(sign_sha "$STR_NO_LOCALE")"

echo ""
echo "Done. If none show code=00000, we need Choice Bank to confirm exact algorithm."
echo ""
echo "Variants tried: SHA256/HMAC (with/without senderKey), key+string, params omitted,"
echo "  locale en_ke/en_KE, sender lowercased, double SHA256, params= empty, UTF-16LE, no locale."
