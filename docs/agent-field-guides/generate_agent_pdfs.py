#!/usr/bin/env python3
"""Generate VycePay field-agent SOP PDFs (one per use case / flow).

Run once:
  python3 docs/agent-field-guides/generate_agent_pdfs.py

No production data is read. Output is static PDF SOPs for market agents.
"""

from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    SimpleDocTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
    KeepTogether,
    HRFlowable,
)

OUT_DIR = Path(__file__).resolve().parent

BRAND = colors.HexColor("#0B3D5C")
ACCENT = colors.HexColor("#1A7A4C")
WARN = colors.HexColor("#B45309")
LIGHT = colors.HexColor("#F0F4F8")
BORDER = colors.HexColor("#D0D7DE")
RED = colors.HexColor("#B91C1C")


def styles():
    base = getSampleStyleSheet()
    return {
        "cover_title": ParagraphStyle(
            "cover_title",
            parent=base["Title"],
            fontSize=22,
            textColor=BRAND,
            alignment=TA_CENTER,
            spaceAfter=6,
            leading=26,
            fontName="Helvetica-Bold",
        ),
        "cover_sub": ParagraphStyle(
            "cover_sub",
            parent=base["Normal"],
            fontSize=12,
            textColor=colors.HexColor("#334155"),
            alignment=TA_CENTER,
            spaceAfter=4,
            leading=16,
        ),
        "badge": ParagraphStyle(
            "badge",
            parent=base["Normal"],
            fontSize=10,
            textColor=ACCENT,
            alignment=TA_CENTER,
            fontName="Helvetica-Bold",
            spaceAfter=12,
        ),
        "h1": ParagraphStyle(
            "h1",
            parent=base["Heading1"],
            fontSize=14,
            textColor=BRAND,
            spaceBefore=12,
            spaceAfter=6,
            fontName="Helvetica-Bold",
            leading=18,
        ),
        "body": ParagraphStyle(
            "body",
            parent=base["Normal"],
            fontSize=10,
            leading=14,
            textColor=colors.HexColor("#1E293B"),
            alignment=TA_JUSTIFY,
            spaceAfter=4,
        ),
        "step": ParagraphStyle(
            "step",
            parent=base["Normal"],
            fontSize=10,
            leading=14,
            textColor=colors.HexColor("#1E293B"),
            spaceAfter=3,
            leftIndent=4,
        ),
        "note": ParagraphStyle(
            "note",
            parent=base["Normal"],
            fontSize=9,
            leading=12,
            textColor=colors.HexColor("#334155"),
            spaceAfter=3,
        ),
        "table_cell": ParagraphStyle(
            "table_cell",
            parent=base["Normal"],
            fontSize=9,
            leading=12,
            textColor=colors.HexColor("#1E293B"),
        ),
        "table_head": ParagraphStyle(
            "table_head",
            parent=base["Normal"],
            fontSize=9,
            leading=12,
            textColor=colors.white,
            fontName="Helvetica-Bold",
        ),
        "check": ParagraphStyle(
            "check",
            parent=base["Normal"],
            fontSize=10,
            leading=13,
            textColor=colors.HexColor("#1E293B"),
            spaceAfter=2,
        ),
    }


def header_footer(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(BRAND)
    canvas.setLineWidth(1.5)
    canvas.line(18 * mm, A4[1] - 12 * mm, A4[0] - 18 * mm, A4[1] - 12 * mm)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#64748B"))
    canvas.drawString(18 * mm, A4[1] - 10 * mm, "VycePay — Field Agent SOP")
    canvas.drawRightString(A4[0] - 18 * mm, A4[1] - 10 * mm, "Confidential — Agent Use Only")
    canvas.line(18 * mm, 12 * mm, A4[0] - 18 * mm, 12 * mm)
    canvas.drawCentredString(A4[0] / 2, 7 * mm, f"Page {doc.page}")
    canvas.restoreState()


def callout(title, lines, s, fill=LIGHT, border=BRAND):
    data = [[Paragraph(f"<b>{title}</b>", s["note"])]]
    for line in lines:
        data.append([Paragraph(line, s["note"])])
    t = Table(data, colWidths=[170 * mm])
    t.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), fill),
                ("BOX", (0, 0), (-1, -1), 0.8, border),
                ("LEFTPADDING", (0, 0), (-1, -1), 8),
                ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
            ]
        )
    )
    return t


