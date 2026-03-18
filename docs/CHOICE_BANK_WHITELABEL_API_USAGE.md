# Using the Choice Bank White-Label Wallet API (HMAC-SHA256)

This describes how to **hit Choice Bank APIs** that use the **HMAC-SHA256** signature (string-to-sign: `METHOD + "\n" + PATH + "\n" + TIMESTAMP + "\n" + BODY`).

**Note:** The **BaaS API** (e.g. getBankCodes, onboarding at `baas-pilot.choicebankapi.com`) uses a **different** signing scheme (flattened key=value + SHA256). For those, use `ChoiceBankClient` / `ChoiceBankRequestFactory`. This guide is for the **white-label wallet** style APIs that expect HMAC-SHA256 with the above string-to-sign.

---

## 1. Configuration

Add to `application.yml` (or `.env` / env vars) in the service that calls Choice Bank:

```yaml
vycepay:
  choice-bank:
    wallet:
      base-url: https://api.sandbox.choicebank.com   # or production URL from Choice Bank
      secret-key: ${CHOICE_BANK_WALLET_SECRET_KEY}   # HMAC secret (never commit)
      signature-hex-encoding: false                  # false = Base64 (default), true = HEX
```

Ensure `CHOICE_BANK_WALLET_SECRET_KEY` is set in the environment (or replace with your secret). The service requires a non-empty secret to sign requests.

---

## 2. Option A – Use the ready-made service (recommended)

Inject `ChoiceBankWalletSignatureService`. It builds the signature and sends the request with the right headers.

### Dependencies

- `vycepay-common` (already has the service).
- Your app must provide `RestTemplate` and `ObjectMapper` beans (e.g. from `WebConfig` in common).

### Example: POST with JSON body

```java
@Service
@RequiredArgsConstructor
public class MyWalletService {

    private final ChoiceBankWalletSignatureService choiceBankWallet;

    public String createWallet(String userId, String currency) {
        String path = "/v1/wallet/create";  // path only, no domain
        Map<String, Object> body = Map.of(
            "userId", userId,
            "currency", currency
        );
        ResponseEntity<String> response = choiceBankWallet.postSignedJson(path, body);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Choice Bank error: " + response.getBody());
        }
        return response.getBody();
    }
}
```

### Example: POST with raw JSON string

If you already have the exact JSON string (e.g. from another layer), use it as-is so the signature matches the bytes sent:

```java
String path = "/v1/wallet/create";
String rawJson = "{\"userId\":\"U123\",\"currency\":\"KES\"}";
ResponseEntity<String> response = choiceBankWallet.postSigned(path, rawJson);
```

### Example: GET (no body)

```java
String path = "/v1/wallet/balance";  // or with query: "/v1/wallet/balance?userId=U123"
ResponseEntity<String> response = choiceBankWallet.getSigned(path);
```

### Headers sent by the service

- **Timestamp:** `X-Choice-Timestamp` – ISO-8601 (e.g. `2026-02-24T10:30:45.123456789Z`).
- **Signature:** `X-Choice-Signature` – Base64 (or HEX if `signature-hex-encoding: true`).

If Choice Bank uses different header names, you can copy the logic from `ChoiceBankWalletSignatureService` and change the header keys.

---

## 3. Option B – Use the utility yourself

Use `ChoiceBankSignatureUtil.generateSignature(...)` and set headers in your own HTTP client (RestTemplate, WebClient, etc.).

### 1) Build the string-to-sign (same as the spec)

- **Path:** path only, no domain.  
  Example: full URL `https://api.sandbox.choicebank.com/v1/wallet/create` → use **`/v1/wallet/create`**.
- **Body:** exact raw JSON string (no pretty-print, no extra spaces). No body → `""`.
- **Timestamp:** ISO-8601, e.g. `Instant.now().toString()`.

### 2) Generate signature and call API

```java
import java.time.Instant;
import com.vycepay.common.choicebank.ChoiceBankSignatureUtil;

String method = "POST";
String path = "/v1/wallet/create";           // no domain
String timestamp = Instant.now().toString();
String body = "{\"userId\":\"U1\",\"currency\":\"KES\"}";
String secretKey = "your-hmac-secret";

String signature = ChoiceBankSignatureUtil.generateSignature(
    method, path, timestamp, body, secretKey);  // Base64

// Add headers and send request
headers.set("X-Choice-Timestamp", timestamp);
headers.set("X-Choice-Signature", signature);
// POST to https://api.sandbox.choicebank.com/v1/wallet/create with body
```

### 3) HEX instead of Base64

```java
String signature = ChoiceBankSignatureUtil.generateSignature(
    method, path, timestamp, body, secretKey, true);  // HEX
```

---

## 4. Rules to avoid 12004 (invalid signature)

1. **Path:** never include domain or query string in the path unless the spec says so (e.g. path only: `/v1/wallet/create`).
2. **Body:** must be the **exact** byte sequence sent in the request (usually compact JSON). Same string used in `generateSignature` and in the HTTP body.
3. **Timestamp:** same value in header and in `generateSignature`.
4. **Newlines:** string-to-sign uses exactly `\n` (the util does this).
5. **Encoding:** UTF-8 for string-to-sign and for the key (the util uses UTF-8).

---

## 5. Summary

| Goal | What to use |
|------|-------------|
| Call white-label wallet APIs (HMAC-SHA256) | Set config → inject `ChoiceBankWalletSignatureService` → `postSigned` / `postSignedJson` / `getSigned`. |
| Custom client (WebClient, etc.) | Use `ChoiceBankSignatureUtil.generateSignature(...)` and set timestamp + signature headers yourself. |
| Call BaaS APIs (getBankCodes, onboarding) | Use existing `ChoiceBankClient` / `ChoiceBankRequestFactory` (different signing). |

If Choice Bank gives you a different base URL, header names, or encoding (e.g. HEX), set `base-url` / `signature-hex-encoding` and, if needed, adjust the header names in `ChoiceBankWalletSignatureService` or your own client.
