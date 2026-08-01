package com.vycepay.transaction.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vycepay.transaction.domain.model.Beneficiary;

/**
 * Persistence for saved beneficiaries.
 */
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByCustomerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long customerId);

    Optional<Beneficiary> findByExternalIdAndCustomerIdAndDeletedAtIsNull(String externalId, Long customerId);

    Optional<Beneficiary> findByCustomerIdAndAccountTypeAndPayeeBankCodeAndPayeeAccountId(
            Long customerId, int accountType, String payeeBankCode, String payeeAccountId);
}
