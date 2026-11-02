package com.vycepay.transaction.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vycepay.common.exception.BusinessException;
import com.vycepay.transaction.api.v1.dto.BeneficiaryListResponse;
import com.vycepay.transaction.api.v1.dto.BeneficiaryResponse;
import com.vycepay.transaction.api.v1.dto.CreateBeneficiaryRequest;
import com.vycepay.transaction.api.v1.dto.UpdateBeneficiaryRequest;
import com.vycepay.transaction.domain.model.Beneficiary;
import com.vycepay.transaction.infrastructure.persistence.BeneficiaryRepository;

/**
 * CRUD for saved transfer beneficiaries with soft-delete and identity upsert.
 * Paybill ({@code accountType} 1) identity includes {@code payeeReferenceNumber} (Acc).
 */
@Service
public class BeneficiaryService {

    private static final Logger log = LoggerFactory.getLogger(BeneficiaryService.class);
    private static final int ACCOUNT_TYPE_MIN = 0;
    private static final int ACCOUNT_TYPE_MAX = 5;
    private static final int ACCOUNT_TYPE_PAYBILL = 1;
    private static final int ACCOUNT_TYPE_PESALINK = 4;
    private static final int NICKNAME_MAX = 50;
    private static final int REFERENCE_MAX = 64;

