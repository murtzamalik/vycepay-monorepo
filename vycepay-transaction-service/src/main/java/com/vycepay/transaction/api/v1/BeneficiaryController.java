package com.vycepay.transaction.api.v1;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.transaction.api.v1.dto.BeneficiaryListResponse;
import com.vycepay.transaction.api.v1.dto.BeneficiaryResponse;
import com.vycepay.transaction.api.v1.dto.CreateBeneficiaryRequest;
import com.vycepay.transaction.api.v1.dto.UpdateBeneficiaryRequest;
import com.vycepay.transaction.application.service.BeneficiaryService;
import com.vycepay.transaction.infrastructure.persistence.CustomerRepository;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Saved beneficiaries CRUD. Selecting a beneficiary still requires validate-account + send.
 */
@RestController
@RequestMapping("/api/v1/transactions/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final CustomerRepository customerRepository;

    public BeneficiaryController(BeneficiaryService beneficiaryService,
                                 CustomerRepository customerRepository) {
        this.beneficiaryService = beneficiaryService;
        this.customerRepository = customerRepository;
    }

    @Operation(summary = "List active beneficiaries")
    @GetMapping
    public ResponseEntity<BeneficiaryListResponse> list(
            @RequestHeader("X-Customer-Id") String externalId) {
        var customer = requireCustomer(externalId);
        return ResponseEntity.ok(beneficiaryService.list(customer.getId()));
    }

    @Operation(summary = "Create or upsert beneficiary")
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<BeneficiaryResponse>> create(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody CreateBeneficiaryRequest request) {
        var customer = requireCustomer(externalId);
        BeneficiaryService.SaveResult result = beneficiaryService.save(customer.getId(), request);
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiSuccessResponses.ok("BENEFICIARY_SAVED", "Beneficiary saved.", result.response()));
        }
        return ResponseEntity.ok(
                ApiSuccessResponses.ok("BENEFICIARY_UPDATED", "Beneficiary updated.", result.response()));
    }

    @Operation(summary = "Update beneficiary nickname")
    @PatchMapping("/{externalId}")
    public ResponseEntity<ApiSuccessResponse<BeneficiaryResponse>> updateNickname(
            @RequestHeader("X-Customer-Id") String customerExternalId,
            @PathVariable("externalId") String beneficiaryExternalId,
            @RequestBody UpdateBeneficiaryRequest request) {
        var customer = requireCustomer(customerExternalId);
        BeneficiaryResponse data = beneficiaryService.updateNickname(
                customer.getId(), beneficiaryExternalId, request);
        return ResponseEntity.ok(
                ApiSuccessResponses.ok("BENEFICIARY_UPDATED", "Beneficiary updated.", data));
    }

    @Operation(summary = "Soft-delete beneficiary")
    @DeleteMapping("/{externalId}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(
            @RequestHeader("X-Customer-Id") String customerExternalId,
            @PathVariable("externalId") String beneficiaryExternalId) {
        var customer = requireCustomer(customerExternalId);
        beneficiaryService.softDelete(customer.getId(), beneficiaryExternalId);
        return ResponseEntity.ok(
                ApiSuccessResponses.ok("BENEFICIARY_DELETED", "Beneficiary deleted."));
    }

    private com.vycepay.transaction.domain.model.Customer requireCustomer(String externalId) {
        return customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND",
                        "Customer not found", HttpStatus.NOT_FOUND));
    }
}
