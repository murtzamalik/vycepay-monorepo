package com.vycepay.auth.application.service;

import com.vycepay.auth.domain.model.OtpVerification;
import com.vycepay.auth.infrastructure.persistence.OtpVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Generates and validates OTP for registration.
 * SMS delivery is delegated to a port; default implementation logs for dev.
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpVerificationRepository otpRepository;
    private final int otpLength;
    private final int otpExpiryMinutes;

    public OtpService(OtpVerificationRepository otpRepository,
                      @Value("${vycepay.otp.length:6}") int otpLength,
                      @Value("${vycepay.otp.expiry-minutes:5}") int otpExpiryMinutes) {
        this.otpRepository = otpRepository;
        this.otpLength = otpLength;
        this.otpExpiryMinutes = otpExpiryMinutes;
    }

    /**
     * Generates OTP, persists it, and sends via SMS (or logs in dev).
     *
     * @param mobileCountryCode Country code (e.g. 254)
     * @param mobile            Mobile number
     * @return OTP that was sent (for dev/testing; production would not return)
     */
    public String sendOtp(String mobileCountryCode, String mobile) {
        String code = generateOtp();
        Instant expiresAt = Instant.now().plus(otpExpiryMinutes, TimeUnit.MINUTES.toChronoUnit());

        OtpVerification otp = new OtpVerification();
        otp.setMobileCountryCode(mobileCountryCode);
        otp.setMobile(mobile);
        otp.setOtpCode(code);
        otp.setExpiresAt(expiresAt);
        otp.setVerified(false);
        otpRepository.save(otp);

        log.info("OTP sent to {} {}: {} (expires {})", mobileCountryCode, maskMobile(mobile), code, expiresAt);
        return code;
    }

    /**
     * Verifies OTP. Marks as verified if valid.
     *
     * @param mobileCountryCode Country code
     * @param mobile            Mobile number
     * @param otpCode           Code entered by user
     * @return true if valid and verified
     */
    public boolean verifyOtp(String mobileCountryCode, String mobile, String otpCode) {
        return otpRepository.findLatestValidOtp(mobileCountryCode, mobile)
                .filter(o -> o.getOtpCode().equals(otpCode))
                .map(otp -> {
                    otp.setVerified(true);
                    otpRepository.save(otp);
                    return true;
                })
                .orElse(false);
    }

    private String generateOtp() {
        StringBuilder sb = new StringBuilder(otpLength);
        for (int i = 0; i < otpLength; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return "****" + mobile.substring(mobile.length() - 4);
    }
}
