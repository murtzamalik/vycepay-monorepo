package com.vycepay.kyc.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vycepay.kyc.domain.model.biometric.BiometricStep;
import com.vycepay.kyc.domain.model.biometric.KycBiometricAttempt;

public interface KycBiometricAttemptRepository extends JpaRepository<KycBiometricAttempt, Long> {
    List<KycBiometricAttempt> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    int countBySessionIdAndStep(Long sessionId, BiometricStep step);
}
