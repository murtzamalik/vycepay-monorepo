package com.vycepay.transaction.infrastructure.persistence;

import com.vycepay.transaction.domain.model.KycVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Loads KYC rows to resolve Choice Bank {@code userId} ({@code choice_user_id}).
 */
public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {

    List<KycVerification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
