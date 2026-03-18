package com.vycepay.kyc.application.facade;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import com.vycepay.kyc.domain.model.KycVerification;
import com.vycepay.kyc.infrastructure.persistence.CustomerRepository;
import com.vycepay.kyc.infrastructure.persistence.KycVerificationRepository;

/**
 * Orchestrates Choice Bank easy onboarding: submit, send OTP, confirm OTP.
 * Result arrives via callback 0001; status can be polled via getOnboardingStatus.
 */
@Service
@ConditionalOnBean(BankingProviderPort.class)
public class KycOnboardingFacade {

    private static final Logger log = LoggerFactory.getLogger(KycOnboardingFacade.class);
    private static final String STATUS_SUBMITTED = "1";

    private final BankingProviderPort bankingProvider;
    private final CustomerRepository customerRepository;
    private final KycVerificationRepository kycRepository;

    public KycOnboardingFacade(BankingProviderPort bankingProvider,
                               CustomerRepository customerRepository,
                               KycVerificationRepository kycRepository) {
        this.bankingProvider = bankingProvider;
        this.customerRepository = customerRepository;
        this.kycRepository = kycRepository;
    }

    /**
     * Submits easy onboarding request to Choice Bank.
     *
     * @param customerId    Our customer ID
     * @param userId        External ID (Choice userId)
     * @param params        Onboarding params per Choice API
     * @return Choice onboardingRequestId if success
     */
    @Transactional
    public String submitOnboarding(Long customerId, String userId, Map<String, Object> params) {
        ChoiceBankResponse response = bankingProvider.post(
                "onboarding/v3/submitEasyOnboardingRequest", params);
        if (!response.isSuccess()) {
            log.warn("Choice onboarding failed: code={} msg={}", response.getCode(), response.getMsg());
            throw new BusinessException("ONBOARDING_FAILED", "Onboarding failed: " + response.getMsg(), HttpStatus.BAD_GATEWAY);
        }
        String onboardingRequestId = extractOnboardingRequestId(response.getData());
        if (onboardingRequestId == null) {
            throw new BusinessException("INVALID_RESPONSE", "No onboardingRequestId in response", HttpStatus.BAD_GATEWAY);
        }

        KycVerification kyc = new KycVerification();
        kyc.setCustomerId(customerId);
        kyc.setChoiceOnboardingRequestId(onboardingRequestId);
        kyc.setStatus(STATUS_SUBMITTED);
        kycRepository.save(kyc);
        log.info("Onboarding submitted: customerId={} onboardingRequestId={}", customerId, onboardingRequestId);
        return onboardingRequestId;
    }

    /**
     * Sends OTP via Choice Bank for onboarding or transaction.
     *
     * @param businessId Onboarding request ID or transaction ID
     * @param otpType    SMS or EMAIL
     */
    public void sendOtp(String businessId, String otpType) {
        Map<String, Object> params = Map.of("businessId", businessId, "otpType", otpType);
        ChoiceBankResponse response = bankingProvider.post("common/sendOtp", params);
        if (!response.isSuccess()) {
            throw new BusinessException("SEND_OTP_FAILED", "Send OTP failed: " + response.getMsg(), HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Resends OTP via Choice Bank (common/resendOtp). Use when initial OTP expired or not received.
     *
     * @param businessId Onboarding request ID or transaction ID
     * @param otpType    SMS or EMAIL
     */
    public void resendOtp(String businessId, String otpType) {
        Map<String, Object> params = Map.of("businessId", businessId, "otpType", otpType);
        ChoiceBankResponse response = bankingProvider.post("common/resendOtp", params);
        if (!response.isSuccess()) {
            throw new BusinessException("RESEND_OTP_FAILED", "Resend OTP failed: " + response.getMsg(), HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Confirms OTP via Choice Bank.
     *
     * @param businessId Onboarding request ID or transaction ID
     * @param otpCode    OTP from user
     * @return true if confirmation succeeded
     */
    public boolean confirmOtp(String businessId, String otpCode) {
        Map<String, Object> params = Map.of("businessId", businessId, "otpCode", otpCode);
        ChoiceBankResponse response = bankingProvider.post("common/confirmOperation", params);
        return response.isSuccess();
    }

    /**
     * Polls Choice Bank for onboarding status.
     */
    public Map<String, Object> getOnboardingStatus(String onboardingRequestId) {
        Map<String, Object> params = Map.of("onboardingRequestId", onboardingRequestId);
        ChoiceBankResponse response = bankingProvider.post("onboarding/getOnboardingStatus", params);
        if (!response.isSuccess()) {
            throw new BusinessException("GET_STATUS_FAILED", "Get status failed: " + response.getMsg(), HttpStatus.BAD_GATEWAY);
        }
        return response.getData() != null ? (Map<String, Object>) response.getData() : Map.of();
    }

    @SuppressWarnings("unchecked")
    private String extractOnboardingRequestId(Object data) {
        if (data instanceof Map) {
            Object id = ((Map<String, Object>) data).get("onboardingRequestId");
            return id != null ? id.toString() : null;
        }
        return null;
    }
}
