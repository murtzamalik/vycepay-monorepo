package com.vycepay.auth.application.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.vycepay.auth.domain.model.OtpPurpose;
import com.vycepay.auth.domain.model.OtpVerification;
import com.vycepay.auth.domain.model.SmsMessage;
import com.vycepay.auth.infrastructure.persistence.OtpVerificationRepository;
import com.vycepay.auth.infrastructure.persistence.SmsMessageRepository;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.sms.KenyaPhoneNormalizer;

/**
 * Generates and validates purpose-scoped OTPs for signup, device bind, PIN reset, and migrate.
 * Delivers SMS via {@link AuthOtpSmsPort} (soft-fail).
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ADMIN_RESENDS_PER_HOUR = 5;

    private final OtpVerificationRepository otpRepository;
    private final SmsMessageRepository smsMessageRepository;
    private final AuthOtpSmsPort authOtpSmsPort;
    private final int otpLength;
    private final int otpExpiryMinutes;
    private final String devFixedCode;

    public OtpService(OtpVerificationRepository otpRepository,
                      SmsMessageRepository smsMessageRepository,
                      AuthOtpSmsPort authOtpSmsPort,
                      @Value("${vycepay.otp.length:6}") int otpLength,
                      @Value("${vycepay.otp.expiry-minutes:5}") int otpExpiryMinutes,
                      @Value("${vycepay.otp.dev-fixed-code:}") String devFixedCode) {
        this.otpRepository = otpRepository;
        this.smsMessageRepository = smsMessageRepository;
        this.authOtpSmsPort = authOtpSmsPort;
        this.otpLength = otpLength;
        this.otpExpiryMinutes = otpExpiryMinutes;
        this.devFixedCode = devFixedCode == null ? "" : devFixedCode.trim();
    }

    /**
     * Generates OTP for the given purpose, persists it, and sends SMS (best-effort).
     */
    public String sendOtp(String mobileCountryCode, String mobile, OtpPurpose purpose) {
        return sendOtp(mobileCountryCode, mobile, purpose, SmsApplicationService.TRIGGER_AUTO, null);
    }

    /**
     * Generates OTP and sends SMS with explicit trigger source (AUTO or RESEND) and optional admin id.
     */
    public String sendOtp(String mobileCountryCode, String mobile, OtpPurpose purpose,
                          String triggerSource, Long adminId) {
        String code = generateOtp();
        Instant expiresAt = Instant.now().plus(otpExpiryMinutes, TimeUnit.MINUTES.toChronoUnit());

        OtpVerification otp = new OtpVerification();
        otp.setMobileCountryCode(mobileCountryCode);
        otp.setMobile(mobile);
        otp.setPurpose(purpose.name());
        otp.setOtpCode(code);
        otp.setExpiresAt(expiresAt);
        otp.setVerified(false);
        otp = otpRepository.save(otp);

        try {
            SmsMessage sent = authOtpSmsPort.sendAuthOtp(
                    mobileCountryCode, mobile, purpose, code, otp.getId(), triggerSource, adminId);
            log.info("OTP sent purpose={} to {} {} smsStatus={} expires {}",
                    purpose, mobileCountryCode, maskMobile(mobile), sent.getStatus(), expiresAt);
        } catch (Exception e) {
            log.warn("OTP SMS soft-fail purpose={} to {} {}: {}",
                    purpose, mobileCountryCode, maskMobile(mobile), e.getMessage());
        }
        return code;
    }

    /**
     * Admin resend: loads AUTH_OTP ledger row, rate-limits, generates a new OTP and SMS.
     */
    public Map<String, Object> resendAuthOtpFromMessage(Long smsMessageId, Long adminId) {
        SmsMessage original = smsMessageRepository.findById(smsMessageId)
                .orElseThrow(() -> new BusinessException("SMS_NOT_FOUND", "SMS message not found", HttpStatus.NOT_FOUND));
        if (!SmsApplicationService.PURPOSE_AUTH_OTP.equals(original.getPurpose())) {
            throw new BusinessException("SMS_NOT_AUTH_OTP",
                    "Only AUTH_OTP messages can be resent via auth", HttpStatus.BAD_REQUEST);
        }
        if (original.getOtpPurpose() == null || original.getOtpPurpose().isBlank()) {
            throw new BusinessException("SMS_INVALID", "Missing OTP purpose on SMS message", HttpStatus.BAD_REQUEST);
        }

        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        long recent = smsMessageRepository.countAdminAuthOtpResendsSince(original.getRecipient(), since);
        if (recent >= MAX_ADMIN_RESENDS_PER_HOUR) {
            throw new BusinessException("SMS_RESEND_LIMIT",
                    "Maximum " + MAX_ADMIN_RESENDS_PER_HOUR + " OTP resends per hour for this recipient",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        String recipient = original.getRecipient();
        if (recipient == null || recipient.length() < 12) {
            throw new BusinessException("SMS_INVALID", "Invalid recipient on SMS message", HttpStatus.BAD_REQUEST);
        }
        String countryCode = recipient.substring(0, 3);
        String mobile = recipient.substring(3);
        OtpPurpose purpose = OtpPurpose.valueOf(original.getOtpPurpose());

        sendOtp(countryCode, mobile, purpose, SmsApplicationService.TRIGGER_RESEND, adminId);

        SmsMessage newest = smsMessageRepository.findTopByRecipientAndPurposeAndOtpPurposeOrderByIdDesc(
                recipient, SmsApplicationService.PURPOSE_AUTH_OTP, purpose.name());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("originalSmsMessageId", smsMessageId);
        data.put("otpPurpose", purpose.name());
        data.put("recipientMasked", KenyaPhoneNormalizer.maskRecipient(recipient));
        if (newest != null) {
            data.put("smsMessageId", newest.getId());
            data.put("status", newest.getStatus());
            data.put("providerUid", newest.getProviderUid());
            data.put("errorMessage", newest.getErrorMessage());
        }
        return data;
    }

    /**
     * Verifies OTP for purpose. Marks as verified if valid.
     *
     * @throws BusinessException INVALID_OTP or OTP_EXPIRED
     */
    public void verifyOtpOrThrow(String mobileCountryCode, String mobile, String otpCode, OtpPurpose purpose) {
        OtpVerification otp = otpRepository.findLatestValidOtp(mobileCountryCode, mobile, purpose.name())
                .orElseThrow(() -> new BusinessException("INVALID_OTP", "Invalid or expired OTP", HttpStatus.BAD_REQUEST));

        if (otp.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("OTP_EXPIRED", "OTP has expired", HttpStatus.BAD_REQUEST);
        }
        if (!otp.getOtpCode().equals(otpCode)) {
            throw new BusinessException("INVALID_OTP", "Invalid or expired OTP", HttpStatus.BAD_REQUEST);
        }
        otp.setVerified(true);
        otpRepository.save(otp);
    }

    private String generateOtp() {
        if (!devFixedCode.isEmpty()) {
            return devFixedCode;
        }
        StringBuilder sb = new StringBuilder(otpLength);
        for (int i = 0; i < otpLength; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return "****" + mobile.substring(mobile.length() - 4);
    }
}
