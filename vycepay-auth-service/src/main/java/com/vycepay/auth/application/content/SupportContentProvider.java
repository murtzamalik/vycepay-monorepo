package com.vycepay.auth.application.content;

import com.vycepay.auth.api.v1.dto.support.AboutHighlightDto;
import com.vycepay.auth.api.v1.dto.support.AboutSectionDto;
import com.vycepay.auth.api.v1.dto.support.AboutVycePayResponse;
import com.vycepay.auth.api.v1.dto.support.FaqCategoryDto;
import com.vycepay.auth.api.v1.dto.support.FaqItemDto;
import com.vycepay.auth.api.v1.dto.support.HelpCentreResponse;
import com.vycepay.auth.api.v1.dto.support.SupportContactDto;
import com.vycepay.auth.api.v1.dto.support.SupportLinkDto;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Production-ready in-app support and about content for mobile clients.
 * Copy is aligned with vycepay.com brand positioning (Kenya-first wallet, VyceScore, Choice Bank).
 */
@Service
public class SupportContentProvider {

    private static final String WEBSITE = "https://vycepay.com";
    private static final String SUPPORT_EMAIL = "support@vycepay.com";
    private static final String PRIVACY_URL = "https://vycepay.com/privacy";
    private static final String TERMS_URL = "https://vycepay.com/terms";

    /**
     * Returns structured help centre content for GET /api/v1/auth/support/help-centre.
     */
    public HelpCentreResponse helpCentre() {
        return new HelpCentreResponse(
                "Help & Support",
                "Find answers about your wallet, payments, verification, and security. "
                        + "Our team is here to help you move money with confidence.",
                List.of(
                        new SupportContactDto(
                                "EMAIL",
                                "Email support",
                                SUPPORT_EMAIL,
                                "Monday–Friday, 8:00–18:00 EAT"
                        ),
                        new SupportContactDto(
                                "WEB",
                                "Visit help centre online",
                                WEBSITE + "#questions",
                                "FAQs and pilot information"
                        )
                ),
                List.of(
                        accountCategory(),
                        paymentsCategory(),
                        vyceScoreCategory(),
                        securityCategory()
                ),
                List.of(
                        new SupportLinkDto("Terms of Service", TERMS_URL, "TERMS"),
                        new SupportLinkDto("Privacy Policy", PRIVACY_URL, "PRIVACY"),
                        new SupportLinkDto("VycePay website", WEBSITE, "WEBSITE")
                ),
                "Never share your PIN, OTP, or login codes with anyone — including people claiming to be "
                        + "VycePay staff. Report suspicious activity to " + SUPPORT_EMAIL + " immediately."
        );
    }

