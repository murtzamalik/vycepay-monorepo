package com.vycepay.callback.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.callback.application.push.CallbackPushPublisher;
import com.vycepay.callback.domain.model.ChoiceBankCallback;
import com.vycepay.callback.domain.port.NotificationHandler;
import com.vycepay.callback.infrastructure.persistence.KycVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles 0024 - Profile / document check result (e.g. Document Verified).
 * Resolves customer via onboardingRequestId and sends KYC_DOCUMENT_CHECK push.
 */
@Component
public class ProfileCheckResultHandler implements NotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(ProfileCheckResultHandler.class);
    private static final String NOTIFICATION_TYPE = "0024";

    private final KycVerificationRepository kycRepository;
    private final ObjectMapper objectMapper;
    private final CallbackPushPublisher pushPublisher;

    public ProfileCheckResultHandler(KycVerificationRepository kycRepository,
                                     ObjectMapper objectMapper,
                                     CallbackPushPublisher pushPublisher) {
        this.kycRepository = kycRepository;
        this.objectMapper = objectMapper;
        this.pushPublisher = pushPublisher;
    }

    @Override
    public String getNotificationType() {
        return NOTIFICATION_TYPE;
    }

    @Override
    public void handle(ChoiceBankCallback callback) {
        Map<String, Object> params = parseParams(callback.getRawPayload());
        if (params == null) {
            return;
        }
        String onboardingRequestId = getString(params, "onboardingRequestId");
        if (onboardingRequestId == null || onboardingRequestId.isBlank()) {
            log.warn("Profile check callback missing onboardingRequestId");
            return;
        }
        kycRepository.findByChoiceOnboardingRequestId(onboardingRequestId).ifPresentOrElse(
                kyc -> {
                    log.info("Profile check result for onboardingRequestId={} resultCode={}",
                            onboardingRequestId, getString(params, "resultCode"));
                    pushPublisher.publishBestEffort(kyc.getCustomerId(), NOTIFICATION_TYPE, params);
                },
                () -> log.warn("KYC not found for onboardingRequestId={}", onboardingRequestId)
        );
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
}
