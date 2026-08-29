package com.vycepay.callback.application.push;

import com.vycepay.callback.application.notification.NotificationOrchestrator;
import com.vycepay.callback.domain.model.PushMessage;
import com.vycepay.callback.domain.model.PushSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Best-effort async fan-out of callback-driven push notifications via {@link NotificationOrchestrator}.
 * Failures are logged and never rethrown to Choice Bank webhook processing.
 */
@Service
public class CallbackPushPublisher {

    private static final Logger log = LoggerFactory.getLogger(CallbackPushPublisher.class);

    private final PushMessageFactory pushMessageFactory;
    private final NotificationOrchestrator notificationOrchestrator;

    public CallbackPushPublisher(PushMessageFactory pushMessageFactory,
                                 NotificationOrchestrator notificationOrchestrator) {
        this.pushMessageFactory = pushMessageFactory;
        this.notificationOrchestrator = notificationOrchestrator;
    }

    /**
     * Builds and sends a push for the given Choice notification type.
     * No-op when customerId is null, type is unsupported, or factory returns null.
     * 0002/0003 money events are deduped by TX:{txId} in {@link NotificationOrchestrator}.
     */
    public void publishBestEffort(Long customerId, String notificationType, Map<String, Object> params) {
        publishBestEffort(customerId, notificationType, params, null);
    }

    /**
     * Same as {@link #publishBestEffort(Long, String, Map)} with optional Choice callback id link.
     */
    public void publishBestEffort(Long customerId, String notificationType, Map<String, Object> params,
                                  Long choiceCallbackId) {
        try {
            if (customerId == null) {
                log.debug("Push skipped: no customerId for notificationType={}", notificationType);
                notificationOrchestrator.recordSkipOnly(null, PushSendResult.SKIP_NO_CUSTOMER);
                return;
            }
            PushMessage message = pushMessageFactory.create(notificationType, params);
            if (message == null) {
                notificationOrchestrator.recordSkipOnly(customerId, PushSendResult.SKIP_UNSUPPORTED_TYPE);
                return;
            }
            notificationOrchestrator.createAndSendFromCallback(customerId, message, choiceCallbackId);
        } catch (Exception e) {
            log.error("Push publish failed customerId={} type={}: {}",
                    customerId, notificationType, e.getMessage());
        }
    }
}
