package com.vycepay.auth.api.internal;

import com.vycepay.auth.application.service.OtpService;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Internal SMS OTP APIs for admin-service (AUTH_OTP resend with new code).
 * Protected by {@code X-Internal-Api-Key}.
 */
@RestController
@RequestMapping("/internal/v1/sms")
public class InternalSmsController {

    private final OtpService otpService;

    public InternalSmsController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/otp/resend")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> resendOtp(
            @RequestHeader(value = "X-Admin-Id", required = false) Long adminIdHeader,
            @Valid @RequestBody ResendOtpRequest body) {
        Long adminId = body.adminId() != null ? body.adminId() : adminIdHeader;
        Map<String, Object> result = otpService.resendAuthOtpFromMessage(body.smsMessageId(), adminId);
        return ResponseEntity.ok(ApiSuccessResponses.ok("SMS_OTP_RESENT", "OTP SMS resent", result));
    }

    public record ResendOtpRequest(
            @NotNull Long smsMessageId,
            Long adminId
    ) {}
}
