package com.vycepay.auth.infrastructure.persistence;

import com.vycepay.auth.domain.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repository for OTP verification records.
 */
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    /**
     * Finds latest valid OTP for mobile (unverified, not expired).
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.mobileCountryCode = :cc AND o.mobile = :mobile "
            + "AND o.verified = false AND o.expiresAt > CURRENT_TIMESTAMP ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpVerification> findLatestValidOtp(String cc, String mobile);
}
