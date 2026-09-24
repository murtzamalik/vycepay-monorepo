package com.vycepay.callback.application.notification;

import com.vycepay.callback.domain.model.SmsOutbox;
import com.vycepay.callback.domain.port.SmsOutboxPort;
import com.vycepay.callback.infrastructure.persistence.SmsOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parks failed money-event SMS and supports claim / mark lifecycle for the retry job.
 */
@Service
public class SmsOutboxService implements SmsOutboxPort {

    private static final Logger log = LoggerFactory.getLogger(SmsOutboxService.class);
    private static final int ERROR_MAX = 255;
    private static final long STUCK_SENDING_MINUTES = 10;
    private static final long DISABLED_DEFER_MINUTES = 5;

    private final SmsOutboxRepository outboxRepository;
    private final int defaultMaxAttempts;

    public SmsOutboxService(SmsOutboxRepository outboxRepository,
                            @Value("${vycepay.sms.outbox.max-attempts:10}") int defaultMaxAttempts) {
        this.outboxRepository = outboxRepository;
        this.defaultMaxAttempts = defaultMaxAttempts > 0 ? defaultMaxAttempts : 10;
    }

    /**
     * Inserts or refreshes a PENDING outbox row for a provider FAILED send.
     * No-op when {@code dedupe_key} is already SENT. Idempotent on unique dedupe_key.
     */
    @Override
    @Transactional
    public void parkFailed(Long customerId, Long notificationId, String dedupeKey,
                           String recipient, String messageBody, String errorMessage) {
        if (dedupeKey == null || dedupeKey.isBlank()) {
            log.warn("SMS outbox park skipped: missing dedupeKey customerId={}", customerId);
            return;
        }
        if (recipient == null || recipient.isBlank() || messageBody == null || messageBody.isBlank()) {
            log.warn("SMS outbox park skipped: missing recipient/body dedupeKey={}", dedupeKey);
            return;
        }
        Instant now = Instant.now();
        Optional<SmsOutbox> existingOpt = outboxRepository.findByDedupeKey(dedupeKey.trim());
        if (existingOpt.isPresent()) {
            SmsOutbox existing = existingOpt.get();
            if (SmsOutbox.STATUS_SENT.equals(existing.getStatus())) {
                log.debug("SMS outbox park skipped (already SENT) dedupeKey={}", dedupeKey);
                return;
            }
            existing.setCustomerId(customerId);
            if (notificationId != null) {
                existing.setNotificationId(notificationId);
            }
            existing.setRecipient(recipient);
            existing.setMessageBody(truncate(messageBody, 640));
            existing.setStatus(SmsOutbox.STATUS_PENDING);
            existing.setNextAttemptAt(now);
            existing.setLastError(truncate(errorMessage, ERROR_MAX));
            existing.setMaxAttempts(defaultMaxAttempts);
            outboxRepository.save(existing);
            log.info("SMS outbox re-parked id={} dedupeKey={}", existing.getId(), dedupeKey);
            return;
        }

        SmsOutbox row = new SmsOutbox();
        row.setCustomerId(customerId);
        row.setNotificationId(notificationId);
        row.setDedupeKey(dedupeKey.trim());
        row.setRecipient(recipient);
        row.setMessageBody(truncate(messageBody, 640));
        row.setStatus(SmsOutbox.STATUS_PENDING);
        row.setAttemptCount(0);
        row.setMaxAttempts(defaultMaxAttempts);
        row.setNextAttemptAt(now);
        row.setLastError(truncate(errorMessage, ERROR_MAX));
        try {
            outboxRepository.save(row);
            log.info("SMS outbox parked dedupeKey={} customerId={}", dedupeKey, customerId);
        } catch (DataIntegrityViolationException e) {
            log.debug("SMS outbox park race dedupeKey={}: {}", dedupeKey, e.getMessage());
            outboxRepository.findByDedupeKey(dedupeKey.trim()).ifPresent(raced -> {
                if (!SmsOutbox.STATUS_SENT.equals(raced.getStatus())) {
                    raced.setRecipient(recipient);
                    raced.setMessageBody(truncate(messageBody, 640));
                    raced.setStatus(SmsOutbox.STATUS_PENDING);
                    raced.setNextAttemptAt(Instant.now());
                    raced.setLastError(truncate(errorMessage, ERROR_MAX));
                    outboxRepository.save(raced);
                }
            });
        }
    }

    /**
     * Resets stuck SENDING rows, then claims up to {@code limit} due PENDING rows as SENDING.
     */
    @Override
    @Transactional
    public List<SmsOutbox> claimDue(int limit) {
        Instant now = Instant.now();
        Instant staleBefore = now.minus(STUCK_SENDING_MINUTES, ChronoUnit.MINUTES);
        int reset = outboxRepository.resetStuckSending(staleBefore, now);
        if (reset > 0) {
            log.warn("SMS outbox reset {} stuck SENDING row(s)", reset);
        }
        int batch = limit > 0 ? limit : 50;
        List<SmsOutbox> due = outboxRepository.findDueForRetry(now, PageRequest.of(0, batch));
        List<SmsOutbox> claimed = new ArrayList<>();
        for (SmsOutbox row : due) {
            row.setStatus(SmsOutbox.STATUS_SENDING);
            claimed.add(outboxRepository.save(row));
        }
        return claimed;
    }

    @Override
    @Transactional
    public void markSent(Long id, String providerUid) {
        outboxRepository.findById(id).ifPresent(row -> {
            row.setStatus(SmsOutbox.STATUS_SENT);
            row.setProviderUid(providerUid);
            row.setSentAt(Instant.now());
            row.setLastError(null);
            outboxRepository.save(row);
        });
    }

    /**
     * Increments attempt; DEAD when at max, else PENDING with exponential backoff (cap 60 min).
     */
    @Override
    @Transactional
    public void markRetryOrDead(Long id, String errorMessage) {
        outboxRepository.findById(id).ifPresent(row -> {
            int nextAttempt = row.getAttemptCount() + 1;
            row.setAttemptCount(nextAttempt);
            row.setLastError(truncate(errorMessage, ERROR_MAX));
            if (nextAttempt >= row.getMaxAttempts()) {
                row.setStatus(SmsOutbox.STATUS_DEAD);
                log.warn("SMS outbox DEAD id={} dedupeKey={} attempts={}",
                        row.getId(), row.getDedupeKey(), nextAttempt);
            } else {
                long delayMinutes = Math.min(1L << Math.min(nextAttempt, 6), 60L);
                row.setStatus(SmsOutbox.STATUS_PENDING);
                row.setNextAttemptAt(Instant.now().plus(delayMinutes, ChronoUnit.MINUTES));
            }
            outboxRepository.save(row);
        });
    }

    /**
     * SMS disabled: keep PENDING, do not burn attempts, retry in 5 minutes.
     */
    @Override
    @Transactional
    public void deferDisabled(Long id) {
        outboxRepository.findById(id).ifPresent(row -> {
            row.setStatus(SmsOutbox.STATUS_PENDING);
            row.setNextAttemptAt(Instant.now().plus(DISABLED_DEFER_MINUTES, ChronoUnit.MINUTES));
            row.setLastError("SMS_DISABLED");
            outboxRepository.save(row);
        });
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
