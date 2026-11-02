# Mobile Team — Paybill saved payee Acc (`payeeReferenceNumber`)

**Audience:** Android / iOS  
**From:** Backend  
**Priority:** Product bug (Paybill saved payees)  
**Status:** Backend ready — mobile must consume the new field  
**Related:** [MOBILE_BENEFICIARIES_HANDOFF.md](MOBILE_BENEFICIARIES_HANDOFF.md), send field `payeeReferenceNumber` on `POST /api/v1/transactions/send`

---

## 1. What was broken

Saving a **Paybill** beneficiary only stored the **Paybill number**, not the **Acc / account number**.

| Field on Paybill send | Meaning | Before | After (backend) |
|-----------------------|---------|--------|-----------------|
| Paybill number | e.g. `888880` | Saved as `payeeAccountId` ✅ | Same ✅ |
| Acc / account number | e.g. `1234567890` | **Not saved** ❌ | Saved + returned as `payeeReferenceNumber` ✅ |

Picking a saved Paybill payee only pre-filled the Paybill number; the user had to re-type Acc every time.

Mobile could not fix this alone — the beneficiaries API did not accept or return Acc. That API change is now ready on backend (after deploy).

---

## 2. Backend contract (what you must use)

Same field name as send: **`payeeReferenceNumber`**.

### 2.1 Save Paybill beneficiary

`POST /api/v1/transactions/beneficiaries`

```json
{
  "nickname": "KPLC home",
  "accountType": 1,
  "payeeAccountId": "888880",
  "payeeBankCode": "M-PESA",
  "payeeAccountName": "Kenya Power",
  "payeeReferenceNumber": "1234567890"
}
```

| Field | Required for Paybill (`accountType: 1`) |
|-------|----------------------------------------|
| `payeeAccountId` | Yes — Paybill / business number |
| `payeeReferenceNumber` | **Yes** — Acc / account number |
| Other rails (0, 2, 3, 4, …) | Omit or ignore; backend stores null/empty |

If Paybill save is sent **without** Acc → HTTP `4xx`, code **`INVALID_PAYEE_REFERENCE`**.

### 2.2 List / save / rename response

Every beneficiary object now includes:

```json
{
  "externalId": "bne_…",
  "nickname": "KPLC home",
  "accountType": 1,
  "payeeBankCode": "M-PESA",
  "payeeAccountId": "888880",
  "payeeAccountName": "Kenya Power",
  "payeeReferenceNumber": "1234567890"
}
```

| Case | `payeeReferenceNumber` |
|------|------------------------|
| Paybill with Acc | string (e.g. `"1234567890"`) |
| Non-Paybill | `null` |
| Legacy Paybill saved before this change | `null` — user must re-save or type Acc once |

### 2.3 Uniqueness (product note)

Same Paybill + **different** Acc = **separate** saved payees.  
Same Paybill + same Acc = upsert (update nickname / restore).

---

## 3. Mobile work required

### 3.1 After successful Paybill send → “Save beneficiary?”

Include Acc from the send draft / receipt:

- `payeeAccountId` = Paybill number  
- `payeeReferenceNumber` = Acc that was used on send  

Do **not** save Paybill without Acc.

### 3.2 Saved payee → select → send

When user taps a Paybill beneficiary (`accountType == 1`):

1. Prefill Paybill number from `payeeAccountId`
2. Prefill Acc from `payeeReferenceNumber` (if non-null)
3. If `payeeReferenceNumber` is null (legacy), keep asking for Acc in UI
4. Still call `POST /validate-account` before send (unchanged rule)
5. On send, pass both:
   - `payeeAccountId`
   - `payeeReferenceNumber` (**required** for Paybill send — already true today)

### 3.3 Out of scope for this ticket

- Till / Mobile / Choice / PesaLink saved payees — no Acc field  
- Rename / delete APIs — nickname-only; responses just include the new field  
- Changing OTP / validate-account contracts  

---

## 4. Acceptance checklist (mobile)

- [ ] Paybill “Save beneficiary” body includes `payeeReferenceNumber`
- [ ] Blank Acc on Paybill save is blocked in UI (and/or handle `INVALID_PAYEE_REFERENCE`)
- [ ] `GET /beneficiaries` models include `payeeReferenceNumber`
- [ ] Selecting a Paybill saved payee prefills **both** Paybill and Acc
- [ ] Send from that flow still sends `payeeReferenceNumber`
- [ ] Legacy rows with `null` Acc: Acc field empty until user enters / re-saves
- [ ] Non-Paybill flows unchanged

---

## 5. Quick map (select → send)

| UI | API field |
|----|-----------|
| Paybill number | `payeeAccountId` |
| Acc / account number | `payeeReferenceNumber` |
| Rail | `accountType: 1`, `payeeBankCode: "M-PESA"` (as today) |

Full beneficiaries guide: [MOBILE_BENEFICIARIES_HANDOFF.md](MOBILE_BENEFICIARIES_HANDOFF.md).

---

## 6. Contact

Questions on field naming or edge cases: backend.  
Field name is fixed to match send: **`payeeReferenceNumber`**.