def info_table(rows, s, col_widths=None):
    head = [
        Paragraph(rows[0][0], s["table_head"]),
        Paragraph(rows[0][1], s["table_head"]),
    ]
    body = [
        [Paragraph(a, s["table_cell"]), Paragraph(b, s["table_cell"])]
        for a, b in rows[1:]
    ]
    data = [head] + body
    widths = col_widths or [50 * mm, 120 * mm]
    t = Table(data, colWidths=widths)
    t.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), BRAND),
                ("GRID", (0, 0), (-1, -1), 0.5, BORDER),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 6),
                ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, LIGHT]),
            ]
        )
    )
    return t


def steps_block(steps, s):
    items = []
    for i, text in enumerate(steps, 1):
        items.append(
            KeepTogether(
                [
                    Paragraph(f"<b>Step {i}.</b> {text}", s["step"]),
                    Spacer(1, 2),
                ]
            )
        )
    return items


def checklist(items, s):
    return [Paragraph(f"☐  {item}", s["check"]) for item in items]


def build_pdf(filename, meta, sections):
    path = OUT_DIR / filename
    doc = SimpleDocTemplate(
        str(path),
        pagesize=A4,
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=18 * mm,
        bottomMargin=18 * mm,
        title=meta["title"],
        author="VycePay",
    )
    s = styles()
    story = []

    story.append(Spacer(1, 8 * mm))
    story.append(Paragraph("VycePay", s["cover_title"]))
    story.append(Paragraph(meta["title"], s["cover_sub"]))
    story.append(
        Paragraph(
            meta.get("subtitle", "Field Agent Standard Operating Procedure"),
            s["cover_sub"],
        )
    )
    story.append(
        Paragraph(
            f"Document ID: {meta['doc_id']}  |  Version {meta.get('version', '1.0')}",
            s["badge"],
        )
    )
    story.append(HRFlowable(width="100%", thickness=1.2, color=BRAND, spaceAfter=8))

    story.append(
        info_table(
            [
                ["Field", "Value"],
                ["Audience", "Market / Field Agents helping customers use VycePay"],
                ["Purpose", meta["purpose"]],
                ["When to use", meta["when"]],
                ["Estimated time", meta.get("time", "5–15 minutes")],
                ["Language", "English (show customer the app screens)"],
            ],
            s,
        )
    )
    story.append(Spacer(1, 6))

    for section in sections:
        kind = section[0]
        if kind == "h1":
            story.append(Paragraph(section[1], s["h1"]))
        elif kind == "p":
            story.append(Paragraph(section[1], s["body"]))
        elif kind == "steps":
            story.extend(steps_block(section[1], s))
        elif kind == "callout":
            story.append(callout(section[1], section[2], s, fill=section[3], border=section[4]))
            story.append(Spacer(1, 4))
        elif kind == "table":
            story.append(info_table(section[1], s, section[2] if len(section) > 2 else None))
            story.append(Spacer(1, 4))
        elif kind == "checklist":
            story.extend(checklist(section[1], s))
            story.append(Spacer(1, 4))

    doc.build(story, onFirstPage=header_footer, onLaterPages=header_footer)
    print(f"Wrote {path.name}")
    return path


DOCS = []


def add(filename, meta, sections):
    DOCS.append((filename, meta, sections))


