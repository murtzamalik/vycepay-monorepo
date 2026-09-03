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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Persists SMS ledger rows and delivers via {@link SmsPort} (soft-fail on provider errors).
 */
@Service
public class SmsApplicationService implements AuthOtpSmsPort {

    public static final String PURPOSE_AUTH_OTP = "AUTH_OTP";
    public static final String TRIGGER_AUTO = "AUTO";
    public static final String TRIGGER_RESEND = "RESEND";

    private final SmsPort smsPort;
    private final SmsMessageRepository smsMessageRepository;
    private final SmsDeliveryAttemptRepository attemptRepository;

    public SmsApplicationService(SmsPort smsPort,
                                 SmsMessageRepository smsMessageRepository,
                                 SmsDeliveryAttemptRepository attemptRepository) {
        this.smsPort = smsPort;
        this.smsMessageRepository = smsMessageRepository;
        this.attemptRepository = attemptRepository;
    }

    @Override
    @Transactional
    public SmsMessage sendAuthOtp(String mobileCountryCode, String mobile, OtpPurpose purpose,
                                  String otpCode, Long otpVerificationId,
                                  String triggerSource, Long adminId) {
        String recipient = mobileCountryCode + mobile;
        String body = buildOtpMessage(purpose, otpCode);
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

    static String buildOtpMessage(OtpPurpose purpose, String otpCode) {
        String label = switch (purpose) {
            case SIGNUP -> "verification";
            case DEVICE_BIND -> "new device login";
            case PIN_RESET -> "PIN reset";
            case CREDENTIALS_MIGRATE -> "account setup";
        };
        return "Your VycePay " + label + " code is " + otpCode + ". Valid for 5 minutes. Do not share.";
    }
}
