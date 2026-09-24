package com.vycepay.callback.infrastructure.persistence;

import com.vycepay.callback.domain.model.SmsOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence for money-event SMS outbox retries.
 */
public interface SmsOutboxRepository extends JpaRepository<SmsOutbox, Long> {

    Optional<SmsOutbox> findByDedupeKey(String dedupeKey);

    @Query("""
            SELECT o FROM SmsOutbox o
            WHERE o.status = 'PENDING' AND o.nextAttemptAt <= :now
            ORDER BY o.nextAttemptAt ASC
            """)
    List<SmsOutbox> findDueForRetry(@Param("now") Instant now, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE SmsOutbox o
            SET o.status = 'PENDING', o.nextAttemptAt = :now, o.updatedAt = :now
            WHERE o.status = 'SENDING' AND o.updatedAt < :staleBefore
            """)
    int resetStuckSending(@Param("staleBefore") Instant staleBefore, @Param("now") Instant now);
}
