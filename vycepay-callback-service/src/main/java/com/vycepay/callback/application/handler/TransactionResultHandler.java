package com.vycepay.callback.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.callback.application.push.CallbackPushPublisher;
import com.vycepay.callback.application.service.InboundMoneyEventService;
import com.vycepay.callback.domain.model.ChoiceBankCallback;
import com.vycepay.callback.domain.model.Transaction;
import com.vycepay.callback.domain.port.NotificationHandler;
import com.vycepay.callback.infrastructure.persistence.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handles 0002 - Transaction Result Notification.
 * Updates transaction status when a local row exists; for unmatched credits (inbound Pay Bill),
 * upserts a DEPOSIT then sends TRANSACTION_RESULT push.
 * Paired 0003 is deduped by TX:{txId} in the notification orchestrator.
 */
@Component
public class TransactionResultHandler implements NotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(TransactionResultHandler.class);
    private static final String NOTIFICATION_TYPE = "0002";

    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;
    private final CallbackPushPublisher pushPublisher;
    private final InboundMoneyEventService inboundMoneyEventService;

    public TransactionResultHandler(TransactionRepository transactionRepository,
                                    ObjectMapper objectMapper,
                                    CallbackPushPublisher pushPublisher,
                                    InboundMoneyEventService inboundMoneyEventService) {
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
        this.pushPublisher = pushPublisher;
        this.inboundMoneyEventService = inboundMoneyEventService;
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
        String accountId = getString(params, "accountId");
        String choiceRequestId = parseEnvelopeRequestId(callback.getRawPayload());

        Optional<Transaction> opt = transactionRepository.findByChoiceTxId(txId)
                .or(() -> transactionRepository.findByChoiceRequestId(txId));

        if (opt.isPresent()) {
            Transaction tx = opt.get();
            tx.setStatus(txStatus != null ? String.valueOf(txStatus) : null);
            tx.setErrorCode(errorCode);
            tx.setErrorMsg(errorMsg);
            tx.setCompletedAt(updateTime != null ? Instant.ofEpochMilli(updateTime) : Instant.now());
            transactionRepository.save(tx);
            log.info("Updated transaction choiceTxId={} status={}", txId, txStatus);
            Map<String, Object> pushParams = new HashMap<>(params);
            if (tx.getExternalId() != null) {
                pushParams.put("externalId", tx.getExternalId());
            }
            pushPublisher.publishBestEffort(tx.getCustomerId(), NOTIFICATION_TYPE, pushParams, callback.getId());
            return;
        }

        log.info("Transaction not found for txId={}; attempting inbound upsert", txId);
        Optional<Transaction> created = inboundMoneyEventService.upsertInboundCredit(
                txId, accountId, choiceRequestId, params,
                txStatus != null ? String.valueOf(txStatus) : null);
        if (created.isEmpty()) {
            log.warn("Transaction not found and inbound upsert skipped for txId={}", txId);
            return;
        }
        Transaction tx = created.get();
        Map<String, Object> pushParams = new HashMap<>(params);
        pushParams.put("externalId", tx.getExternalId());
        pushPublisher.publishBestEffort(tx.getCustomerId(), NOTIFICATION_TYPE, pushParams, callback.getId());
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

    @SuppressWarnings("unchecked")
    private String parseEnvelopeRequestId(String rawPayload) {
        try {
            Map<String, Object> root = objectMapper.readValue(rawPayload, Map.class);
            Object v = root.get("requestId");
            return v != null ? v.toString() : null;
        } catch (Exception e) {
            return null;
        }
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

    private Long getLong(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
