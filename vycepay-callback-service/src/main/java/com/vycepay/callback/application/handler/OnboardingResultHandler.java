package com.vycepay.callback.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.callback.domain.model.ChoiceBankCallback;
import com.vycepay.callback.domain.model.KycVerification;
import com.vycepay.common.security.port.SensitiveDataEncryptionPort;
import com.vycepay.callback.domain.model.Wallet;
import com.vycepay.callback.domain.port.NotificationHandler;
import com.vycepay.callback.infrastructure.persistence.KycVerificationRepository;
import com.vycepay.callback.infrastructure.persistence.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Handles 0001 - Personal Onboarding Result Notification.
 * Updates kyc_verification; creates wallet when status=7 (account opened).
 */
@Component
public class OnboardingResultHandler implements NotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(OnboardingResultHandler.class);
    private static final String NOTIFICATION_TYPE = "0001";
    private static final int STATUS_ACCOUNT_OPENED = 7;

    private final KycVerificationRepository kycRepository;
    private final WalletRepository walletRepository;
    private final ObjectMapper objectMapper;
    private final SensitiveDataEncryptionPort encryptionPort;

    public OnboardingResultHandler(KycVerificationRepository kycRepository,
                                   WalletRepository walletRepository,
                                   ObjectMapper objectMapper,
                                   @Autowired(required = false) SensitiveDataEncryptionPort encryptionPort) {
        this.kycRepository = kycRepository;
        this.walletRepository = walletRepository;
        this.objectMapper = objectMapper;
        this.encryptionPort = encryptionPort;
    }

    @Override
    public String getNotificationType() {
        return NOTIFICATION_TYPE;
    }

    @Override
    public void handle(ChoiceBankCallback callback) {
        Map<String, Object> params = parseParams(callback.getRawPayload());
        if (params == null) return;

        String onboardingRequestId = getString(params, "onboardingRequestId");
        String userId = getString(params, "userId");
        Integer status = getInt(params, "status");
        String accountId = getString(params, "accountId");
        String accountType = getString(params, "accountType");
        String idNumber = getString(params, "idNumber");
        String rejectionReasonIds = getString(params, "rejectionReasonIds");
        String rejectionReasonMsgs = getString(params, "rejectionReasonMsgs");

        kycRepository.findByChoiceOnboardingRequestId(onboardingRequestId).ifPresentOrElse(
                kyc -> {
                    kyc.setStatus(String.valueOf(status));
                    kyc.setChoiceUserId(userId);
                    if (idNumber != null && encryptionPort != null) {
                        kyc.setIdNumber(encryptionPort.encrypt(idNumber));
                    } else if (idNumber != null) {
                        kyc.setIdNumber(idNumber);
                    }
                    kyc.setChoiceAccountId(accountId);
                    kyc.setChoiceAccountType(accountType);
                    kyc.setRejectionReasonIds(rejectionReasonIds);
                    kyc.setRejectionReasonMsgs(rejectionReasonMsgs);
                    kycRepository.save(kyc);

                    if (status != null && status == STATUS_ACCOUNT_OPENED && accountId != null) {
                        createWalletIfNeeded(kyc.getCustomerId(), accountId, accountType);
                    }
                },
                () -> log.warn("KYC not found for onboardingRequestId={}", onboardingRequestId)
        );
    }

    private void createWalletIfNeeded(Long customerId, String choiceAccountId, String accountType) {
        if (walletRepository.findByChoiceAccountId(choiceAccountId).isEmpty()) {
            Wallet wallet = Wallet.builder()
                    .customerId(customerId)
                    .choiceAccountId(choiceAccountId)
                    .choiceAccountType(accountType != null ? accountType : "C002")
                    .balanceCache(BigDecimal.ZERO)
                    .currency("KES")
                    .status("ACTIVE")
                    .build();
            walletRepository.save(wallet);
            log.info("Created wallet for customerId={}, choiceAccountId={}", customerId, choiceAccountId);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseParams(String rawPayload) {
        try {
            Map<String, Object> root = objectMapper.readValue(rawPayload, Map.class);
            return (Map<String, Object>) root.get("params");
        } catch (Exception e) {
            log.error("Failed to parse callback params", e);
            return null;
        }
    }

    private String getString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v != null ? v.toString() : null;
    }

    private Integer getInt(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
