package com.vycepay.kyc.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vycepay.kyc.domain.model.biometric.BiometricSessionStatus;
import com.vycepay.kyc.domain.model.biometric.KycBiometricSession;

public interface KycBiometricSessionRepository extends JpaRepository<KycBiometricSession, Long> {
    Optional<KycBiometricSession> findByPublicId(String publicId);
    Optional<KycBiometricSession> findFirstByCustomerIdAndStatusInOrderByCreatedAtDesc(
            Long customerId, List<BiometricSessionStatus> statuses);
    List<KycBiometricSession> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
