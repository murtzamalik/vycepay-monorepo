# Choice Bank Signature Logic – Verification vs Sandbox Docs

**Official source:** [Choice Bank – Authentication](https://choice-bank.gitbook.io/choice-bank/getting-started/authentication)

## Official algorithm (from GitBook)

1. **Remove** `senderKey` from the request JSON (for the *final* payload; it is added only for signing).
2. **Convert** the request JSON to a single string: key-value pairs as `key=value`, **alphabetically sorted (ASCII)**, joined with `&`.
3. **Hash** that string with **SHA-256** and put the **hex** result in the field `signature`.
4. **Add** the private key to the request as `senderKey` (only when building the string to sign).
5. **Remove** `senderKey` from the request before sending.

So the **string that is hashed** must include `senderKey` (and all other fields). After computing the signature, we remove `senderKey` and send the rest.

## Locale in signing string (important)

The [GitBook table](https://choice-bank.gitbook.io/choice-bank/getting-started/authentication) shows the **flat string** as:

`locale=en_ke&params.name=Tester&...`

So the **value** used for `locale` in the string-to-sign is **lowercase `en_ke`**, not `en_KE`. The request body still sends `"locale": "en_KE"`. We use `en_ke` only when building the string that is hashed.

## Nested objects (e.g. `params`)

- Doc example: `params: { "name": "Tester" }`.
- Flattening: use **dot notation** → `params.name=Tester`.
- So in the string: `...&params.name=Tester&...` (keys still in alphabetical order).

## Empty `params`

- GitBook does not show an empty-params example.
- **Choice Bank support (call):** “params empty bhejo but bhejo” → flattened string must still include params, but empty.
- We use: **`params={}`** in the flattened string and `"params": {}` in the JSON body for APIs with no params (e.g. getBankCodes).

## Cross-check: our implementation vs official

| Rule | Official (GitBook) | Our Java | Our Bash script |
|------|--------------------|----------|------------------|
| Sort keys | Alphabetical (ASCII) | `TreeMap` (String::compareTo) | Manual order: locale, params/params.*, requestId, salt, sender, senderKey, timestamp |
| Delimiter | `&` | `&` | `&` |
| Format | `key=value` | `key=value` | `key=value` |
| Nested | Dot notation (e.g. params.name) | `flattenNested` → params.name | params.type=0, params.userId=... |
| Empty params | Not specified | `flat.put("params", "{}")` | `params={}` in STRING_TO_SIGN |
| Hash | SHA-256 | `MessageDigest SHA-256`, UTF-8 bytes | `openssl dgst -sha256 -hex` |
| Signature | Hex | `String.format("%02x", b)` (lowercase hex) | `openssl` hex (lowercase) |
| senderKey | In string to sign, removed before send | Added to flatMap for sign, not in ChoiceBankRequest | In STRING_TO_SIGN, not in BODY |

## Key order (alphabetical) examples

- **getBankCodes (empty params):**  
  `locale=en_ke&params={}&requestId=...&salt=...&sender=...&senderKey=...&timestamp=...`
- **getOperationalAccounts (params.type=0):**  
  `locale=en_ke&params.type=0&requestId=...&salt=...&sender=...&senderKey=...&timestamp=...`

## Code references

- **Java:** `vycepay-common/.../ChoiceBankSignatureUtil.java` (buildStringToSign, sha256Hex, sign), `ChoiceBankRequestFactory.java` (flattenForSigning, params={} when empty).
- **Bash:** `scripts/choice-bank-call.sh` (STRING_TO_SIGN per API, then SHA-256).

## Variants we tested (all returned 12004)

Script `scripts/test-choice-bank-signature-variants.sh` runs getBankCodes with 15 signature variants; **none** returned 00000: SHA256 / HMAC (with or without senderKey in string), key+string, params omitted or empty, locale en_ke vs en_KE, sender lowercased, double SHA256, UTF-16LE, no locale. So we need Choice Bank to confirm the **exact** string format and hash (e.g. sample flat string + expected signature hex).

## Note on GitBook example signature

The GitBook example shows an expected signature for a sample request. Recomputing the same string (with `params.name=Tester`, dot notation) with SHA-256 gives a **different** hash. So the doc’s example signature may be illustrative only. Our implementation follows the **written steps** (alphabetical key=value, &, SHA-256 hex, senderKey in string then removed).

## Possible cause: “with the private key” = HMAC?

Step 4 says: “Hash the converted string … **with SHA-256 and the private key**.” That could mean:

- **A)** SHA256(flat_string) where flat_string includes `senderKey` (what we do), or  
- **B)** HMAC-SHA256(private_key, flat_string) where flat_string may or may not include `senderKey`.

The doc table shows `senderKey=yourKey` inside the flat string, so A is the natural reading. If Choice Bank’s server uses B, our signature would not match. If you get 12004 even with `locale=en_ke`, ask them to confirm: plain SHA256(flat_string) vs HMAC-SHA256(privateKey, flat_string).

## Summary

- Signature logic is **aligned** with the official sandbox auth doc: flatten to alphabetically sorted `key=value&...`, include `senderKey` in that string, **use `locale=en_ke` (lowercase) in the flat string**, SHA-256 hex, then remove `senderKey` from the payload.
- Empty params: we send **`params={}`** in the flattened string and `"params": {}` in the body.
- **Postman:** Run `./scripts/choice-bank-call.sh getBankCodes` or `./scripts/curl-getBankCodes-postman.sh` and use the printed URL + body or curl.
