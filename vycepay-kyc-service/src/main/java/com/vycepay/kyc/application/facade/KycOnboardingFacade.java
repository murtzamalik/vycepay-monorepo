package com.vycepay.kyc.application.facade;

import java.time.LocalDate;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.errors.ChoiceBankResponseAssessor;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.security.port.SensitiveDataEncryptionPort;
import com.vycepay.kyc.application.dto.KycProfileCommand;
import com.vycepay.kyc.domain.model.Customer;
import com.vycepay.kyc.domain.model.KycVerification;
import com.vycepay.kyc.infrastructure.persistence.CustomerRepository;
import com.vycepay.kyc.infrastructure.persistence.KycVerificationRepository;

/**
 * Orchestrates Choice Bank easy onboarding: submit, send OTP, confirm OTP.
 * Persists customer and KYC profile locally before forwarding to Choice Bank.
 * Result arrives via callback 0001; status can be polled via getOnboardingStatus.
 */
@Service
@ConditionalOnBean(BankingProviderPort.class)
public class KycOnboardingFacade {

    private static final Logger log = LoggerFactory.getLogger(KycOnboardingFacade.class);
    private static final String STATUS_SUBMITTED = "1";
    private static final String PATH_SUBMIT_EASY_ONBOARDING = "onboarding/v3/submitEasyOnboardingRequest";
    private static final String PATH_SEND_OTP = "common/sendOtp";
    private static final String PATH_RESEND_OTP = "common/resendOtp";
    private static final String PATH_CONFIRM_OPERATION = "common/confirmOperation";
    private static final String PATH_GET_ONBOARDING_STATUS = "onboarding/getOnboardingStatus";

    private final BankingProviderPort bankingProvider;
    private final ChoiceBankResponseAssessor choiceAssessor;
    private final CustomerRepository customerRepository;
    private final KycVerificationRepository kycRepository;
    private final SensitiveDataEncryptionPort encryptionPort;

    public KycOnboardingFacade(BankingProviderPort bankingProvider,
                               ChoiceBankResponseAssessor choiceAssessor,
                               CustomerRepository customerRepository,
                               KycVerificationRepository kycRepository,
                               @Autowired(required = false) SensitiveDataEncryptionPort encryptionPort) {
        this.bankingProvider = bankingProvider;
        this.choiceAssessor = choiceAssessor;
        this.customerRepository = customerRepository;
        this.kycRepository = kycRepository;
        this.encryptionPort = encryptionPort;
    }

    /**
     * Submits easy onboarding request to Choice Bank and persists profile locally.
     *
     * @param customerId Our customer ID
     * @param userId     External ID (Choice userId)
     * @param params     Onboarding params per Choice API
     * @param profile    Captured KYC profile for admin display and audit
     * @return Choice onboardingRequestId if success
     */
    @Transactional
    public String submitOnboarding(Long customerId, String userId, Map<String, Object> params, KycProfileCommand profile) {
        if (profile != null) {
            syncCustomerProfile(customerId, profile);
        }

        ChoiceBankResponse response = bankingProvider.post(PATH_SUBMIT_EASY_ONBOARDING, params);
        choiceAssessor.requireSuccess(response, PATH_SUBMIT_EASY_ONBOARDING);
        String onboardingRequestId = extractOnboardingRequestId(response.getData());
        if (onboardingRequestId == null) {
            throw new BusinessException("INVALID_RESPONSE", "No onboardingRequestId in response", HttpStatus.BAD_GATEWAY);
        }

        KycVerification kyc = new KycVerification();
        kyc.setCustomerId(customerId);
        kyc.setChoiceOnboardingRequestId(onboardingRequestId);
        kyc.setStatus(STATUS_SUBMITTED);
        if (profile != null) {
            applyKycProfile(kyc, profile);
        }
        kycRepository.save(kyc);
        log.info("Onboarding submitted: customerId={} onboardingRequestId={}", customerId, onboardingRequestId);
        return onboardingRequestId;
    }

