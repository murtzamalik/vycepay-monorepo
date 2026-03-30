package com.vycepay.callback.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.callback.domain.model.ChoiceBankCallback;
import com.vycepay.callback.domain.port.NotificationHandler;
import com.vycepay.callback.infrastructure.persistence.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles 0021 - Account status change (e.g. dormant/active). Updates local wallet.status when accountId matches.
 */
@Component
public class AccountStatusChangeHandler implements NotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(AccountStatusChangeHandler.class);
    private static final String NOTIFICATION_TYPE = "0021";

    private final WalletRepository walletRepository;
    private final ObjectMapper objectMapper;

    public AccountStatusChangeHandler(WalletRepository walletRepository, ObjectMapper objectMapper) {
        this.walletRepository = walletRepository;
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
        String accountId = getString(params, "accountId");
        if (accountId == null || accountId.isBlank()) {
            log.warn("Account status callback missing accountId");
            return;
        }
        Integer accountStatus = getInt(params, "accountStatus");
        if (accountStatus == null) {
            accountStatus = getInt(params, "status");
        }
        Integer finalStatus = accountStatus;
        walletRepository.findByChoiceAccountId(accountId).ifPresentOrElse(
                w -> {
                    w.setStatus(mapWalletStatus(finalStatus));
                    walletRepository.save(w);
                    log.info("Updated wallet status for accountId={} choiceStatus={}", accountId, finalStatus);
                },
                () -> log.warn("Wallet not found for accountId={}", accountId)
        );
    }

    /**
     * Maps Choice numeric account status to stored wallet status string (extend when Choice publishes full enum).
     */
    private static String mapWalletStatus(Integer choiceStatus) {
        if (choiceStatus == null) {
            return null;
        }
        return switch (choiceStatus) {
            case 0 -> "NORMAL";
            case 1 -> "LOCKED";
            case 2 -> "CLOSED";
            default -> "STATUS_" + choiceStatus;
        };
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
