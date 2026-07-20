package com.vycepay.callback.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.callback.application.push.CallbackPushPublisher;
import com.vycepay.callback.domain.model.ChoiceBankCallback;
import com.vycepay.callback.domain.port.NotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles 0009 - Account statement generation result (legacy periodic statement callback).
 * Sends STATEMENT_READY push when a download URL is present.
 */
@Component
public class AccountStatementResultHandler implements NotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(AccountStatementResultHandler.class);
    private static final String NOTIFICATION_TYPE = "0009";

    private final StatementJobCallbackUpdater updater;
    private final ObjectMapper objectMapper;
    private final CallbackPushPublisher pushPublisher;

    public AccountStatementResultHandler(StatementJobCallbackUpdater updater,
                                         ObjectMapper objectMapper,
                                         CallbackPushPublisher pushPublisher) {
        this.updater = updater;
        this.objectMapper = objectMapper;
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
        updater.updateFromParams(params, callback.getChoiceRequestId()).ifPresent(job ->
                pushPublisher.publishBestEffort(job.getCustomerId(), NOTIFICATION_TYPE, params));
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
