package com.vycepay.kyc.api.v1;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.kyc.api.v1.dto.biometric.BiometricFaceMatchRequest;
import com.vycepay.kyc.api.v1.dto.biometric.BiometricMediaRequest;
import com.vycepay.kyc.api.v1.dto.biometric.BiometricOcrRequest;
import com.vycepay.kyc.api.v1.dto.biometric.CreateBiometricSessionRequest;
import com.vycepay.kyc.application.facade.KycBiometricFacade;
import com.vycepay.kyc.domain.model.Customer;
import com.vycepay.kyc.infrastructure.persistence.CustomerRepository;

/**
 * Mobile biometric KYC APIs (WeVipers via VycePay). Never call WeVipers from the app.
 */
@RestController
@RequestMapping("/api/v1/kyc/biometric")
public class KycBiometricController {

    private final KycBiometricFacade biometricFacade;
    private final CustomerRepository customerRepository;

    public KycBiometricController(KycBiometricFacade biometricFacade, CustomerRepository customerRepository) {
        this.biometricFacade = biometricFacade;
        this.customerRepository = customerRepository;
    }

    @PostMapping("/sessions")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> createSession(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody(required = false) CreateBiometricSessionRequest request) {
        Customer customer = requireCustomer(externalId);
        CreateBiometricSessionRequest body = request != null ? request : new CreateBiometricSessionRequest();
        boolean forceNew = Boolean.TRUE.equals(body.getForceNew());
        Map<String, Object> data = biometricFacade.createSession(
                customer.getId(), body.getIdType(), body.getDocumentCountry(), forceNew);
        return ResponseEntity.ok(ApiSuccessResponses.ok("KYC_BIOMETRIC_SESSION_CREATED",
                "Biometric session ready.", data));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(
            @RequestHeader("X-Customer-Id") String externalId,
            @PathVariable String sessionId) {
        Customer customer = requireCustomer(externalId);
        return ResponseEntity.ok(biometricFacade.getSession(customer.getId(), sessionId));
    }

    @PostMapping("/sessions/{sessionId}/document-liveness")
    public ResponseEntity<Map<String, Object>> documentLiveness(
            @RequestHeader("X-Customer-Id") String externalId,
            @PathVariable String sessionId,
            @RequestBody BiometricMediaRequest request) {
        Customer customer = requireCustomer(externalId);
        return ResponseEntity.ok(biometricFacade.documentLiveness(
                customer.getId(), sessionId, request.getVideoBase64()));
    }

    @PostMapping("/sessions/{sessionId}/ocr")
    public ResponseEntity<Map<String, Object>> ocr(
            @RequestHeader("X-Customer-Id") String externalId,
            @PathVariable String sessionId,
            @RequestBody BiometricOcrRequest request) {
        Customer customer = requireCustomer(externalId);
        return ResponseEntity.ok(biometricFacade.ocr(
                customer.getId(), sessionId, request.getDocumentKind(),
                request.getFrontImageBase64(), request.getBackImageBase64()));
    }

    @PostMapping("/sessions/{sessionId}/face-liveness")
    public ResponseEntity<Map<String, Object>> faceLiveness(
            @RequestHeader("X-Customer-Id") String externalId,
            @PathVariable String sessionId,
            @RequestBody BiometricMediaRequest request) {
        Customer customer = requireCustomer(externalId);
        return ResponseEntity.ok(biometricFacade.faceLiveness(
                customer.getId(), sessionId, request.getVideoBase64()));
    }

    @PostMapping("/sessions/{sessionId}/face-match")
    public ResponseEntity<Map<String, Object>> faceMatch(
            @RequestHeader("X-Customer-Id") String externalId,
            @PathVariable String sessionId,
            @RequestBody BiometricFaceMatchRequest request) {
        Customer customer = requireCustomer(externalId);
        return ResponseEntity.ok(biometricFacade.faceMatch(
                customer.getId(), sessionId, request.getIdPortraitBase64(), request.getSelfieBase64()));
    }

    private Customer requireCustomer(String externalId) {
        return customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
    }
}
