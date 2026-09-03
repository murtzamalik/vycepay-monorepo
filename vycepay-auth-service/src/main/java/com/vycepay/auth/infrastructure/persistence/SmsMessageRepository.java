package com.vycepay.auth.infrastructure.persistence;

import com.vycepay.auth.domain.model.SmsMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/**
 * Persistence for outbound SMS ledger rows.
 */
public interface SmsMessageRepository extends JpaRepository<SmsMessage, Long> {

    @Query("""
            SELECT COUNT(m) FROM SmsMessage m
            WHERE m.recipient = :recipient
              AND m.purpose = 'AUTH_OTP'
              AND m.createdByAdminId IS NOT NULL
              AND m.createdAt >= :since
            """)
    long countAdminAuthOtpResendsSince(@Param("recipient") String recipient,
                                       @Param("since") Instant since);

    SmsMessage findTopByRecipientAndPurposeAndOtpPurposeOrderByIdDesc(
            String recipient, String purpose, String otpPurpose);
}