add(
    "00_VycePay_Agent_Guide_Index.pdf",
    {
        "doc_id": "VP-AGENT-00",
        "title": "Field Agent Guide — Index & Standards",
        "purpose": "Overview of all agent SOPs and the rules every agent must follow.",
        "when": "Read this first before using any other guide. Re-read weekly.",
        "time": "10 minutes (read)",
    },
    [
        ("h1", "1. What is VycePay?"),
        (
            "p",
            "VycePay is a digital wallet app. Customers register with their mobile number, "
            "complete KYC (identity check), open a wallet, add money via M-PESA, and send "
            "payments (M-PESA mobile, Paybill, Till, or other banks).",
        ),
        ("h1", "2. Your role as a Field Agent"),
        (
            "p",
            "You help customers in the market: install the app, register, complete KYC, "
            "fund the wallet, and complete their first successful transaction. You do "
            "<b>not</b> take customer PINs, OTPs, or ID photos home. You guide; the customer taps.",
        ),
        ("h1", "3. Guide pack (one PDF per flow)"),
        (
            "table",
            [
                ["Doc ID", "Use case / flow"],
                ["VP-AGENT-01", "App install & account registration (mobile OTP)"],
                ["VP-AGENT-02", "KYC & wallet opening"],
                ["VP-AGENT-03", "Login (username / mobile + PIN)"],
                ["VP-AGENT-04", "Deposit money (M-PESA STK Push)"],
                ["VP-AGENT-05", "Send money to M-PESA mobile number"],
                ["VP-AGENT-06", "Pay M-PESA Paybill"],
                ["VP-AGENT-07", "Buy Goods (M-PESA Till)"],
                ["VP-AGENT-08", "Send to other bank (PesaLink)"],
                ["VP-AGENT-09", "Check balance & transaction history"],
                ["VP-AGENT-10", "Forgot / reset PIN"],
                ["VP-AGENT-11", "Agent dos & don'ts + escalation"],
            ],
        ),
        ("h1", "4. Golden standards (always)"),
        (
            "steps",
            [
                "Customer holds their own phone. Agent only guides.",
                "Never ask for or type the customer's 4-digit PIN or SMS OTP.",
                "Never take photos of national ID / passport for yourself; only help them capture inside the app.",
                "Confirm the name shown after account validation before any send.",
                "Wait for success confirmation on screen before leaving the customer.",
                "If something fails, note the on-screen message and escalate (see Guide 11).",
            ],
        ),
        (
            "callout",
            "Compliance reminder",
            [
                "VycePay does not hold cash for the customer. Money moves through Choice Bank / M-PESA. "
                "Do not collect cash as “agent float” unless your company has a separate, written cash policy.",
            ],
            colors.HexColor("#FEF3C7"),
            WARN,
        ),
        ("h1", "5. Recommended visit order (new customer)"),
        (
            "steps",
            [
                "Guide 01 — Install & register",
                "Guide 02 — KYC & wait for wallet",
                "Guide 03 — Confirm login works",
                "Guide 04 — Small M-PESA deposit (test amount)",
                "Guide 05 or 06 — One small send / Paybill (demo success)",
                "Guide 09 — Show balance & history",
                "Leave Guide 10 / 11 contacts with the customer",
            ],
        ),
    ],
)

add(
    "01_App_Install_and_Registration.pdf",
    {
        "doc_id": "VP-AGENT-01",
        "title": "Use Case: App Install & Account Registration",
        "purpose": "Get the customer onto VycePay with a verified mobile number and first session.",
        "when": "New customer who has never used VycePay.",
        "time": "5–10 minutes",
    },
    [
        ("h1", "1. Goal"),
        (
            "p",
            "Customer installs the app, enters mobile number, verifies SMS OTP, and reaches the "
            "next onboarding screen (KYC). At this stage the wallet is <b>not</b> open yet.",
        ),
        ("h1", "2. Prerequisites"),
        (
            "checklist",
            [
                "Smartphone with internet (Android / iOS as supported)",
                "Customer's own Kenya mobile number that can receive SMS",
                "Customer available to enter OTP themselves",
            ],
        ),
        ("h1", "3. Standard steps"),
        (
            "steps",
            [
                "Open Play Store / App Store on the <b>customer's phone</b> and install <b>VycePay</b> (official listing only).",
                "Open the app. On Register / Sign up, enter country code <b>254</b> and the mobile number (e.g. 712345678).",
                "Tap Send OTP / Continue. Confirm SMS arrives on that same phone.",
                "Customer enters the OTP. Do not take the phone to read the OTP yourself.",
                "If the app asks for device permission / notifications, allow as needed for the product.",
                "On success, customer is registered and logged in for this session. Proceed to <b>Guide 02 — KYC</b>.",
            ],
        ),
        ("h1", "4. What success looks like"),
        (
            "table",
            [
                ["Check", "Expected result"],
                ["OTP", "Accepted; no error message"],
                ["Next screen", "KYC / profile / home asking to complete identity"],
                ["Wrong number", "Customer can restart with correct number"],
            ],
        ),
        (
            "callout",
            "Common issues",
            [
                "• No SMS: wait 60 seconds → Resend OTP once → check network / correct number.",
                "• Number already registered: use Login (Guide 03) or Forgot PIN (Guide 10), not a second signup.",
                "• App crash: reinstall official app; retry once; escalate if persistent.",
            ],
            colors.HexColor("#FEF3C7"),
            WARN,
        ),
        ("h1", "5. Agent checklist before leaving this step"),
        (
            "checklist",
            [
                "App installed from official store",
                "OTP verified by customer",
                "Customer ready for KYC (Guide 02)",
            ],
        ),
    ],
)

