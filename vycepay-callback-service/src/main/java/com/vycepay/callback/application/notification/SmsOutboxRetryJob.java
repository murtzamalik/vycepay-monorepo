package com.vycepay.callback.application.notification;

import com.vycepay.callback.domain.model.SmsOutbox;
import com.vycepay.callback.domain.port.SmsOutboxPort;
import com.vycepay.common.sms.port.SmsPort;
import com.vycepay.common.sms.port.SmsSendRequest;
import com.vycepay.common.sms.port.SmsSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Retries parked money-event SMS from {@code sms_outbox}. Soft-fail; does not touch OTP ledger.
 */
@Component
public class SmsOutboxRetryJob {

    private static final Logger log = LoggerFactory.getLogger(SmsOutboxRetryJob.class);

    private final SmsOutboxPort smsOutboxService;
    private final SmsPort smsPort;
    private final int batchSize;

    public SmsOutboxRetryJob(SmsOutboxPort smsOutboxService,
                             SmsPort smsPort,
                             @Value("${vycepay.sms.outbox.batch-size:50}") int batchSize) {
        this.smsOutboxService = smsOutboxService;
        this.smsPort = smsPort;
        this.batchSize = batchSize > 0 ? batchSize : 50;
    }

    @Scheduled(fixedDelayString = "${vycepay.sms.outbox.poll-interval-ms:60000}")
    public void pollAndRetry() {
        List<SmsOutbox> claimed;
        try {
            claimed = smsOutboxService.claimDue(batchSize);
        } catch (Exception e) {
            log.warn("SMS outbox claim failed: {}", e.getMessage());
            return;
        }
        if (claimed.isEmpty()) {
            return;
        }
        log.info("SMS outbox retry claiming {} row(s)", claimed.size());
        for (SmsOutbox row : claimed) {
            try {
                processOne(row);
            } catch (Exception e) {
                log.warn("SMS outbox retry error id={}: {}", row.getId(), e.getMessage());
                try {
                    smsOutboxService.markRetryOrDead(row.getId(), e.getMessage());
                } catch (Exception markEx) {
                    log.warn("SMS outbox markRetryOrDead failed id={}: {}", row.getId(), markEx.getMessage());
                }
            }
        }
    }

    private void processOne(SmsOutbox row) {
        SmsSendResult result = smsPort.send(new SmsSendRequest(row.getRecipient(), row.getMessageBody()));
        if (result.isSent()) {
            smsOutboxService.markSent(row.getId(), result.providerUid());
            log.info("SMS outbox SENT id={} dedupeKey={} providerUid={}",
                    row.getId(), row.getDedupeKey(), result.providerUid());
            return;
        }
        if (SmsSendResult.SKIPPED.equals(result.status())) {
            smsOutboxService.deferDisabled(row.getId());
            log.info("SMS outbox deferred (disabled) id={} dedupeKey={}", row.getId(), row.getDedupeKey());
            return;
        }
        smsOutboxService.markRetryOrDead(row.getId(), result.errorMessage());
        log.warn("SMS outbox retry FAILED id={} dedupeKey={} error={}",
                row.getId(), row.getDedupeKey(), result.errorMessage());
    }
}
