package com.vycepay.callback.infrastructure.push;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.vycepay.callback.domain.model.DeviceToken;
import com.vycepay.callback.domain.model.PushMessage;
import com.vycepay.callback.domain.port.PushNotificationPort;
import com.vycepay.callback.infrastructure.persistence.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Firebase Admin adapter: multicasts push to all device tokens for a customer.
 * When Firebase is disabled or unavailable, logs and no-ops.
 * Removes tokens that FCM reports as unregistered/invalid.
 */
@Component
public class FirebasePushAdapter implements PushNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushAdapter.class);
    private static final int MAX_TOKENS_PER_BATCH = 500;

    private final DeviceTokenRepository deviceTokenRepository;
    private final FirebaseMessaging firebaseMessaging;
    private final boolean enabled;

    public FirebasePushAdapter(DeviceTokenRepository deviceTokenRepository,
                               @Autowired(required = false) FirebaseMessaging firebaseMessaging,
                               @Value("${vycepay.firebase.enabled:false}") boolean enabled) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.firebaseMessaging = firebaseMessaging;
        this.enabled = enabled;
    }

    @Override
    public void sendToCustomer(Long customerId, PushMessage message) {
        if (customerId == null || message == null) {
            return;
        }
        if (!enabled || firebaseMessaging == null) {
            log.debug("Push skipped (firebase disabled): customerId={} pushType={}",
                    customerId, message.getPushType());
            return;
        }
        List<DeviceToken> devices = deviceTokenRepository.findByCustomerId(customerId);
        if (devices.isEmpty()) {
            log.debug("No device tokens for customerId={} pushType={}", customerId, message.getPushType());
            return;
        }
        List<String> tokens = devices.stream()
                .map(DeviceToken::getFcmToken)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();
        if (tokens.isEmpty()) {
            return;
        }
        try {
            for (int i = 0; i < tokens.size(); i += MAX_TOKENS_PER_BATCH) {
                List<String> batch = tokens.subList(i, Math.min(i + MAX_TOKENS_PER_BATCH, tokens.size()));
                sendBatch(customerId, message, batch);
            }
        } catch (Exception e) {
            log.error("FCM send failed customerId={} pushType={}: {}",
                    customerId, message.getPushType(), e.getMessage());
        }
    }

    private void sendBatch(Long customerId, PushMessage message, List<String> tokens)
            throws FirebaseMessagingException {
        MulticastMessage multicast = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(message.getTitle())
                        .setBody(message.getBody())
                        .build())
                .putAllData(message.getData())
                .addAllTokens(tokens)
                .build();
        BatchResponse response = firebaseMessaging.sendEachForMulticast(multicast);
        log.info("FCM sent customerId={} pushType={} success={} failure={}",
                customerId, message.getPushType(), response.getSuccessCount(), response.getFailureCount());
        pruneInvalidTokens(tokens, response.getResponses());
    }

    private void pruneInvalidTokens(List<String> tokens, List<SendResponse> responses) {
        List<String> toDelete = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);
            if (r.isSuccessful()) {
                continue;
            }
            FirebaseMessagingException ex = r.getException();
            if (ex == null) {
                continue;
            }
            MessagingErrorCode code = ex.getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED
                    || code == MessagingErrorCode.INVALID_ARGUMENT) {
                toDelete.add(tokens.get(i));
            }
        }
        for (String token : toDelete) {
            deviceTokenRepository.deleteByFcmToken(token);
            log.info("Removed invalid FCM token ending ...{}",
                    token.length() > 8 ? token.substring(token.length() - 8) : "****");
        }
    }
}