add(
    "02_KYC_and_Wallet_Opening.pdf",
    {
        "doc_id": "VP-AGENT-02",
        "title": "Use Case: KYC & Wallet Opening",
        "purpose": "Complete identity onboarding so Choice Bank opens the customer wallet.",
        "when": "After successful registration (Guide 01). Wallet does not exist until KYC is approved.",
        "time": "10–20 minutes (+ waiting for approval)",
    },
    [
        ("h1", "1. Goal"),
        (
            "p",
            "Submit accurate KYC details, set <b>username + 4-digit PIN</b>, confirm KYC OTP, "
            "and wait until the wallet appears (account opened). Only then can the customer deposit or send money.",
        ),
        ("h1", "2. Prerequisites"),
        (
            "checklist",
            [
                "Customer logged in after registration",
                "Valid government ID ready (as required by the app / Choice Bank)",
                "Customer chooses a username and a private 4-digit PIN (agent must not know the PIN)",
                "Working SMS for KYC OTP",
            ],
        ),
        ("h1", "3. Standard steps"),
        (
            "steps",
            [
                "Open KYC / Complete profile in the app.",
                "Enter personal details exactly as on the ID (names, ID number, dates). No shortcuts or nicknames.",
                "Capture or upload ID photos <b>inside the app</b> if prompted. Good light, no blur, full document visible.",
                "Customer creates <b>username</b> and <b>4-digit PIN</b> when the app asks (this is for VycePay login). "
                "Agent turns away while PIN is entered.",
                "Submit KYC. Wait for confirmation that the request was submitted.",
                "When prompted, send / confirm <b>KYC OTP</b> from SMS. Customer enters the code.",
                "Explain: wallet opening can take a short time (bank processing). Stay on the network.",
                "Refresh / open Wallet or Home until balance / wallet screen loads successfully. "
                "If still waiting after the agreed SLA, escalate with customer mobile and time of submit.",
            ],
        ),
        ("h1", "4. Status meanings (agent language)"),
        (
            "table",
            [
                ["What customer sees", "What it means"],
                ["KYC submitted / pending", "Bank is reviewing; wallet not ready"],
                ["OTP required", "Complete SMS OTP first"],
                ["Rejected / more info", "Fix data or documents; resubmit as app allows"],
                ["Wallet / balance visible", "Account opened — proceed to deposit"],
            ],
        ),
        (
            "callout",
            "Critical rules",
            [
                "• Never invent or change ID numbers to “make it pass”.",
                "• Never reuse another person's ID.",
                "• PIN and username belong only to the customer.",
                "• Do not leave until you have either a visible wallet OR a clear rejection message to escalate.",
            ],
            colors.HexColor("#FEE2E2"),
            RED,
        ),
        ("h1", "5. Agent checklist"),
        (
            "checklist",
            [
                "Details match ID",
                "KYC OTP confirmed by customer",
                "Username set; PIN known only to customer",
                "Wallet visible OR escalation logged",
            ],
        ),
    ],
)

