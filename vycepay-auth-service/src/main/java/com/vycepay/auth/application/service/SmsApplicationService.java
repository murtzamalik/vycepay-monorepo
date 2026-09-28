package com.vycepay.auth.application.service;

import com.vycepay.auth.domain.model.OtpPurpose;
import com.vycepay.auth.domain.model.SmsDeliveryAttempt;
import com.vycepay.auth.domain.model.SmsMessage;
import com.vycepay.auth.infrastructure.persistence.SmsDeliveryAttemptRepository;
import com.vycepay.auth.infrastructure.persistence.SmsMessageRepository;
import com.vycepay.common.sms.KenyaPhoneNormalizer;
import com.vycepay.common.sms.port.SmsPort;
import com.vycepay.common.sms.port.SmsSendRequest;
import com.vycepay.common.sms.port.SmsSendResult;
import com.vycepay.common.sms.template.SmsTemplateKeys;
import com.vycepay.common.sms.template.SmsTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persists SMS ledger rows and delivers via {@link SmsPort} (soft-fail on provider errors).
 * OTP body from admin {@code sms_template} with code defaults as fallback.
 */
@Service
public class SmsApplicationService implements AuthOtpSmsPort {

    public static final String PURPOSE_AUTH_OTP = "AUTH_OTP";
    public static final String TRIGGER_AUTO = "AUTO";
    public static final String TRIGGER_RESEND = "RESEND";
    private static final String VALID_MINUTES = "5";

    private final SmsPort smsPort;
    private final SmsMessageRepository smsMessageRepository;
    private final SmsDeliveryAttemptRepository attemptRepository;
    private final SmsTemplateService smsTemplateService;

    public SmsApplicationService(SmsPort smsPort,
                                 SmsMessageRepository smsMessageRepository,
                                 SmsDeliveryAttemptRepository attemptRepository,
                                 SmsTemplateService smsTemplateService) {
        this.smsPort = smsPort;
        this.smsMessageRepository = smsMessageRepository;
        this.attemptRepository = attemptRepository;
        this.smsTemplateService = smsTemplateService;
    }

    @Override
    @Transactional
    public SmsMessage sendAuthOtp(String mobileCountryCode, String mobile, OtpPurpose purpose,
                                  String otpCode, Long otpVerificationId,
                                  String triggerSource, Long adminId) {
        // MobiWave expects digits only, e.g. 2547XXXXXXXX (no plus / spaces).
        String recipient = KenyaPhoneNormalizer.toRecipient(mobileCountryCode + mobile)
                .orElse(mobileCountryCode + mobile);
        String body = renderOtpBody(purpose, otpCode);
        String redacted = KenyaPhoneNormalizer.redactOtpDigits(body);

        SmsMessage msg = new SmsMessage();
        msg.setPublicId(UUID.randomUUID().toString());
        msg.setRecipient(recipient);
        msg.setPurpose(PURPOSE_AUTH_OTP);
        msg.setOtpPurpose(purpose.name());
        msg.setOtpVerificationId(otpVerificationId);
        msg.setMessageBody(body);
        msg.setMessageRedacted(redacted);
        msg.setStatus("PENDING");
        msg.setCreatedByAdminId(adminId);
        msg = smsMessageRepository.save(msg);

        SmsSendResult result = smsPort.send(new SmsSendRequest(recipient, body));
        applyResult(msg, result, triggerSource != null ? triggerSource : TRIGGER_AUTO, adminId);
        return smsMessageRepository.save(msg);
    }

    private void applyResult(SmsMessage msg, SmsSendResult result, String triggerSource, Long adminId) {
        msg.setStatus(result.status());
        msg.setProviderUid(result.providerUid());
        msg.setErrorMessage(result.errorMessage());
        if (result.isSent()) {
            msg.setSentAt(Instant.now());
        }

        SmsDeliveryAttempt attempt = new SmsDeliveryAttempt();
        attempt.setSmsMessageId(msg.getId());
        attempt.setTriggerSource(triggerSource);
        attempt.setStatus(result.status());
        attempt.setProviderUid(result.providerUid());
        attempt.setErrorMessage(result.errorMessage());
        attempt.setCreatedByAdminId(adminId);
        attemptRepository.save(attempt);
    }

    private String renderOtpBody(OtpPurpose purpose, String otpCode) {
        String key = templateKeyFor(purpose);
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("otp", otpCode != null ? otpCode : "");
        vars.put("purpose_label", purposeLabel(purpose));
        vars.put("valid_minutes", VALID_MINUTES);
        return smsTemplateService.render(key, vars);
    }

    static String templateKeyFor(OtpPurpose purpose) {
        if (purpose == null) {
            return SmsTemplateKeys.OTP_SIGNUP;
        }
        return switch (purpose) {
            case SIGNUP -> SmsTemplateKeys.OTP_SIGNUP;
            case DEVICE_BIND -> SmsTemplateKeys.OTP_DEVICE_BIND;
            case PIN_RESET -> SmsTemplateKeys.OTP_PIN_RESET;
            case CREDENTIALS_MIGRATE -> SmsTemplateKeys.OTP_CREDENTIALS_MIGRATE;
        };
    }

    static String purposeLabel(OtpPurpose purpose) {
        if (purpose == null) {
            return "verification";
        }
        return switch (purpose) {
            case SIGNUP -> "verification";
            case DEVICE_BIND -> "new device login";
            case PIN_RESET -> "PIN reset";
            case CREDENTIALS_MIGRATE -> "account setup";
        };
    }

    /**
     * Legacy helper for tests; prefers template defaults via {@link #renderOtpBody} in production path.
     */
    static String buildOtpMessage(OtpPurpose purpose, String otpCode) {
        Map<String, String> vars = Map.of(
                "otp", otpCode != null ? otpCode : "",
                "purpose_label", purposeLabel(purpose),
                "valid_minutes", VALID_MINUTES);
        return com.vycepay.common.sms.template.SmsTemplateRenderer.render(
                com.vycepay.common.sms.template.SmsTemplateDefaults.bodyFor(templateKeyFor(purpose)),
                vars);
    }
}
