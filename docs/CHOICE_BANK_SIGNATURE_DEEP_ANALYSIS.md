# Choice Bank Signature – Step-by-Step Analysis (Why 12004?)

This doc walks through the **official authentication document** and the **Postman collection script** step by step, then compares our implementation to find why the signature is rejected (12004).

---

## Part 1: Official Doc (GitBook) – Sign the Request

**Source:** https://choice-bank.gitbook.io/choice-bank/getting-started/authentication

The doc gives **5 steps** (numbering is logical, not necessarily execution order):

| Step | Doc text | Meaning |
|------|----------|--------|
| 1 | Remove the `senderKey` field from request JSON object. | Before sending, do not include `senderKey` in the payload. |
| 2 | Hash the converted string which got in step 1 with SHA-256 and the private key then fill the hashed string back to the request JSON with field name - `signature`. | Signature = hash of “the converted string”. “Private key” is used by including it in the payload as `senderKey` when building that string (see step 4). |
| 3 | Convert the key-value pair of the request JSON object into string with **alphabetical sorting**. When converting, make the key-value pair as **string=string** and join them with **&**. | Build one string: sort keys alphabetically, each entry `key=value` (value as string), joined by `&`. Nested objects use **dot notation** (doc example: `params.name=Tester`). |
| 4 | Fill the private key into the request JSON object with `senderKey` field name. | For **signing only**: add `senderKey` to the object, then build the string (step 3) and hash it (step 2). So the string that is hashed **includes** `senderKey=yourkey`. |
| 5 | Fill the salt into the request JSON object with `salt` field name. | Salt must be in the request (and thus in the string we sign). |

**Execution order that matches the doc and Postman:**

1. Have JSON with: requestId, sender, locale, timestamp, params (and salt).
2. Add **senderKey** (private key) to the object (for signing only).
3. **Remove** `signature` from the object (it is not part of what we sign).
4. **Convert** to one string: sort keys alphabetically, format `key=value`, join with `&`. Nested → dot notation; empty object → `params={}`.
5. **Hash** that string with **SHA-256**; output **hex** → put in `signature`.
6. **Remove** `senderKey` from the object.
7. Send the JSON (with `signature`, without `senderKey`).

---

## Part 2: Postman Collection – Exact Script Logic

**Source:** Choice Bank BaaS API Postman collection → **Pre-request script**.

1. **requestId** = `sender` + UUID (with hyphens, lowercase).  
   Example: `VYCEIN` + `f15c903d-23a9-4d5f-b5d7-8f375ccd919b`.

2. **Body** is parsed after variable replace: `pm.variables.replaceIn(pm.request.body.raw)` then `JSON.parse(...)`.  
   So `{{$timestamp}}` is replaced first; in Postman **`$timestamp` is in seconds** (10 digits).

3. **Remove** `signature`: `delete jsonObj['signature'];`

4. **Flatten** (deepTraverse):
   - Primitive → `path + "=" + obj` (value stringified; numbers become string, e.g. `"1650533105687"`).
   - Empty object → `path + "={}"` (e.g. `params={}`).
   - Empty array → `path + "=[]"`.
   - Non-empty object → recurse with `path + "." + key`.
   - Array → `path + "[" + i + "]"` for each element.

5. **Add** `senderKey`: `arr.push('senderKey=' + pm.collectionVariables.get("privateKey"));`

6. **Sort**: `arr.sort()` (JavaScript string sort → alphabetical).

7. **Join**: `arr.join('&')`.

8. **Hash**: `CryptoJS.SHA256(stringToSign).toString()` → **hex** (default for CryptoJS).

So in Postman:

- **Timestamp** in the body is `{{$timestamp}}` → **seconds** (10 digits). So the flattened string has `timestamp=1771954209` (e.g.), not 13 digits.
- **Locale** in the body is `"en_KE"`. So flattened string has `locale=en_KE`.
- **Empty params** → `params={}` in the string.

---

## Part 3: Our Implementation vs Doc + Postman

