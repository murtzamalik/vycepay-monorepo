package com.vycepay.callback.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.callback.domain.model.ChoiceBankCallback;
import com.vycepay.callback.domain.port.NotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles 0015 - File job complete (account statement download URL per Choice Bank docs).
 */
@Component
public class AccountStatementFileJobHandler implements NotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(AccountStatementFileJobHandler.class);
    private static final String NOTIFICATION_TYPE = "0015";

    private final StatementJobCallbackUpdater updater;
    private final ObjectMapper objectMapper;

    public AccountStatementFileJobHandler(StatementJobCallbackUpdater updater, ObjectMapper objectMapper) {
        this.updater = updater;
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
        updater.updateFromParams(params, callback.getChoiceRequestId());
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
}
