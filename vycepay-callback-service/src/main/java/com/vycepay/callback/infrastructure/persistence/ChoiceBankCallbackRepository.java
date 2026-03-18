package com.vycepay.callback.infrastructure.persistence;

import com.vycepay.callback.domain.model.ChoiceBankCallback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for Choice Bank callback audit records.
 */
public interface ChoiceBankCallbackRepository extends JpaRepository<ChoiceBankCallback, Long> {

    /**
     * Finds existing callback by Choice request ID and notification type for idempotency.
     *
     * @param choiceRequestId  Request ID from Choice payload
     * @param notificationType Notification type (e.g. 0001, 0002)
     * @return Existing callback if present
     */
    Optional<ChoiceBankCallback> findByChoiceRequestIdAndNotificationType(String choiceRequestId, String notificationType);
}
