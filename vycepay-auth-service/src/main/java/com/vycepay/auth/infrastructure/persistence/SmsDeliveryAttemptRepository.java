package com.vycepay.auth.infrastructure.persistence;

import com.vycepay.auth.domain.model.SmsDeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for SMS delivery attempts.
 */
public interface SmsDeliveryAttemptRepository extends JpaRepository<SmsDeliveryAttempt, Long> {
}