add(
    "03_Login_Username_PIN.pdf",
    {
        "doc_id": "VP-AGENT-03",
        "title": "Use Case: Login (Username / Mobile + PIN)",
        "purpose": "Help customer sign in securely on their device.",
        "when": "Returning customer, or after KYC credentials were set. Also after new phone / device change.",
        "time": "2–5 minutes",
    },
    [
        ("h1", "1. Goal"),
        (
            "p",
            "Customer logs in with <b>username or mobile</b> plus <b>4-digit PIN</b>. "
            "VycePay allows <b>one device at a time</b>. A new device needs an SMS device OTP, then login again.",
        ),
        ("h1", "2. Standard steps — normal login"),
        (
            "steps",
            [
                "Open VycePay → Login.",
                "Enter username <b>or</b> registered mobile number.",
                "Customer enters 4-digit PIN (agent does not watch).",
                "On success, Home / Wallet appears.",
            ],
        ),
        ("h1", "3. Standard steps — new or different phone"),
        (
            "steps",
            [
                "Attempt login as above. App may say device verification is required.",
                "Customer requests / receives <b>device OTP</b> by SMS and enters it.",
                "After device bind, return to <b>Login</b> and sign in again with PIN (no auto-login after device OTP).",
                "Remind customer: old phone session is replaced; only this phone stays linked.",
            ],
        ),
        (
            "callout",
            "Security",
            [
                "• After 5 wrong PINs the account can lock for a period — wait and use Forgot PIN (Guide 10) if needed.",
                "• Never reset PIN by guessing; use official Forgot PIN flow.",
            ],
            colors.HexColor("#FEF3C7"),
            WARN,
        ),
        ("h1", "4. Agent checklist"),
        (
            "checklist",
            [
                "Customer can open Home after login",
                "If new device: device OTP done + second login succeeded",
                "PIN never spoken aloud in a public place",
            ],
        ),
    ],
)

add(
    "04_Deposit_MPesa_STK.pdf",
    {
        "doc_id": "VP-AGENT-04",
        "title": "Use Case: Deposit Money (M-PESA STK Push)",
        "purpose": "Add money from M-PESA into the VycePay wallet.",
        "when": "Wallet is open (Guide 02 done). Customer has M-PESA balance.",
        "time": "3–8 minutes",
    },
    [
        ("h1", "1. Goal"),
        (
            "p",
            "Customer deposits a chosen amount into their VycePay wallet via M-PESA STK Push, "
            "approves on the phone, and sees updated balance.",
        ),
        ("h1", "2. Prerequisites"),
        (
            "checklist",
            [
                "Wallet visible in app",
                "M-PESA PIN known to customer",
                "Enough M-PESA float for amount + any M-PESA charges",
            ],
        ),
        ("h1", "3. Standard steps"),
        (
            "steps",
            [
                "Open VycePay → Deposit / Add money / M-PESA deposit.",
                "Confirm the mobile number to charge (customer may use registered or allowed MSISDN as per app).",
                "Enter amount in KES. For first demo, use a small test amount.",
                "Confirm / Submit. An M-PESA STK prompt should appear on the phone.",
                "Customer enters <b>M-PESA PIN</b> on the STK prompt (agent looks away).",
                "Wait on the VycePay screen for success / pending update.",
                "Refresh wallet balance (pull to refresh if available) until new balance shows.",
                "Show customer the matching transaction in History if listed.",
            ],
        ),
        ("h1", "4. Success criteria"),
        (
            "table",
            [
                ["Check", "Expected"],
                ["STK", "Appeared and was approved"],
                ["App status", "Success or completed (not failed)"],
                ["Balance", "Increased by deposit (minus any fees if shown)"],
            ],
        ),
        (
            "callout",
            "If STK does not appear",
            [
                "• Confirm phone has network and M-PESA is active.",
                "• Retry once with same or slightly different amount only if app allows a new deposit.",
                "• Do not ask customer to send money to your personal Till/Paybill.",
                "• Escalate with time, amount, and mobile number.",
            ],
            colors.HexColor("#FEF3C7"),
            WARN,
        ),
        ("h1", "5. Agent checklist"),
        (
            "checklist",
            [
                "Deposit completed or clear failure message captured",
                "Balance refreshed and shown to customer",
                "Customer understands M-PESA PIN ≠ VycePay PIN",
            ],
        ),
    ],
)