    private final BeneficiaryRepository beneficiaryRepository;

    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
    }

    /**
     * Lists active beneficiaries for the customer, newest updated first.
     */
    @Transactional(readOnly = true)
    public BeneficiaryListResponse list(Long customerId) {
        List<BeneficiaryResponse> items = beneficiaryRepository
                .findByCustomerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new BeneficiaryListResponse(items);
    }

    /**
     * Creates or upserts a beneficiary. Soft-deleted match is restored.
     * Identity: customer + accountType + bankCode + accountId + reference (empty for non-Paybill).
     *
     * @return result with response DTO and whether this was a new create vs update/restore
     */
    @Transactional
    public SaveResult save(Long customerId, CreateBeneficiaryRequest request) {
        String nickname = requireNickname(request.getNickname());
        int accountType = requireAccountType(request.getAccountType());
        String accountId = requireAccountId(request.getPayeeAccountId());
        String bankCode = normalizeBankCode(accountType, request.getPayeeBankCode());
        String accountName = trimToNull(request.getPayeeAccountName(), 120);
        String reference = normalizeReference(accountType, request.getPayeeReferenceNumber());

        var existing = beneficiaryRepository
                .findByCustomerIdAndAccountTypeAndPayeeBankCodeAndPayeeAccountIdAndPayeeReferenceNumber(
                        customerId, accountType, bankCode, accountId, reference);

        if (existing.isPresent()) {
            Beneficiary b = existing.get();
            boolean wasDeleted = b.isDeleted();
            b.setNickname(nickname);
            if (accountName != null) {
                b.setPayeeAccountName(accountName);
            }
            b.setDeletedAt(null);
            beneficiaryRepository.save(b);
            log.info("Beneficiary upserted customerId={} externalId={} restored={}",
                    customerId, b.getExternalId(), wasDeleted);
            return new SaveResult(toResponse(b), false);
        }

        Beneficiary created = new Beneficiary();
        created.setExternalId(UUID.randomUUID().toString());
        created.setCustomerId(customerId);
        created.setNickname(nickname);
        created.setAccountType(accountType);
        created.setPayeeBankCode(bankCode);
        created.setPayeeAccountId(accountId);
        created.setPayeeAccountName(accountName);
        created.setPayeeReferenceNumber(reference);
        beneficiaryRepository.save(created);
        log.info("Beneficiary created customerId={} externalId={}", customerId, created.getExternalId());
        return new SaveResult(toResponse(created), true);
    }

    /**
     * Updates nickname for an owned active beneficiary.
     */
    @Transactional
    public BeneficiaryResponse updateNickname(Long customerId, String externalId, UpdateBeneficiaryRequest request) {
        Beneficiary b = requireOwnedActive(customerId, externalId);
        b.setNickname(requireNickname(request.getNickname()));
        beneficiaryRepository.save(b);
        return toResponse(b);
    }

    /**
     * Soft-deletes an owned active beneficiary.
     */
    @Transactional
    public void softDelete(Long customerId, String externalId) {
        Beneficiary b = requireOwnedActive(customerId, externalId);
        b.setDeletedAt(Instant.now());
        beneficiaryRepository.save(b);
        log.info("Beneficiary soft-deleted customerId={} externalId={}", customerId, externalId);
    }

    private Beneficiary requireOwnedActive(Long customerId, String externalId) {
        if (externalId == null || externalId.isBlank()) {
            throw new BusinessException("BENEFICIARY_NOT_FOUND", "Beneficiary not found", HttpStatus.NOT_FOUND);
        }
        return beneficiaryRepository.findByExternalIdAndCustomerIdAndDeletedAtIsNull(externalId.trim(), customerId)
                .orElseThrow(() -> new BusinessException("BENEFICIARY_NOT_FOUND",
                        "Beneficiary not found", HttpStatus.NOT_FOUND));
    }

    private String requireNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new BusinessException("INVALID_NICKNAME", "Nickname is required", HttpStatus.BAD_REQUEST);
        }
        String trimmed = nickname.trim();
        if (trimmed.length() > NICKNAME_MAX) {
            throw new BusinessException("INVALID_NICKNAME",
                    "Nickname must be at most " + NICKNAME_MAX + " characters", HttpStatus.BAD_REQUEST);
        }
        return trimmed;
    }

    private int requireAccountType(Integer accountType) {
        if (accountType == null || accountType < ACCOUNT_TYPE_MIN || accountType > ACCOUNT_TYPE_MAX) {
            throw new BusinessException("INVALID_ACCOUNT_TYPE",
                    "accountType must be an integer between 0 and 5", HttpStatus.BAD_REQUEST);
        }
        return accountType;
    }

    private String requireAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new BusinessException("INVALID_ACCOUNT_ID", "payeeAccountId is required", HttpStatus.BAD_REQUEST);
        }
        return accountId.trim();
    }

    private String normalizeBankCode(int accountType, String payeeBankCode) {
        String code = payeeBankCode == null ? "" : payeeBankCode.trim();
        if (accountType == ACCOUNT_TYPE_PESALINK && code.isEmpty()) {
            throw new BusinessException("BANK_CODE_REQUIRED",
                    "payeeBankCode is required when accountType is 4 (PesaLink)", HttpStatus.BAD_REQUEST);
        }
        return code;
    }

    /**
     * Paybill requires Acc; other rails ignore any provided reference (store empty).
     */
    private String normalizeReference(int accountType, String payeeReferenceNumber) {
        if (accountType != ACCOUNT_TYPE_PAYBILL) {
            return "";
        }
        if (payeeReferenceNumber == null || payeeReferenceNumber.isBlank()) {
            throw new BusinessException("INVALID_PAYEE_REFERENCE",
                    "payeeReferenceNumber is required when accountType is 1 (M-Pesa Paybill)",
                    HttpStatus.BAD_REQUEST);
        }
        String trimmed = payeeReferenceNumber.trim();
        if (trimmed.length() > REFERENCE_MAX) {
            throw new BusinessException("INVALID_PAYEE_REFERENCE",
                    "payeeReferenceNumber must be at most " + REFERENCE_MAX + " characters",
                    HttpStatus.BAD_REQUEST);
        }
        return trimmed;
    }

    private String trimToNull(String value, int maxLen) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > maxLen ? trimmed.substring(0, maxLen) : trimmed;
    }

    private BeneficiaryResponse toResponse(Beneficiary b) {
        String ref = b.getPayeeReferenceNumber();
        String refOut = (ref == null || ref.isBlank()) ? null : ref;
        return new BeneficiaryResponse(
                b.getExternalId(),
                b.getNickname(),
                b.getAccountType(),
                b.getPayeeBankCode(),
                b.getPayeeAccountId(),
                b.getPayeeAccountName(),
                refOut);
    }

    /**
     * Outcome of create/upsert.
     */
    public record SaveResult(BeneficiaryResponse response, boolean created) {
    }
}
