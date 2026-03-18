# Choice Bank – Signing logic from official Postman collection

Choice Bank ne jo Postman collection share ki hai uske **pre-request script** se exact signing logic ye hai.

## Source

File: `Choice Bank BaaS API.postman_collection.json`  
Collection → **Event** → **prerequest** script.

## Steps (script se)

1. **requestId** = `sender` + UUID (with hyphens, lowercase hex).  
   Example: `VYCEINf15c903d-23a9-4d5f-b5d7-8f375ccd919b`.

2. Request body (raw JSON) par **variable replace** karo (`{{requestId}}`, `{{$timestamp}}`, etc.), phir parse karo.  
   **`signature` key delete** karo – signed content mein signature nahi aata.

3. **Flatten** (nested JSON → string for signing):
   - Primitives: `path=value` (e.g. `locale=en_KE`, `sender=VYCEIN`).
   - Empty object: `path={}` (e.g. `params={}`).
   - Empty array: `path=[]`.
   - Nested: dot notation (e.g. `params.type=0`).
   - Arrays: `path[0]=...`, `path[1]=...` (index notation).

4. **senderKey** add karo:  
   `arr.push('senderKey=' + privateKey)`.

5. **Sort** keys alphabetically:  
   `arr.sort()` then `arr.join('&')`.

6. **Signature**:  
   `CryptoJS.SHA256(stringToSign).toString()` → lowercase hex.

## Important details

| Item        | Postman behaviour |
|------------|-------------------|
| **locale** | Body mein `"en_KE"` → flat string mein **locale=en_KE** (uppercase KE). |
| **timestamp** | `{{$timestamp}}` = **seconds** (10 digits) in Postman. API docs/response sometimes 13-digit (ms) – dono try kar sakte ho. |
| **requestId** | **sender + UUID** (hyphens, lowercase), e.g. `VYCEIN` + `f15c903d-23a9-4d5f-b5d7-8f375ccd919b`. |
| **Empty params** | `params={}` flat string mein aata hai. |
| **Hash** | SHA-256 of the flat string; output hex (CryptoJS default). |

## Hamari implementation vs Postman

- **Bash** (`scripts/choice-bank-call.sh`):  
  `locale=en_KE`, `params={}`, requestId = sender + UUID (lowercase), alphabetical order, SHA-256 hex.  
  Timestamp ab **milliseconds** (API compatibility).

- **Java** (`ChoiceBankRequestFactory`, `ChoiceBankSignatureUtil`):  
  Same logic: flatten, sort, add senderKey, SHA-256 hex.

## Postman se test kaise karein

1. Collection import karo: **Choice Bank BaaS API.postman_collection.json**.
2. Collection variables set karo:
   - **sender**: `VYCEIN`
   - **privateKey**: (apna private key)
   - **baseUrl**: `https://baas-pilot.choicebankapi.com` (agar collection mein `baseUrl` use ho raha ho aur preset na ho).
3. **Get Bank Codes** (ya koi bhi request) chalao – pre-request script automatically `requestId` aur `signature` set karega.

Agar **Postman** se same credentials se request success ho (code 00000) aur **humari script/Java** se 12004 aaye, to difference encoding / timestamp unit / exact string format mein ho sakta hai – us case mein Postman ke Console se “String to Sign” log karke compare karo.