add(
    "05_Send_Money_MPesa_Mobile.pdf",
    {
        "doc_id": "VP-AGENT-05",
        "title": "Use Case: Send Money to M-PESA Mobile",
        "purpose": "Transfer from VycePay wallet to another person's M-PESA mobile number.",
        "when": "Wallet funded; customer wants to pay a mobile number.",
        "time": "5–10 minutes",
    },
    [
        ("h1", "1. Goal"),
        (
            "p",
            "Validate the recipient number, confirm the displayed name, send the amount, "
            "complete any payment OTP, and confirm success.",
        ),
        ("h1", "2. Standard steps"),
        (
            "steps",
            [
                "Open Send Money → M-PESA Mobile (or equivalent).",
                "Enter recipient mobile number carefully (e.g. 712345678).",
                "Tap Continue / Validate. App must show the <b>registered name</b> (title fetch).",
                "Customer confirms the name is correct. If wrong number, stop and re-enter.",
                "Enter amount ≤ available wallet balance. Optional remark / note.",
                "Confirm Send. If payment OTP is required, customer enters SMS OTP.",
                "Wait for success screen. Do not resubmit the same payment unless app clearly failed.",
                "Optionally save recipient as beneficiary if customer wants.",
                "Show updated balance and transaction in history.",
            ],
        ),
        (
            "callout",
            "Mandatory standard",
            [
                "Always validate and read the name aloud to the customer before sending. "
                "Never send based only on a number written on paper without name confirmation.",
            ],
            colors.HexColor("#FEE2E2"),
            RED,
        ),
        ("h1", "3. Agent checklist"),
        (
            "checklist",
            [
                "Name confirmed by customer",
                "Amount confirmed",
                "OTP entered only by customer",
                "Success shown before leaving",
            ],
        ),
    ],
)

add(
    "06_Pay_MPesa_Paybill.pdf",
    {
        "doc_id": "VP-AGENT-06",
        "title": "Use Case: Pay M-PESA Paybill",
        "purpose": "Pay a business Paybill (e.g. utilities, schools, merchants) from the wallet.",
        "when": "Customer has Paybill number and account / reference number.",
        "time": "5–10 minutes",
    },
    [
        ("h1", "1. Goal"),
        (
            "p",
            "Pay the correct Paybill with the correct account/reference number so the merchant credits the right bill.",
        ),
        ("h1", "2. Information you must collect from customer"),
        (
            "table",
            [
                ["Field", "Example / notes"],
                ["Paybill (business) number", "From bill / merchant"],
                ["Account / reference number", "Required — e.g. meter no., student ID, invoice"],
                ["Amount (KES)", "Exact amount on bill if possible"],
            ],
        ),
        ("h1", "3. Standard steps"),
        (
            "steps",
            [
                "Open Paybill / Pay Bill in the app.",
                "Enter Paybill number → Validate / Continue.",
                "Confirm business name shown matches what customer expects.",
                "Enter <b>Account / reference number</b> in the dedicated field (not only in remark).",
                "Enter amount and optional remark.",
                "Send → complete payment OTP if asked.",
                "Show success and advise customer to keep the reference / screenshot for the merchant.",
            ],
        ),
        (
            "callout",
            "Common mistake",
            [
                "Putting the account number only in “remark” can cause failed or mis-posted payments. "
                "Use the Account Number / Reference field the app provides.",
            ],
            colors.HexColor("#FEF3C7"),
            WARN,
        ),
        ("h1", "4. Agent checklist"),
        (
            "checklist",
            [
                "Paybill name confirmed",
                "Account/reference entered in correct field",
                "Success confirmed",
            ],
        ),
    ],
)

