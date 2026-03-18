# Choice Bank API Signing Verification

## Summary

We have verified our signing implementation against the [Choice Bank Authentication docs](https://choice-bank.gitbook.io/choice-bank/getting-started/authentication). Our implementation matches the documented algorithm, but we receive **"Invalid signature" (12004)** when calling the API. Request sent to Choice Bank support.

## Our Implementation (per Choice Bank docs)

### Signing Algorithm

1. **Build request JSON** with: requestId, sender, locale, timestamp, salt, params
2. **Add senderKey** (private key) to the object for signing
3. **Flatten to string**: Convert key-value pairs to `key=value`, join with `&`, sort alphabetically (ASCII)
4. **Nested objects**: Use dot notation (e.g. `params.name=Tester` for `params: {name: "Tester"}`)
5. **Empty params**: Include params in the string as `params={}` (per Choice Bank: “params empty bhejo but bhejo”)
6. **SHA-256**: Hash the string (UTF-8), use hex encoding for signature
7. **Remove senderKey** from the final request before sending

### Example (empty params - getBankCodes)

**Object for signing:** locale, params, requestId, salt, sender, senderKey, timestamp

**String to sign:**
```
locale=en_KE&params={}&requestId=CURL-xxx&salt=xxx&sender=vycein&senderKey=<private_key>&timestamp=xxx
```

**Keys in ASCII order:** locale, params, requestId, salt, sender, senderKey, timestamp

See **CHOICE_BANK_SIGNATURE_VERIFICATION.md** for full cross-check vs official GitBook.

### Code References

- Java: `ChoiceBankRequestFactory.java`, `ChoiceBankSignatureUtil.java`
- Curl test: `scripts/curl-choice-bank.sh`

## Observed Behavior

| Sender ID   | Result              |
|-------------|---------------------|
| vyceinkey   | 12003 Invalid sender id |
| vycein      | 12004 Invalid signature  |

So `vycein` is a valid sender ID, but the signature is rejected. Possible causes:
1. Private key does not match sender `vycein`
2. Choice Bank expects a different signing format (e.g. empty params handling)
3. Encoding or character handling difference

## Request to Choice Bank

Please confirm:
1. Is the private key `9x9euCAYsXxBaw02jzzrBiHGisDVVtf8dMNfhpd17BcZxLK13G56KvjFY3bx69j` correct for sender `vycein`?
2. For `getBankCodes` with empty params `{}`, what is the exact string-to-sign format? (e.g. should we include `params=` or `params={}`?)
3. Can you provide a working example request/response for getBankCodes with our sender credentials?
