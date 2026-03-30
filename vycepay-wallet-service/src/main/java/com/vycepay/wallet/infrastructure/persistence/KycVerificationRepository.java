package com.vycepay.wallet.infrastructure.persistence;

import com.vycepay.wallet.domain.model.KycVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Read access to kyc_verification for Choice userId. */
public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {

    List<KycVerification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
