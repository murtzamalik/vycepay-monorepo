package com.vycepay.callback.application.push;

import com.vycepay.callback.domain.model.PushMessage;
import com.vycepay.callback.domain.port.PushNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Best-effort async fan-out of callback-driven push notifications.
 * Failures are logged and never rethrown to Choice Bank webhook processing.
 */
@Service
public class CallbackPushPublisher {

    private static final Logger log = LoggerFactory.getLogger(CallbackPushPublisher.class);

    private final PushMessageFactory pushMessageFactory;
    private final PushNotificationPort pushNotificationPort;

    public CallbackPushPublisher(PushMessageFactory pushMessageFactory,
                                 PushNotificationPort pushNotificationPort) {
        this.pushMessageFactory = pushMessageFactory;
        this.pushNotificationPort = pushNotificationPort;
    }

    /**
     * Builds and sends a push for the given Choice notification type.
     * No-op when customerId is null, type is unsupported (e.g. 0003), or factory returns null.
     */
    @Async
    public void publishBestEffort(Long customerId, String notificationType, Map<String, Object> params) {
        try {
            if (customerId == null) {
                log.debug("Push skipped: no customerId for notificationType={}", notificationType);
                return;
            }
            PushMessage message = pushMessageFactory.create(notificationType, params);
            if (message == null) {
                return;
            }
            pushNotificationPort.sendToCustomer(customerId, message);
        } catch (Exception e) {
            log.error("Push publish failed customerId={} type={}: {}",
                    customerId, notificationType, e.getMessage());
        }
    }
}