    /**
     * Returns structured about content for GET /api/v1/auth/support/about.
     */
    public AboutVycePayResponse aboutVycePay() {
        return new AboutVycePayResponse(
                "VycePay",
                "Turn every transaction into bankable credit.",
                "VycePay is a Kenya-first digital wallet that helps SMEs and everyday users "
                        + "pay, get paid, and build a verifiable financial footprint — with settlement "
                        + "through a regulated banking partner.",
                List.of(
                        new AboutSectionDto(
                                "Who we are",
                                "VycePay is built for how Kenya pays: M-PESA, Airtel Money, Buy Goods, "
                                        + "Paybill, Pesalink, and cross-border flows — in one identity-driven wallet.\n\n"
                                        + "We are onboarding users in controlled pilot batches, starting with the "
                                        + "Mombasa rollout, so every experience stays fast, stable, and secure."
                        ),
                        new AboutSectionDto(
                                "What VyceScore means for you",
                                "VyceScore turns your real payment activity into a credit signal — not a survey "
                                        + "or a guess. Every transfer, deposit, and merchant payment you make through "
                                        + "VycePay feeds a verifiable ledger that can unlock working capital when you qualify.\n\n"
                                        + "Your transaction history belongs to you. It follows you across lenders and "
                                        + "grows as your business or personal activity grows."
                        ),
                        new AboutSectionDto(
                                "Fees that stay transparent",
                                "SME payment processing is capped at 1.5% — well below the fragmented fees many "
                                        + "traders face on informal rails. Person-to-person transfers use a low blended "
                                        + "fee, and small frequent transfers stay free where the product allows.\n\n"
                                        + "We believe pricing should be simple before you tap Pay."
                        ),
                        new AboutSectionDto(
                                "Regulation & data protection",
                                "VycePay operates as CBK-aligned e-money with settlement through Choice Bank, "
                                        + "a regulated banking partner. Customer onboarding uses National ID and "
                                        + "biometric verification.\n\n"
                                        + "Personal data is handled in line with the Kenya Data Protection Act. "
                                        + "We never sell your transaction history."
                        )
                ),
                List.of(
                        new AboutHighlightDto(
                                "shield",
                                "Regulated settlement",
                                "Funds move through Choice Bank with real-time settlement designed for everyday Kenyan rails."
                        ),
                        new AboutHighlightDto(
                                "score",
                                "VyceScore",
                                "Build creditworthiness from the trade you are already doing — automatically."
                        ),
                        new AboutHighlightDto(
                                "wallet",
                                "One wallet, every rail",
                                "M-PESA, Airtel, Till, Paybill, Pesalink, and @username payments in a single app."
                        ),
                        new AboutHighlightDto(
                                "lock",
                                "Security first",
                                "PIN protection, device checks, and OTP verification on sensitive actions."
                        )
                ),
                List.of(
                        new SupportLinkDto("VycePay website", WEBSITE, "WEBSITE"),
                        new SupportLinkDto("Terms of Service", TERMS_URL, "TERMS"),
                        new SupportLinkDto("Privacy Policy", PRIVACY_URL, "PRIVACY"),
                        new SupportLinkDto("Contact support", "mailto:" + SUPPORT_EMAIL, "EMAIL")
                ),
                "Choice Bank",
                "VycePay is not a bank. Wallet balances are held and settled through our licensed "
                        + "banking partner. Deposits, transfers, and statements are subject to partner "
                        + "availability, KYC status, and applicable Kenyan financial regulations.",
                "© " + java.time.Year.now().getValue() + " VycePay. All rights reserved."
        );
    }

    private static FaqCategoryDto accountCategory() {
        return new FaqCategoryDto(
                "account",
                "Account & verification",
                List.of(
                        new FaqItemDto(
                                "join-pilot",
                                "Who can use VycePay?",
                                "VycePay is rolling out in controlled pilot batches. Growth-stage SMEs and "
                                        + "active everyday payers in supported regions can request access. If you "
                                        + "are not onboarded immediately, you may be placed on an early-access waitlist."
                        ),
                        new FaqItemDto(
                                "kyc-required",
                                "Why do I need to verify my identity?",
                                "Regulated wallets must confirm who you are before you can hold balance or move money. "
                                        + "Verification typically uses your National ID and a biometric check. "
                                        + "This protects you and keeps the platform compliant with Kenyan regulations."
                        ),
                        new FaqItemDto(
                                "update-details",
                                "How do I update my phone number or email?",
                                "Open Profile and select the item you want to change. Some updates require OTP "
                                        + "confirmation or partner verification. If you no longer have access to your "
                                        + "registered number, contact " + SUPPORT_EMAIL + " from your registered email."
                        ),
                        new FaqItemDto(
                                "close-account",
                                "How do I close my account?",
                                "Ensure your wallet balance is zero and no pending transfers remain, then email "
                                        + SUPPORT_EMAIL + " with your registered mobile number. We will confirm "
                                        + "identity before closure and explain any retention required by law."
                        )
                )
        );
    }

