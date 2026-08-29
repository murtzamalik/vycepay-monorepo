package com.vycepay.callback.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.callback.application.push.CallbackPushPublisher;
import com.vycepay.callback.application.service.InboundMoneyEventService;
import com.vycepay.callback.domain.model.ChoiceBankCallback;
import com.vycepay.callback.domain.model.Transaction;
import com.vycepay.callback.domain.model.Wallet;
import com.vycepay.callback.domain.port.NotificationHandler;
import com.vycepay.callback.infrastructure.persistence.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handles 0003 - Balance Change Notification.
 * Updates wallet balance_cache and last_balance_update_at.
 * Upserts an inbound DEPOSIT when no local row exists for the Choice txId, then sends
 * TRANSACTION_RESULT push (deduped with 0002 via TX:{txId}).
 */
@Component
public class BalanceChangeHandler implements NotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(BalanceChangeHandler.class);
    private static final String NOTIFICATION_TYPE = "0003";

    private final WalletRepository walletRepository;
    private final ObjectMapper objectMapper;
    private final InboundMoneyEventService inboundMoneyEventService;
    private final CallbackPushPublisher pushPublisher;

    public BalanceChangeHandler(WalletRepository walletRepository,
                                ObjectMapper objectMapper,
                                InboundMoneyEventService inboundMoneyEventService,
                                CallbackPushPublisher pushPublisher) {
        this.walletRepository = walletRepository;
        this.objectMapper = objectMapper;
        this.inboundMoneyEventService = inboundMoneyEventService;
        this.pushPublisher = pushPublisher;
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

        String accountId = getString(params, "accountId");
        String balance = getString(params, "balance");
        Long completeTime = getLong(params, "completeTime");
        String txId = firstNonBlank(
                getString(params, "txId"),
                getString(params, "batchId"),
                getString(params, "utilityTxId"));
        String choiceRequestId = parseEnvelopeRequestId(callback.getRawPayload());

        Optional<Wallet> walletOpt = accountId != null
                ? walletRepository.findByChoiceAccountId(accountId)
                : Optional.empty();

        walletOpt.ifPresentOrElse(
                wallet -> {
                    try {
                        wallet.setBalanceCache(new BigDecimal(balance != null ? balance : "0"));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid balance format: {}", balance);
                    }
                    wallet.setLastBalanceUpdateAt(completeTime != null
                            ? Instant.ofEpochMilli(completeTime)
                            : Instant.now());
                    walletRepository.save(wallet);
                    log.info("Updated balance for accountId={} balance={}", accountId, balance);

                    Map<String, Object> pushParams = new HashMap<>(params);
                    if (txId != null && !txId.isBlank()) {
                        Optional<Transaction> tx = inboundMoneyEventService.upsertInboundCredit(
                                txId, accountId, choiceRequestId, params, Transaction.STATUS_SUCCESS);
                        tx.ifPresent(t -> pushParams.put("externalId", t.getExternalId()));
                    } else {
                        log.warn("Balance change missing txId; balance updated but no push for accountId={}",
                                accountId);
                        return;
                    }
                    pushPublisher.publishBestEffort(
                            wallet.getCustomerId(), NOTIFICATION_TYPE, pushParams, callback.getId());
                },
                () -> log.warn("Wallet not found for accountId={}", accountId)
        );
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
