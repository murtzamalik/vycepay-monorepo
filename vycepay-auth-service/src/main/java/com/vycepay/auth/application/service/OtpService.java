package com.vycepay.auth.application.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.vycepay.auth.domain.model.OtpPurpose;
import com.vycepay.auth.domain.model.OtpVerification;
import com.vycepay.auth.infrastructure.persistence.OtpVerificationRepository;
import com.vycepay.common.exception.BusinessException;

/**
 * Generates and validates purpose-scoped OTPs for signup, device bind, PIN reset, and migrate.
 * SMS delivery is logged in dev; production should wire an SMS port.
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpVerificationRepository otpRepository;
    private final int otpLength;
    private final int otpExpiryMinutes;
    private final String devFixedCode;

    public OtpService(OtpVerificationRepository otpRepository,
                      @Value("${vycepay.otp.length:6}") int otpLength,
                      @Value("${vycepay.otp.expiry-minutes:5}") int otpExpiryMinutes,
                      @Value("${vycepay.otp.dev-fixed-code:}") String devFixedCode) {
        this.otpRepository = otpRepository;
        this.otpLength = otpLength;
        this.otpExpiryMinutes = otpExpiryMinutes;
        this.devFixedCode = devFixedCode == null ? "" : devFixedCode.trim();
    }

    /**
     * Generates OTP for the given purpose, persists it, and logs (dev) / sends SMS.
     */
    public String sendOtp(String mobileCountryCode, String mobile, OtpPurpose purpose) {
        String code = generateOtp();
        Instant expiresAt = Instant.now().plus(otpExpiryMinutes, TimeUnit.MINUTES.toChronoUnit());

        OtpVerification otp = new OtpVerification();
        otp.setMobileCountryCode(mobileCountryCode);
        otp.setMobile(mobile);
        otp.setPurpose(purpose.name());
        otp.setOtpCode(code);
        otp.setExpiresAt(expiresAt);
        otp.setVerified(false);
        otpRepository.save(otp);

        // Never log the code in production-style messaging; keep for local when fixed code enabled
        if (!devFixedCode.isEmpty()) {
            log.info("OTP sent purpose={} to {} {} (dev-fixed) expires {}",
                    purpose, mobileCountryCode, maskMobile(mobile), expiresAt);
        } else {
            log.info("OTP sent purpose={} to {} {} expires {}",
                    purpose, mobileCountryCode, maskMobile(mobile), expiresAt);
        }
        return code;
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
