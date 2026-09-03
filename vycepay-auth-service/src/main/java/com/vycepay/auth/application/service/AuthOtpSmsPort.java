package com.vycepay.auth.application.service;

import com.vycepay.auth.domain.model.OtpPurpose;
import com.vycepay.auth.domain.model.SmsMessage;

/**
 * Delivers auth OTP SMS and persists the outbound ledger row.
 */
public interface AuthOtpSmsPort {

    /**
     * Sends an auth OTP SMS and returns the ledger row with final status.
     */
    SmsMessage sendAuthOtp(String mobileCountryCode, String mobile, OtpPurpose purpose,
                           String otpCode, Long otpVerificationId,
                           String triggerSource, Long adminId);
}
