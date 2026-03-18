package com.vycepay.callback.infrastructure.persistence;

import com.vycepay.callback.domain.model.KycVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository for KYC verification records. */
public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {

    Optional<KycVerification> findByChoiceOnboardingRequestId(String choiceOnboardingRequestId);
}
