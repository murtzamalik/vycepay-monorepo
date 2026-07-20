package com.vycepay.callback.domain.port;

import com.vycepay.callback.domain.model.PushMessage;

/**
 * Outbound port for sending FCM push notifications to a customer's devices.
 */
public interface PushNotificationPort {

    /**
     * Sends {@code message} to all registered FCM tokens for {@code customerId}.
     * Best-effort: implementations must not throw to callers for delivery failures.
     *
     * @param customerId VycePay internal customer id
     * @param message    title, body, and string data map
     */
    void sendToCustomer(Long customerId, PushMessage message);
}
