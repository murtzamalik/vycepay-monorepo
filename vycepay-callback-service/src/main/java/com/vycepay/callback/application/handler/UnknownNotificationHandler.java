package com.vycepay.callback.application.handler;

import com.vycepay.callback.domain.model.ChoiceBankCallback;
import com.vycepay.callback.domain.port.NotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback handler for unknown or unsupported notification types.
 * Logs and marks as processed without throwing.
 */
@Component
public class UnknownNotificationHandler implements NotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(UnknownNotificationHandler.class);

    @Override
    public String getNotificationType() {
        return "UNKNOWN";
    }

    @Override
    public void handle(ChoiceBankCallback callback) {
        log.info("Received unknown notification type: requestId={}, type={}",
                callback.getChoiceRequestId(), callback.getNotificationType());
    }
}