add(
    "07_Buy_Goods_MPesa_Till.pdf",
    {
        "doc_id": "VP-AGENT-07",
        "title": "Use Case: Buy Goods (M-PESA Till)",
        "purpose": "Pay a merchant Till number from the VycePay wallet.",
        "when": "Customer is paying a shop / merchant that shows a Till number.",
        "time": "3–8 minutes",
    },
    [
        ("h1", "1. Goal"),
        ("p", "Complete a Buy Goods payment to the correct Till and confirm success with the merchant."),
        ("h1", "2. Standard steps"),
        (
            "steps",
            [
                "Open Buy Goods / Till in the app.",
                "Enter Till number from the merchant display.",
                "Validate and confirm the merchant name with the customer / cashier.",
                "Enter amount exactly as agreed at the counter.",
                "Send → complete OTP if required.",
                "Show success screen to cashier if needed; keep notification/history as proof.",
            ],
        ),
        (
            "callout",
            "Tip",
            [
                "If name does not match the shop sign, stop and re-check the Till number before paying.",
            ],
            colors.HexColor("#FEF3C7"),
            WARN,
        ),
        ("h1", "3. Agent checklist"),
        (
            "checklist",
            [
                "Till name confirmed",
                "Amount confirmed with merchant",
                "Payment success shown",
            ],
        ),
    ],
)

add(
    "08_Send_Other_Bank_PesaLink.pdf",
    {
        "doc_id": "VP-AGENT-08",
        "title": "Use Case: Send to Other Bank (PesaLink)",
        "purpose": "Transfer from VycePay wallet to an account at another bank.",
        "when": "Customer has bank name + account number for an external bank.",
        "time": "5–12 minutes",
    },
    [
        ("h1", "1. Goal"),
        (
            "p",
            "Validate the external bank account, confirm the account name, and complete the transfer.",
        ),
        ("h1", "2. Information required"),
        (
            "checklist",
            [
                "Correct bank selected from the in-app bank list",
                "Account number (as provided by recipient)",
                "Amount in KES",
            ],
        ),
        ("h1", "3. Standard steps"),
        (
            "steps",
            [
                "Open Send Money → Other bank / Bank transfer / PesaLink.",
                "Select the recipient's bank from the list (do not guess codes).",
                "Enter account number → Validate.",
                "Confirm displayed account name with the customer.",
                "Enter amount ≤ available balance; optional remark.",
                "Send → complete OTP if required.",
                "Wait for success; show history entry.",
            ],
        ),
        (
            "callout",
            "Important",
            [
                "Choice / VycePay internal accounts are not the same as “other bank”. "
                "If paying another VycePay / Choice wallet, use the in-app option for that rail, not PesaLink.",
            ],
            colors.HexColor("#FEF3C7"),
            WARN,
        ),
        ("h1", "4. Agent checklist"),
        (
            "checklist",
            [
                "Bank + account validated",
                "Name confirmed",
                "Success confirmed",
            ],
        ),
    ],
)

add(
    "09_Balance_and_Transaction_History.pdf",
    {
        "doc_id": "VP-AGENT-09",
        "title": "Use Case: Check Balance & Transaction History",
        "purpose": "Teach the customer how to verify money and past payments.",
        "when": "After any deposit or send; also at end of every agent visit.",
        "time": "2–5 minutes",
    },
    [
        ("h1", "1. Goal"),
        (
            "p",
            "Customer can independently check wallet balance and find a past transaction.",
        ),
        ("h1", "2. Standard steps — balance"),
        (
            "steps",
            [
                "Open Home / Wallet.",
                "Show available balance in KES.",
                "If balance looks old, use Refresh / pull-to-refresh, then re-check.",
            ],
        ),
        ("h1", "3. Standard steps — history"),
        (
            "steps",
            [
                "Open Transactions / History.",
                "Find recent deposit or transfer by amount and time.",
                "Open detail: status (success / pending / failed), reference if shown.",
                "Explain: pending may update after a short wait; failed means money should not have left (verify balance).",
            ],
        ),
        ("h1", "4. Teach-back (customer must answer)"),
        (
            "checklist",
            [
                "Customer can open balance alone",
                "Customer can find last transaction alone",
                "Customer knows who to call if status stays pending too long",
            ],
        ),
    ],
)

