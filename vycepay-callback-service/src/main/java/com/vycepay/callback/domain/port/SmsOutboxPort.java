package com.vycepay.callback.domain.port;

import com.vycepay.callback.domain.model.SmsOutbox;

import java.util.List;

/**
 * Money-event SMS outbox: park provider failures and retry lifecycle.
 */
public interface SmsOutboxPort {

    void parkFailed(Long customerId, Long notificationId, String dedupeKey,
                    String recipient, String messageBody, String errorMessage);

    List<SmsOutbox> claimDue(int limit);

    void markSent(Long id, String providerUid);

    void markRetryOrDead(Long id, String errorMessage);

    void deferDisabled(Long id);
}
