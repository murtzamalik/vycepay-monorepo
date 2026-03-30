package com.vycepay.callback.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.callback.domain.model.ChoiceBankCallback;
import com.vycepay.callback.domain.model.Transaction;
import com.vycepay.callback.domain.port.NotificationHandler;
import com.vycepay.callback.infrastructure.persistence.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Handles 0002 - Transaction Result Notification.
 * Updates transaction status, error details, completion time.
 */
@Component
public class TransactionResultHandler implements NotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(TransactionResultHandler.class);
    private static final String NOTIFICATION_TYPE = "0002";

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    public TransactionResultHandler(TransactionRepository transactionRepository, ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getNotificationType() {
        return NOTIFICATION_TYPE;
    }

    @Override
    public void handle(ChoiceBankCallback callback) {
        Map<String, Object> params = parseParams(callback.getRawPayload());
        if (params == null) return;

        String txId = firstNonBlank(
                getString(params, "txId"),
                getString(params, "batchId"),
                getString(params, "utilityTxId"));
        if (txId == null || txId.isBlank()) {
            log.warn("Transaction callback missing txId/batchId/utilityTxId");
            return;
        }
        Integer txStatus = getInt(params, "txStatus");
        String errorCode = getString(params, "errorCode");
        String errorMsg = getString(params, "errorMsg");
        Long updateTime = getLong(params, "updateTime");

        Optional<Transaction> opt = transactionRepository.findByChoiceTxId(txId)
                .or(() -> transactionRepository.findByChoiceRequestId(txId));

        opt.ifPresentOrElse(
                tx -> {
                    tx.setStatus(txStatus != null ? String.valueOf(txStatus) : null);
                    tx.setErrorCode(errorCode);
                    tx.setErrorMsg(errorMsg);
                    tx.setCompletedAt(updateTime != null ? Instant.ofEpochMilli(updateTime) : Instant.now());
                    transactionRepository.save(tx);
                    log.info("Updated transaction choiceTxId={} status={}", txId, txStatus);
                },
                () -> log.warn("Transaction not found for txId={}", txId)
        );
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

    private String getString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v != null ? v.toString() : null;
    }

    private Integer getInt(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long getLong(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
