# VycePay Mobile — Title-fetch soft-skip (Choice 10000 / 10001)

**Audience:** Android / iOS  
**Purpose:** When Choice Hakikisha (title fetch) is temporarily unavailable, show a bank-style warning, get user consent, then continue send money without a confirmed recipient name.  
**Base URL:** BFF only (never call transaction-service ports directly).

Related:

- [MOBILE_VALIDATE_ACCOUNT_HANDOFF.md](MOBILE_VALIDATE_ACCOUNT_HANDOFF.md) — normal validate + send rules
- [MOBILE_MPESA_MOBILE_PAYBILL_TILL_HANDOFF.md](MOBILE_MPESA_MOBILE_PAYBILL_TILL_HANDOFF.md) — M-Pesa Mobile / Paybill / Till
- [MOBILE_API_CONTRACT.md](MOBILE_API_CONTRACT.md) — transaction API index

---

## 1. When this applies

| Condition | Soft-skip allowed? |
|-----------|-------------------|
| Choice `validateAccount` returns **`10000`** or **`10001`** | Yes (M-Pesa only) |
| `accountType` **1** (Paybill), **2** (Till), or **3** (Mobile) | Yes |
| `accountType` **0 / 4 / 5** (Choice / PesaLink / IMT) | **No** — keep blocking |
| Any other Choice / business error (`ACCOUNT_FROZEN`, `ACCOUNT_RESTRICT_IN`, invalid account, etc.) | **No** — keep blocking |

Soft-skip means: title fetch failed because Choice is **system error / busy**, not because the number is wrong. User must confirm they checked the number.

---

## 2. Detect skippable validate response

Call as today:

- **POST** `/api/v1/transactions/validate-account`

### Soft-unavailable success (`200`)

HTTP **200** (not an error envelope). Treat as **skippable**, not hard failure:

```json
{
  "accountId": "712345678",
  "accountType": 3,
  "accountName": null,
  "freezeStatus": 0,
  "restrictStatus": 0,
  "valid": false,
  "titleFetchUnavailable": true,
  "skipAllowed": true,
  "choiceCode": "10000",
  "message": "The banking service is temporarily unavailable. Please try again shortly."
}
```

| Field | Mobile action |
|-------|----------------|
| `valid` | `false` — do **not** show a confirmed name |
| `titleFetchUnavailable` | `true` — show warning UI |
| `skipAllowed` | `true` — allow Continue after user Okay |
| `choiceCode` | `10000` or `10001` (for logging / support) |
| `message` | Prefer for toast / alert body when present |
| `accountName` | null / absent — **do not invent** a fake name |

### Still hard-fail (unchanged)

- Non-2xx error envelope (`CHOICE_*`, `ACCOUNT_FROZEN`, `ACCOUNT_RESTRICT_IN`, validation 400s, etc.)
- `200` with `valid=true` → normal path (show name)
- Soft fields **absent** / `skipAllowed=false` → do not offer skip

---

## 3. UI (match bank-style UX)

1. User enters number → taps Continue → `POST /validate-account`
2. If soft-unavailable:
   - Show warning, e.g.  
     **“The bank could not confirm a name for this number. Check the number before you continue.”**
   - Optional alert: recipient name hidden temporarily / namecheck unavailable
   - Primary action: **Okay** / **Continue anyway** (user consent)
   - Secondary: Cancel / edit number
3. After consent → amount screen (no “Paying: {name}”, or show “Name not confirmed”)
4. On send → include `skipTitleFetchConfirmed: true`

Do **not**:

- Invent or type a recipient name as source of truth
- Auto-skip without an explicit Okay
- Use soft-skip for PesaLink / bank rails

---

## 4. Send money after consent

- **POST** `/api/v1/transactions/send`
- Headers: `Authorization`, `Idempotency-Key` (new UUID per attempt)

```json
{
  "payeeBankCode": "M-PESA",
  "payeeAccountId": "712345678",
  "accountType": 3,
  "amount": 100,
  "skipTitleFetchConfirmed": true
}
```

| Field | Required | Notes |
|-------|----------|--------|
| Same fields as normal send | Yes | Same `accountType` / account as validate |
| `skipTitleFetchConfirmed` | Yes for soft path | `true` only after user Okay |
| `payeeAccountName` | No | Still ignored / overwritten by server |

### Server behaviour (do not re-implement)

1. Re-calls Choice validate
2. If validate **succeeds** → uses real `accountName` (flag ignored)
3. If still **10000/10001** and flag `true` and type 1–3 → proceeds **without** `payeeAccountName`
4. If still soft-fail and flag `false` → error (`CHOICE_SYSTEM_ERROR` / `CHOICE_SYSTEM_BUSY`)
5. Flag `true` on type 0/4/5 → `TITLE_FETCH_SKIP_NOT_ALLOWED` (400)

OTP / status / history flows are unchanged.

---

## 5. If validate later succeeds

If user retries validate and gets `valid=true` + `accountName`:

- Show normal “Paying: {accountName}”
- Send with `skipTitleFetchConfirmed: false` (or omit; default false)
- Flag `true` is harmless if Choice name is available — server uses the real name

---

## 6. Decision table (quick)

| Validate outcome | UI | Send flag |
|------------------|----|-----------|
| `valid=true` + name | Show name; Continue | `false` / omit |
| Soft: `titleFetchUnavailable` + `skipAllowed` | Warning → Okay | `true` after Okay |
| Soft without Okay | Stay on number / cancel | Do not send |
| Hard error envelope | Show `message`; block | Do not send |

---

## 7. QA checklist

- [ ] Type 3 validate with mocked Choice `10000` → 200 soft fields; warning shown
- [ ] Type 3 validate with `10001` → same; message from Choice preferred
- [ ] User cancels warning → send not called
- [ ] User Okay → send includes `skipTitleFetchConfirmed: true`
- [ ] Send without flag while Choice still soft-fails → error; no silent transfer
- [ ] Type 4 + Choice `10000` → hard error; no soft UI
- [ ] Soft path then Choice recovers → name shown; transfer uses name
- [ ] Paybill (`1`) / Till (`2`) soft path same as mobile for flag + warning
- [ ] OTP still works after soft-skip send
- [ ] Idempotency-Key unique per send attempt

---

## 8. Copy suggestions

**Inline warning**

> The bank could not confirm a name for this number. Check the number before you continue.

**Alert title / body (optional)**

> Namecheck Service Alert  
> Recipient names may be unavailable temporarily. Confirm the number is correct before sending.