    private static FaqCategoryDto paymentsCategory() {
        return new FaqCategoryDto(
                "payments",
                "Payments & wallet",
                List.of(
                        new FaqItemDto(
                                "add-money",
                                "How do I add money to my wallet?",
                                "Use Deposit from the home screen and follow the M-PESA STK prompt. Funds are "
                                        + "credited after the payment rail confirms success. Timing can vary slightly "
                                        + "by network conditions."
                        ),
                        new FaqItemDto(
                                "send-money",
                                "How long do transfers take?",
                                "Most transfers settle in seconds once confirmed. If an OTP step is required, "
                                        + "complete it promptly. Failed transfers are not debited; check Activity "
                                        + "for the latest status."
                        ),
                        new FaqItemDto(
                                "failed-payment",
                                "My payment failed — was I charged?",
                                "A failed transfer should not leave your wallet debited. If Activity shows "
                                        + "PENDING for longer than expected, pull to refresh or contact support with "
                                        + "the transaction reference."
                        ),
                        new FaqItemDto(
                                "statement-download",
                                "How do I download an account statement?",
                                "Go to Profile → Statement download, choose your period and format (PDF or Excel), "
                                        + "then tap Apply. Statement generation can take a few minutes. You will be "
                                        + "notified when the file is ready to download."
                        ),
                        new FaqItemDto(
                                "fees",
                                "What fees does VycePay charge?",
                                "Merchant processing is capped at 1.5% for SMEs. P2P transfers use a low blended fee "
                                        + "and small transfers may be free. Any fee shown on the review screen is final "
                                        + "before you confirm."
                        )
                )
        );
    }

    private static FaqCategoryDto vyceScoreCategory() {
        return new FaqCategoryDto(
                "vycescore",
                "VyceScore & credit",
                List.of(
                        new FaqItemDto(
                                "what-is-vycescore",
                                "What is VyceScore?",
                                "VyceScore is your activity-based credit signal. It reflects payment frequency, "
                                        + "volume, repayment behaviour, and ecosystem engagement — built automatically "
                                        + "from VycePay transactions, not from manual forms."
                        ),
                        new FaqItemDto(
                                "improve-score",
                                "How can I improve my VyceScore?",
                                "Use VycePay consistently for legitimate business or personal payments, maintain "
                                        + "steady inflows, and repay any micro-advances on time. Sudden irregular "
                                        + "patterns may delay credit offers."
                        ),
                        new FaqItemDto(
                                "loan-offers",
                                "When will I see a credit offer?",
                                "Offers appear when your score and partner underwriting rules qualify you. "
                                        + "Pilot availability varies by batch and product. An offer in the app is "
                                        + "indicative until you accept terms shown at disbursement."
                        )
                )
        );
    }

    private static FaqCategoryDto securityCategory() {
        return new FaqCategoryDto(
                "security",
                "Security & privacy",
                List.of(
                        new FaqItemDto(
                                "protect-account",
                                "How do I keep my account safe?",
                                "Use a strong device PIN, enable biometrics where available, and never share OTP "
                                        + "codes. VycePay will never ask for your PIN or password by phone, SMS, or email."
                        ),
                        new FaqItemDto(
                                "lost-phone",
                                "I lost my phone — what should I do?",
                                "Sign in on a new device if possible and change your PIN immediately. Email "
                                        + SUPPORT_EMAIL + " so we can review active sessions and flag suspicious activity."
                        ),
                        new FaqItemDto(
                                "data-privacy",
                                "How is my data used?",
                                "We use your data to operate the wallet, prevent fraud, meet regulatory obligations, "
                                        + "and — with your activity — compute VyceScore. Read our Privacy Policy at "
                                        + PRIVACY_URL + " for full details and your rights under Kenyan law."
                        ),
                        new FaqItemDto(
                                "report-fraud",
                                "How do I report fraud or unauthorized activity?",
                                "Contact " + SUPPORT_EMAIL + " immediately with your registered number, the "
                                        + "transaction reference, and what happened. We may temporarily restrict "
                                        + "the account while we investigate."
                        )
                )
        );
    }
}
