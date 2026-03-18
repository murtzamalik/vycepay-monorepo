# Signature Reference – BaaS getBankCodes

Short reference for how we build the signature and what might cause 12004.

---

## How we build the signature

1. **Payload fields:** `requestId`, `sender`, `locale`, `timestamp`, `salt`, `params` (no `signature`, no `senderKey` in body).
2. **For signing only:** Add `senderKey` = private key to the set of key-value pairs.
3. **Flatten to one string:** Sort keys **alphabetically**, format `key=value`, join with `&`.  
   Empty `params` → use `params={}`.  
   No spaces, no newlines. **No leading/trailing space.**
4. **Hash:** SHA-256 of that string, output as **hex** (lowercase).
5. **Send:** Put the hex in body as `"signature"`. **Do not send** `senderKey` in the request.

**Example string-to-sign (getBankCodes):**

```
locale=en_KE&params={}&requestId=VYCEINf15c903d-23a9-4d5f-b5d7-8f375ccd919b&salt=a1b2c3d4e5f6&sender=VYCEIN&senderKey=<private_key>&timestamp=1738740000000
```

---

## What might cause 12004

| Suspect | Our current behaviour | Alternative to try |
|--------|------------------------|--------------------|
| **Timestamp** | 13 digits (milliseconds) | 10 digits (seconds), e.g. `timestamp=$(date +%s)` |
| **Locale in string** | `locale=en_KE` | `locale=en_ke` (lowercase) in string only; body can stay `"en_KE"` |
| **Leading/trailing space** | We trim the string | Already handled |
| **Key/encoding** | Same as Postman/env | Confirm key is exactly as provided by Choice Bank |

---

## Trying variants in Postman

From project root you can generate two curl variants (timestamp ms vs seconds, locale en_KE vs en_ke):

```bash
./scripts/curl-getBankCodes-postman-variants.sh
```

Paste each curl into Postman and see which (if any) returns `00000`.

---

## Official docs

- Auth: https://choice-bank.gitbook.io/choice-bank/getting-started/authentication  
- Full analysis in repo: `docs/CHOICE_BANK_SIGNATURE_DEEP_ANALYSIS.md`
