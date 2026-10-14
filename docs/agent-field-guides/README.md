# VycePay Field Agent Guides (PDF)

Market / field agents ke liye **ek PDF per use case**. Self-explanatory SOPs with numbered steps, checklists, and standards.

## Location

`docs/agent-field-guides/`

| PDF | Doc ID | Use case |
|-----|--------|----------|
| `00_VycePay_Agent_Guide_Index.pdf` | VP-AGENT-00 | Index + golden standards |
| `01_App_Install_and_Registration.pdf` | VP-AGENT-01 | App install & signup OTP |
| `02_KYC_and_Wallet_Opening.pdf` | VP-AGENT-02 | KYC & wallet opening |
| `03_Login_Username_PIN.pdf` | VP-AGENT-03 | Login (username/mobile + PIN) |
| `04_Deposit_MPesa_STK.pdf` | VP-AGENT-04 | M-PESA deposit (STK) |
| `05_Send_Money_MPesa_Mobile.pdf` | VP-AGENT-05 | Send to M-PESA mobile |
| `06_Pay_MPesa_Paybill.pdf` | VP-AGENT-06 | Paybill |
| `07_Buy_Goods_MPesa_Till.pdf` | VP-AGENT-07 | Buy Goods (Till) |
| `08_Send_Other_Bank_PesaLink.pdf` | VP-AGENT-08 | Other bank (PesaLink) |
| `09_Balance_and_Transaction_History.pdf` | VP-AGENT-09 | Balance & history |
| `10_Forgot_Reset_PIN.pdf` | VP-AGENT-10 | Forgot / reset PIN |
| `11_Agent_Dos_Donts_Escalation.pdf` | VP-AGENT-11 | Dos/don'ts + escalation |

## Regenerate

```bash
python3 docs/agent-field-guides/generate_agent_pdfs.py
```

Requires: `reportlab` (`pip3 install reportlab`).

## New customer visit order

01 → 02 → 03 → 04 → 05/06 → 09 (see Index PDF).
