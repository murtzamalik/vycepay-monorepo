package com.vycepay.kyc.api.v1;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vycepay.kyc.api.v1.dto.KycStatusResponse;
import com.vycepay.kyc.api.v1.dto.KycSubmitRequest;
import com.vycepay.kyc.application.facade.KycOnboardingFacade;
import com.vycepay.kyc.domain.model.Customer;
import com.vycepay.kyc.domain.model.KycVerification;
import com.vycepay.kyc.infrastructure.persistence.CustomerRepository;
import com.vycepay.kyc.infrastructure.persistence.KycVerificationRepository;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import com.vycepay.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * KYC API: status and submit.
 * All endpoints require customer context (externalId from JWT - passed as header for now).
 */
@RestController
@RequestMapping("/api/v1/kyc")
public class KycController {

    private final KycOnboardingFacade kycFacade;
    private final CustomerRepository customerRepository;
    private final KycVerificationRepository kycRepository;

    public KycController(@Autowired(required = false) KycOnboardingFacade kycFacade,
                         CustomerRepository customerRepository,
                         KycVerificationRepository kycRepository) {
        this.kycFacade = kycFacade; // null when Choice Bank not configured
        this.customerRepository = customerRepository;
        this.kycRepository = kycRepository;
    }

    /**
     * Returns latest KYC status for customer.
     *
     * @param XCustomerId Header with customer external ID (from JWT in production)
     */
    @GetMapping("/status")
    public ResponseEntity<KycStatusResponse> getStatus(@RequestHeader("X-Customer-Id") String externalId) {
        Customer customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        List<KycVerification> list = kycRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        if (list.isEmpty()) {
            return ResponseEntity.ok(new KycStatusResponse("NOT_STARTED", null, null));
        }
        KycVerification latest = list.get(0);
        return ResponseEntity.ok(new KycStatusResponse(
                latest.getStatus(),
                latest.getChoiceOnboardingRequestId(),
                latest.getChoiceAccountId()));
    }

    /**
     * Submits KYC to Choice Bank. Triggers async flow; OTP must be confirmed separately.
     */
    @PostMapping("/submit")
    public ResponseEntity<KycStatusResponse> submit(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody KycSubmitRequest request) {
        if (kycFacade == null) {
            return ResponseEntity.status(503).build();
        }
        Customer customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        var params = request.toChoiceParams(externalId);
        String onboardingRequestId = kycFacade.submitOnboarding(customer.getId(), externalId, params);
        return ResponseEntity.ok(new KycStatusResponse("1", onboardingRequestId, null));
    }

    /**
     * Sends OTP for KYC confirmation.
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiSuccessResponse<Void>> sendOtp(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestParam String onboardingRequestId) {
        if (kycFacade == null) {
            throw new BusinessException("SERVICE_UNAVAILABLE", "KYC service is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        kycFacade.sendOtp(onboardingRequestId, "SMS");
        return ResponseEntity.ok(ApiSuccessResponses.ok("KYC_OTP_SENT", "KYC OTP sent successfully."));
    }

    /**
     * Resends OTP for KYC (Choice Bank common/resendOtp).
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiSuccessResponse<Void>> resendOtp(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestParam String onboardingRequestId,
            @RequestParam(defaultValue = "SMS") String otpType) {
        if (kycFacade == null) {
            throw new BusinessException("SERVICE_UNAVAILABLE", "KYC service is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        Customer customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        KycVerification kyc = kycRepository.findByChoiceOnboardingRequestId(onboardingRequestId)
                .orElseThrow(() -> new BusinessException("KYC_NOT_FOUND", "Onboarding request not found", HttpStatus.NOT_FOUND));
        if (!kyc.getCustomerId().equals(customer.getId())) {
            throw new BusinessException("FORBIDDEN", "Onboarding request does not belong to this customer", HttpStatus.FORBIDDEN);
        }
        kycFacade.resendOtp(onboardingRequestId, otpType);
        return ResponseEntity.ok(ApiSuccessResponses.ok("KYC_OTP_RESENT", "KYC OTP resent successfully."));
    }

    /**
     * Confirms OTP for KYC. Requires X-Customer-Id; onboardingRequestId must belong to this customer.
     */
    @PostMapping("/confirm-otp")
    public ResponseEntity<ApiSuccessResponse<Void>> confirmOtp(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestParam String onboardingRequestId,
            @RequestParam String otpCode) {
        if (kycFacade == null) {
            throw new BusinessException("SERVICE_UNAVAILABLE", "KYC service is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        Customer customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        KycVerification kyc = kycRepository.findByChoiceOnboardingRequestId(onboardingRequestId)
                .orElseThrow(() -> new BusinessException("KYC_NOT_FOUND", "Onboarding request not found", HttpStatus.NOT_FOUND));
        if (!kyc.getCustomerId().equals(customer.getId())) {
            throw new BusinessException("FORBIDDEN", "Onboarding request does not belong to this customer", HttpStatus.FORBIDDEN);
        }
        kycFacade.confirmOtp(onboardingRequestId, otpCode);
        return ResponseEntity.ok(ApiSuccessResponses.ok("KYC_OTP_CONFIRMED", "KYC OTP confirmed successfully."));
    }
}
