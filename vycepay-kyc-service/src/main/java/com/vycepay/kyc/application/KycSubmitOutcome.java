package com.vycepay.kyc.application;

/**
 * KYC submit outcome: Choice onboarding request id plus customer-facing {@code msg}.
 */
public record KycSubmitOutcome(String onboardingRequestId, String choiceMsg) {
}
