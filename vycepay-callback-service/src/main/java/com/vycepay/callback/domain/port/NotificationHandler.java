package com.vycepay.callback.domain.port;

import com.vycepay.callback.domain.model.ChoiceBankCallback;

/**
 * Strategy interface for processing Choice Bank notification callbacks.
 * Each notification type (0001, 0002, 0003, etc.) has a dedicated handler implementation.
 */
public interface NotificationHandler {

    /**
     * Returns the notification type this handler supports (e.g. "0001", "0002").
     *
     * @return Notification type code
     */
    String getNotificationType();

    /**
     * Processes the callback. Must be idempotent - duplicate callbacks should be safe.
     *
     * @param callback Persisted callback entity with raw payload
     */
    void handle(ChoiceBankCallback callback);
}
