package com.vycepay.callback.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.callback.domain.model.AccountStatementJob;
import com.vycepay.callback.domain.model.ChoiceBankCallback;
import com.vycepay.callback.domain.port.NotificationHandler;
import com.vycepay.callback.infrastructure.persistence.AccountStatementJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles 0009 - Account statement generation result (periodic statement file ready).
 * Confirms exact field names with Choice Bank docs; common keys: requestId, fileUrl, status.
 */
@Component
public class AccountStatementResultHandler implements NotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(AccountStatementResultHandler.class);
    private static final String NOTIFICATION_TYPE = "0009";

    private final AccountStatementJobRepository statementJobRepository;
    private final ObjectMapper objectMapper;

    public AccountStatementResultHandler(AccountStatementJobRepository statementJobRepository,
                                         ObjectMapper objectMapper) {
        this.statementJobRepository = statementJobRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getNotificationType() {
        return NOTIFICATION_TYPE;
    }

    @Override
    public void handle(ChoiceBankCallback callback) {
        Map<String, Object> params = parseParams(callback.getRawPayload());
        if (params == null) {
            return;
        }
        String requestId = firstNonBlank(
                getString(params, "requestId"),
                getString(params, "statementRequestId"),
                callback.getChoiceRequestId());
        if (requestId == null || requestId.isBlank()) {
            log.warn("Statement callback missing request id");
            return;
        }
        statementJobRepository.findByChoiceRequestId(requestId).ifPresentOrElse(job -> {
            String fileUrl = firstNonBlank(
                    getString(params, "fileUrl"),
                    getString(params, "downloadUrl"),
                    getString(params, "url"));
            String fileName = getString(params, "fileName");
            Integer st = getInt(params, "status");
            String err = getString(params, "errorMsg");
            if (fileUrl != null && !fileUrl.isBlank()) {
                job.setDownloadUrl(fileUrl);
                job.setStatus(AccountStatementJob.STATUS_READY);
            } else if (err != null && !err.isBlank()) {
                job.setErrorMsg(err);
                job.setStatus(AccountStatementJob.STATUS_FAILED);
            } else if (st != null && st != 0) {
                job.setStatus(AccountStatementJob.STATUS_READY);
            }
            if (fileName != null) {
                job.setFileName(fileName);
            }
            statementJobRepository.save(job);
            log.info("Updated account statement job requestId={} status={}", requestId, job.getStatus());
        }, () -> log.warn("No local statement job for requestId={}", requestId));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseParams(String rawPayload) {
        try {
            Map<String, Object> root = objectMapper.readValue(rawPayload, Map.class);
            return (Map<String, Object>) root.get("params");
        } catch (Exception e) {
            log.error("Failed to parse callback params", e);
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String s : values) {
            if (s != null && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }

    private String getString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v != null ? v.toString() : null;
    }

    private Integer getInt(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
