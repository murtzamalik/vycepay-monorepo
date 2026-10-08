package com.vycepay.admin.application.service;

import com.vycepay.admin.config.BulkSmsAsyncConfig;
import com.vycepay.common.sms.port.SmsPort;
import com.vycepay.common.sms.port.SmsSendRequest;
import com.vycepay.common.sms.port.SmsSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Async provider send for ADMIN_BULK rows enqueued as PENDING (all-customers path).
 * Failures do not abort the batch; each row is updated independently.
 */
@Service
public class BulkSmsDispatchService {

    private static final Logger log = LoggerFactory.getLogger(BulkSmsDispatchService.class);

    private final JdbcTemplate jdbcTemplate;
    private final SmsPort smsPort;

    public BulkSmsDispatchService(JdbcTemplate jdbcTemplate, SmsPort smsPort) {
        this.jdbcTemplate = jdbcTemplate;
        this.smsPort = smsPort;
    }

    /**
     * Sends all PENDING sms_message rows for the batch. Captures adminId on the caller thread.
     */
    @Async(BulkSmsAsyncConfig.BULK_SMS_EXECUTOR)
    public void dispatchBatch(String batchId, Long adminId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, recipient, message_body messageBody FROM sms_message "
                        + "WHERE batch_id=? AND status='PENDING' AND purpose='ADMIN_BULK' ORDER BY id ASC",
                batchId);
        log.info("Bulk SMS async dispatch starting batchId={} pending={}", batchId, rows.size());
        int sent = 0;
        int failed = 0;
        int skipped = 0;
        for (Map<String, Object> row : rows) {
            Long smsId = ((Number) row.get("id")).longValue();
            String recipient = (String) row.get("recipient");
            String messageBody = (String) row.get("messageBody");
            try {
                SmsSendResult result = smsPort.send(new SmsSendRequest(recipient, messageBody));
                applyBulkResult(smsId, result, "BULK", adminId);
                if (SmsSendResult.SENT.equals(result.status())) {
                    sent++;
                } else if (SmsSendResult.SKIPPED.equals(result.status())) {
                    skipped++;
                } else {
                    failed++;
                }
            } catch (Exception ex) {
                log.warn("Bulk SMS send failed batchId={} smsId={}: {}", batchId, smsId, ex.getMessage());
                applyBulkResult(smsId, SmsSendResult.failed(ex.getMessage()), "BULK", adminId);
                failed++;
            }
        }
        log.info("Bulk SMS async dispatch finished batchId={} sent={} failed={} skipped={}",
                batchId, sent, failed, skipped);
    }

    /**
     * Updates sms_message status and appends a delivery attempt (shared by sync MANUAL path).
     */
    public void applyBulkResult(Long smsId, SmsSendResult result, String triggerSource, Long adminId) {
        if (result.isSent()) {
            jdbcTemplate.update(
                    "UPDATE sms_message SET status=?, provider_uid=?, error_message=NULL, sent_at=CURRENT_TIMESTAMP WHERE id=?",
                    result.status(), result.providerUid(), smsId);
        } else {
            jdbcTemplate.update(
                    "UPDATE sms_message SET status=?, provider_uid=?, error_message=? WHERE id=?",
                    result.status(), result.providerUid(), result.errorMessage(), smsId);
        }
        jdbcTemplate.update(
                "INSERT INTO sms_delivery_attempt (sms_message_id, trigger_source, status, provider_uid, error_message, created_by_admin_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                smsId, triggerSource, result.status(), result.providerUid(), result.errorMessage(), adminId);
    }
}
