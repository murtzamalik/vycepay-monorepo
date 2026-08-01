package com.vycepay.callback.infrastructure.persistence;

import com.vycepay.callback.domain.model.PushDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Persistence for FCM delivery attempts.
 */
public interface PushDeliveryLogRepository extends JpaRepository<PushDeliveryLog, Long> {

    List<PushDeliveryLog> findByNotificationIdOrderByCreatedAtDesc(Long notificationId);

    @Query("SELECT COUNT(d) FROM PushDeliveryLog d WHERE d.notificationId = :notificationId AND d.triggerSource = 'RESEND' AND d.createdAt >= :since")
    long countResendsSince(@Param("notificationId") Long notificationId, @Param("since") Instant since);
}
