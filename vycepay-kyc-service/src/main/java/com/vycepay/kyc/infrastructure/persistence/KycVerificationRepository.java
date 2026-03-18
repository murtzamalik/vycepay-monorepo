package com.vycepay.kyc.infrastructure.persistence;

import com.vycepay.kyc.domain.model.KycVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for KYC verification records.
 */
public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {

    Optional<KycVerification> findByChoiceOnboardingRequestId(String choiceOnboardingRequestId);

    List<KycVerification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
