# Choice Bank API – CTO Test Pack

This folder contains everything needed to **test and verify** the Choice Bank BaaS API integration (getBankCodes, signature behaviour) on your machine.

---

## 1. What’s the situation?

- **API:** Choice Bank BaaS (sandbox) – e.g. `getBankCodes`.
- **Issue:** We get **12004 – Invalid signature** when calling with our credentials.
- **Goal:** You can run the same request (curl/Postman), see request/response, and verify signature logic locally.

---

## 2. Credentials (use project env)

Use the same credentials as the main project. **Do not commit real keys to git.**

- **Sender ID:** `VYCEIN`
- **Private key:** Copy from project root `.env`:  
  `CHOICE_BANK_PRIVATE_KEY=...`  
  Or set in your shell:  
  `export CHOICE_BANK_PRIVATE_KEY="your-key"`

Optional: copy `.env` into this folder (e.g. `cp ../.env .env`) or source it:  
`source ../.env 2>/dev/null || true`

---

## 3. Quick run (recommended)

From this folder (`choice-bank-cto-test-pack`):

```bash
# Option A: Use env from project root
export CHOICE_BANK_SENDER_ID="${CHOICE_BANK_SENDER_ID:-VYCEIN}"
export CHOICE_BANK_PRIVATE_KEY="<paste from .env>"
./run-getBankCodes.sh

# Option B: Script will prompt if env not set (or use defaults for sender)
./run-getBankCodes.sh
```

This will:

1. Generate a valid signed request (requestId, salt, timestamp, signature).
2. Print the **curl** command and the **request body**.
3. Optionally **call the API** and print the **response** (so you see 12004 or 00000).

---

## 4. What’s in this pack

| File | Purpose |
|------|--------|
| **README.md** | This file – how to run and what to verify. |
| **run-getBankCodes.sh** | Runnable script: builds signature, prints curl + body, can call API. |
| **run-getBankCodes-variants.sh** | Prints two curls (timestamp ms vs seconds, locale en_KE vs en_ke) to try in Postman. |
| **sample-request-getBankCodes.json** | Example request body (placeholders for requestId, timestamp, salt, signature). |
| **sample-response-success.json** | Example of a successful response (00000) for reference. |
| **sample-response-12004.json** | Example of current failure (12004 Invalid signature). |
| **SIGNATURE_REFERENCE.md** | Short reference: how the signature is built and what we’re not sure about. |
| **env.example** | Example env vars (no real keys). |

---

## 5. How to test in Postman

1. Run once:  
   `./run-getBankCodes.sh`  
   Or to try both signature variants:  
   `./run-getBankCodes-variants.sh`
2. Copy the **curl** line from the script output (or the “Body (raw JSON)” block).
3. In Postman: **Import → Raw text** → paste the curl.  
   Or: **New request → Method POST, URL from script, Body raw JSON** → paste the JSON body.
4. Send. You should see either:
   - **code "00000"** – success; or
   - **code "12004"** – invalid signature (current behaviour).

---

## 6. What to verify

- **Request:** Method POST, URL `https://baas-pilot.choicebankapi.com/staticData/getBankCodes`, Body = JSON with `requestId`, `sender`, `locale`, `timestamp`, `salt`, `signature`, `params`.
- **Signature:** Built from a string like:  
  `locale=en_KE&params={}&requestId=...&salt=...&sender=...&senderKey=<private_key>&timestamp=...`  
  (alphabetical order, no spaces, no `signature` in the string). Then SHA-256 in **hex**.
- **Possible causes of 12004:** Timestamp in seconds vs milliseconds; locale in string `en_KE` vs `en_ke`. See **SIGNATURE_REFERENCE.md**.

---

## 7. Project docs (for deeper dive)

In the main repo:

- `docs/CHOICE_BANK_SIGNATURE_DEEP_ANALYSIS.md` – Step-by-step analysis of the doc and why 12004 may occur.
- `docs/CHOICE_BANK_POSTMAN_SIGNING.md` – How the official Postman collection signs.
- `scripts/choice-bank-call.sh` – Main script used for BaaS calls (getBankCodes, etc.).
- `scripts/curl-getBankCodes-postman-variants.sh` – Two variants (timestamp ms vs seconds, locale en_KE vs en_ke) to try in Postman.

---

## 8. Contact

If you need different samples (e.g. getOperationalAccounts, onboarding) or another format, ask the team for an updated pack or run the main project scripts under `scripts/` with the same credentials.