| Item | Doc / Postman | Our bash script | Our Java |
|------|----------------|-----------------|----------|
| String to sign | Alphabetical `key=value&...` | We build `locale=en_KE&params={}&requestId=...&salt=...&sender=...&senderKey=...&timestamp=...` (alphabetical). | TreeMap → alphabetical. |
| Include senderKey in string | Yes (then remove before send) | Yes | Yes |
| Exclude signature from string | Yes | We never put signature in the string. | We don’t add signature to the flat map. |
| Hash | SHA-256, hex | `openssl dgst -sha256 -hex` | `MessageDigest SHA-256`, then `%02x` |
| Empty params | Doc example has params; Postman uses `params={}`. | `params={}` | `flat.put("params", "{}")` |
| Nested | Dot notation (e.g. params.name) | We use params.type=0 etc. | flattenNested → params.* |
| Locale in body | "en_KE" | "en_KE" | LOCALE = "en_KE" |
| Locale in flat string | Doc **table** shows `locale=en_ke` (lowercase). Body shows "en_KE". | We use **locale=en_KE** (uppercase KE). | We use **en_KE**. |
| Timestamp in body | Doc example 1650533105687 (13 digits). Postman uses `{{$timestamp}}` = **seconds** (10 digits). | We use **milliseconds** (13 digits). | We use **milliseconds** (13 digits). |
| Timestamp in flat string | Same as in body (string representation). | Same 13-digit value. | Same 13-digit value. |
| Leading/trailing space | Not mentioned; support said “no leading space”. | We trim the string. | We trim. |

---

## Part 4: Likely Causes of 12004

### 1. **Timestamp: seconds vs milliseconds (high priority)**

- **Postman** uses `{{$timestamp}}` = **Unix seconds** (10 digits).
- **We** use Unix **milliseconds** (13 digits).
- So our string has `timestamp=1771954209000`, Postman’s has `timestamp=1771954209`.
- If the server uses the **same rule as Postman**, it will build the string with **seconds**. Our signature is then computed over a different string → **12004**.

**Action:** Try using **seconds** in both the request body and the string-to-sign (same value in both places).

### 2. **Locale in flat string: en_KE vs en_ke**

- The GitBook **table** shows the flat string as `locale=**en_ke**` (lowercase).
- The **body** in the doc and Postman uses `"locale": "en_KE"`.
- If the server lowercases the locale **when building the string** (e.g. for canonical form), then we must use `locale=en_ke` in the string we hash, even if the body keeps `"en_KE"`.

**Action:** Try building the string-to-sign with **locale=en_ke** (lowercase) and keep body as `"en_KE"`.

### 3. **Private key / encoding**

- If the key we use (e.g. from env/script) is wrong, or there is encoding/trim/quote difference, the string we sign will differ from the server’s.
- Ensure the key is exactly what Choice Bank gave (no extra spaces, same encoding as Postman).

### 4. **Order of keys**

- We and Postman both sort alphabetically. If the server does **not** sort and instead uses JSON key order, they would get a different string. The doc says “alphabetical sorting”, so this is less likely but worth confirming with Choice Bank.

---

## Part 5: Exact String-to-Sign Format (Reference)

For **getBankCodes** (empty params), the string must look like:

```
locale=en_KE&params={}&requestId=<requestId>&salt=<salt>&sender=<sender>&senderKey=<privateKey>&timestamp=<timestamp>
```

- **No** spaces or newlines; **no** `signature` in the string.
- Keys in **strict alphabetical order**: locale, params, requestId, salt, sender, senderKey, timestamp.
- Values: exact string form (e.g. timestamp as 10 or 13 digits, locale as `en_KE` or `en_ke` depending on server).

Then:

- **Signature** = SHA-256(above string), output as **hex** (lowercase).
- Put that in the request body as `"signature": "<hex>"`.
- Do **not** send `senderKey` in the body.

---

## Part 6: What to Try Next

1. **Switch to seconds for timestamp**  
   In both body and string-to-sign use:  
   `timestamp = current Unix time in seconds` (10 digits).  
   Recompute signature and test getBankCodes again.

2. **Try lowercase locale in string only**  
   Keep body as `"locale": "en_KE"`, but in the string-to-sign use `locale=en_ke`.  
   Recompute signature and test.

3. **Confirm with Choice Bank**  
   Ask them to confirm:
   - Timestamp: **seconds** or **milliseconds** in the string they verify?
   - Locale in the string: **en_KE** or **en_ke**?
   - One sample request (with real requestId, salt, timestamp, and body) and the **exact** string they use to compute the signature (with senderKey redacted).

Once the exact string-to-sign and timestamp/locale rules match the server, the signature will validate and 12004 will go away.