    private void syncCustomerProfile(Long customerId, KycProfileCommand profile) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        if (hasText(profile.firstName())) {
            customer.setFirstName(profile.firstName().trim());
        }
        if (hasText(profile.lastName())) {
            customer.setLastName(profile.lastName().trim());
        }
        if (hasText(profile.email())) {
            customer.setEmail(profile.email().trim());
        }
        customerRepository.save(customer);
    }

    private void applyKycProfile(KycVerification kyc, KycProfileCommand profile) {
        kyc.setIdType(hasText(profile.idType()) ? profile.idType().trim() : "101");
        if (hasText(profile.idNumber())) {
            String idNumber = profile.idNumber().trim();
            kyc.setIdNumber(encryptionPort != null ? encryptionPort.encrypt(idNumber) : idNumber);
        }
        if (hasText(profile.middleName())) {
            kyc.setMiddleName(profile.middleName().trim());
        }
        if (hasText(profile.birthday())) {
            try {
                kyc.setBirthday(LocalDate.parse(profile.birthday().trim()));
            } catch (Exception e) {
                log.warn("Invalid birthday format for customerId={}: {}", kyc.getCustomerId(), profile.birthday());
            }
        }
        if (profile.gender() != null) {
            kyc.setGender(normalizeGender(profile.gender()));
        }
        if (hasText(profile.address())) {
            kyc.setAddress(profile.address().trim());
        }
        if (hasText(profile.kraPin())) {
            kyc.setKraPin(profile.kraPin().trim());
        }
    }

    private int normalizeGender(Integer gender) {
        if (gender == null || gender == 1) {
            return 1;
        }
        return 0;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Sends OTP via Choice Bank for onboarding or transaction.
     *
     * @param businessId Onboarding request ID or transaction ID
     * @param otpType    SMS or EMAIL
     */
    public void sendOtp(String businessId, String otpType) {
        Map<String, Object> params = Map.of("businessId", businessId, "otpType", otpType);
        choiceAssessor.requireSuccess(bankingProvider.post(PATH_SEND_OTP, params), PATH_SEND_OTP);
    }

    /**
     * Resends OTP via Choice Bank (common/resendOtp). Use when initial OTP expired or not received.
     *
     * @param businessId Onboarding request ID or transaction ID
     * @param otpType    SMS or EMAIL
     */
    public void resendOtp(String businessId, String otpType) {
        Map<String, Object> params = Map.of("businessId", businessId, "otpType", otpType);
        choiceAssessor.requireSuccess(bankingProvider.post(PATH_RESEND_OTP, params), PATH_RESEND_OTP);
    }

    /**
     * Confirms OTP via Choice Bank.
     *
     * @param businessId Onboarding request ID or transaction ID
     * @param otpCode    OTP from user
     * @return true when Choice returns success ({@code code=00000})
     */
    public boolean confirmOtp(String businessId, String otpCode) {
        Map<String, Object> params = Map.of("businessId", businessId, "otpCode", otpCode);
        ChoiceBankResponse response = bankingProvider.post(PATH_CONFIRM_OPERATION, params);
        choiceAssessor.requireSuccess(response, PATH_CONFIRM_OPERATION);
        return true;
    }

    /**
     * Polls Choice Bank for onboarding status.
     */
    public Map<String, Object> getOnboardingStatus(String onboardingRequestId) {
        Map<String, Object> params = Map.of("onboardingRequestId", onboardingRequestId);
        ChoiceBankResponse response = bankingProvider.post(PATH_GET_ONBOARDING_STATUS, params);
        choiceAssessor.requireSuccess(response, PATH_GET_ONBOARDING_STATUS);
        if (response.getData() == null) {
            return Map.of();
        }
        if (response.getData() instanceof Map<?, ?> data) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) data;
            return typed;
        }
        throw new BusinessException(
                "CHOICE_INVALID_RESPONSE",
                "Choice Bank returned unexpected data for " + PATH_GET_ONBOARDING_STATUS,
                HttpStatus.BAD_GATEWAY);
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