add(
    "10_Forgot_Reset_PIN.pdf",
    {
        "doc_id": "VP-AGENT-10",
        "title": "Use Case: Forgot / Reset PIN",
        "purpose": "Help customer reset VycePay login PIN safely via SMS OTP.",
        "when": "Customer forgot PIN, or locked out after wrong attempts.",
        "time": "3–7 minutes",
    },
    [
        ("h1", "1. Goal"),
        (
            "p",
            "Reset the 4-digit VycePay PIN using official Forgot PIN + SMS OTP. "
            "This is not the M-PESA PIN.",
        ),
        ("h1", "2. Standard steps"),
        (
            "steps",
            [
                "On Login screen, tap Forgot PIN.",
                "Enter registered mobile number.",
                "Request OTP; customer enters SMS code.",
                "Customer sets a new 4-digit PIN (agent looks away).",
                "Return to Login and sign in with the new PIN.",
                "Confirm Home opens.",
            ],
        ),
        (
            "callout",
            "Security standard",
            [
                "• Agent must never choose or store the customer's new PIN.",
                "• Prefer a private place; avoid saying the PIN out loud.",
                "• If OTP never arrives, escalate — do not use a different person's SIM.",
            ],
            colors.HexColor("#FEE2E2"),
            RED,
        ),
        ("h1", "3. Agent checklist"),
        (
            "checklist",
            [
                "OTP verified by customer",
                "New PIN set privately",
                "Login success with new PIN",
            ],
        ),
    ],
)

add(
    "11_Agent_Dos_Donts_Escalation.pdf",
    {
        "doc_id": "VP-AGENT-11",
        "title": "Agent Standards: Dos, Don'ts & Escalation",
        "purpose": "Behaviour, compliance, and how to escalate problems.",
        "when": "Every visit. Mandatory read for all agents.",
        "time": "10 minutes (read) + use as needed",
    },
    [
        ("h1", "1. Always do"),
        (
            "steps",
            [
                "Use only the official VycePay app from the official store.",
                "Let the customer hold the phone and enter secrets (OTP, PIN, M-PESA PIN).",
                "Validate recipient name before every send.",
                "Use small demo amounts for first-time training when possible.",
                "Capture the exact on-screen error message if something fails.",
                "End every visit by showing balance + last transaction (Guide 09).",
            ],
        ),
        ("h1", "2. Never do"),
        (
            "steps",
            [
                "Never ask for PIN or OTP “so I can finish for you”.",
                "Never register using your own number for the customer's wallet.",
                "Never use fake / borrowed ID for KYC.",
                "Never send money to your personal account as a “test” without written approval.",
                "Never promise instant bank approval times you cannot control.",
                "Never install APKs from WhatsApp / unknown links.",
            ],
        ),
        ("h1", "3. Escalation — what to record"),
        (
            "table",
            [
                ["Item", "Example"],
                ["Customer mobile", "2547XXXXXXXX"],
                ["Time (local)", "2026-09-30 14:05"],
                ["Flow", "Deposit / Paybill / KYC / Login"],
                ["Amount (if any)", "KES 100"],
                ["On-screen message", "Copy exact text"],
                ["What you already tried", "Resend OTP once, refresh balance"],
            ],
        ),
        (
            "callout",
            "Escalation rule",
            [
                "One clear retry is allowed. Do not spam Send / Deposit. "
                "Then escalate with the table above to your supervisor / support channel.",
            ],
            colors.HexColor("#FEF3C7"),
            WARN,
        ),
        ("h1", "4. Visit completion checklist"),
        (
            "checklist",
            [
                "Customer can log in",
                "Wallet open (or KYC status explained)",
                "At least one successful money movement if funding was planned",
                "Customer knows Forgot PIN and support path",
                "No PIN/OTP written in your notes",
            ],
        ),
    ],
)


def main():
    for filename, meta, sections in DOCS:
        build_pdf(filename, meta, sections)
    print(f"\nDone. {len(DOCS)} PDFs in {OUT_DIR}")


if __name__ == "__main__":
    main()
