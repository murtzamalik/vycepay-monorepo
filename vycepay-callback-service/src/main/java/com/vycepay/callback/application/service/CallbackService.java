package com.vycepay.callback.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.callback.domain.model.ChoiceBankCallback;
import com.vycepay.callback.domain.port.NotificationHandler;
import com.vycepay.callback.infrastructure.persistence.ChoiceBankCallbackRepository;
import com.vycepay.common.choicebank.ChoiceBankCallbackSignatureVerifier;
import com.vycepay.common.choicebank.dto.ChoiceBankCallbackPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Receives Choice Bank callbacks, persists for audit, and routes to handlers by notification type.
 * Uses Strategy pattern: each notificationType has a dedicated handler.
 * Verifies callback signature when vycepay.callback.verify-signature=true.
 * Always returns quickly to allow HTTP 200 response; processing is async.
 */
@Service
public class CallbackService {

    private static final Logger log = LoggerFactory.getLogger(CallbackService.class);

    private final ChoiceBankCallbackRepository callbackRepository;
    private final Map<String, NotificationHandler> handlers;
    private final NotificationHandler unknownHandler;
    private final ObjectMapper objectMapper;
    private final ChoiceBankCallbackSignatureVerifier signatureVerifier;

    public CallbackService(ChoiceBankCallbackRepository callbackRepository,
                           List<NotificationHandler> handlerList,
                           ObjectMapper objectMapper,
                           @Autowired(required = false) ChoiceBankCallbackSignatureVerifier signatureVerifier) {
        this.callbackRepository = callbackRepository;
        this.objectMapper = objectMapper;
        this.signatureVerifier = signatureVerifier;
        this.handlers = new ConcurrentHashMap<>();
        NotificationHandler unknown = null;
        for (NotificationHandler h : handlerList) {
            if ("UNKNOWN".equals(h.getNotificationType())) {
                unknown = h;
            } else {
                handlers.put(h.getNotificationType(), h);
            }
        }
        this.unknownHandler = unknown != null ? unknown : new NoOpHandler();
    }

    /**
     * Verifies signature (if enabled), persists raw payload, and processes asynchronously.
     * Duplicate (requestId, notificationType) are idempotent - existing record is returned.
     * When signature verification fails, skips persist and process but caller still returns "ok".
     *
     * @param rawPayload Raw JSON from Choice Bank webhook
     * @return true if accepted (verified or verification disabled), false if signature invalid
     */
    public boolean receiveAndProcess(String rawPayload) {
        if (signatureVerifier != null) {
            try {
                ChoiceBankCallbackPayload payload = objectMapper.readValue(rawPayload, ChoiceBankCallbackPayload.class);
                if (!signatureVerifier.verify(payload)) {
                    log.warn("Rejecting callback with invalid signature");
                    return false;
                }
            } catch (Exception e) {
                log.error("Failed to parse or verify callback", e);
                return false;
            }
        }
        ChoiceBankCallback stored = persist(rawPayload);
        processAsync(stored);
        return true;
    }

    private ChoiceBankCallback persist(String rawPayload) {
        try {
            Map<String, Object> root = objectMapper.readValue(rawPayload, Map.class);
            String requestId = getString(root, "requestId");
            String notificationType = getString(root, "notificationType");
            if (notificationType == null) notificationType = "UNKNOWN";

            Optional<ChoiceBankCallback> existing = callbackRepository
                    .findByChoiceRequestIdAndNotificationType(requestId, notificationType);
            if (existing.isPresent()) {
                log.debug("Duplicate callback ignored: requestId={} type={}", requestId, notificationType);
                return existing.get();
            }

            ChoiceBankCallback callback = ChoiceBankCallback.builder()
                    .choiceRequestId(requestId)
                    .notificationType(notificationType)
                    .rawPayload(rawPayload)
                    .processed(false)
                    .build();
            return callbackRepository.save(callback);
        } catch (Exception e) {
            log.error("Failed to persist callback, storing raw payload", e);
            ChoiceBankCallback fallback = ChoiceBankCallback.builder()
                    .choiceRequestId(null)
                    .notificationType("UNKNOWN")
                    .rawPayload(rawPayload)
                    .processed(false)
                    .build();
            return callbackRepository.save(fallback);
        }
    }

    @Async
    public void processAsync(ChoiceBankCallback callback) {
        try {
            NotificationHandler handler = handlers.getOrDefault(callback.getNotificationType(), unknownHandler);
            handler.handle(callback);

            callback.setProcessed(true);
            callback.setProcessedAt(Instant.now());
            callbackRepository.save(callback);
        } catch (Exception e) {
            log.error("Callback processing failed: id={} type={}", callback.getId(), callback.getNotificationType(), e);
            callback.setProcessingError(e.getMessage());
            callbackRepository.save(callback);
        }
    }

    private String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    private static class NoOpHandler implements NotificationHandler {
        @Override
        public String getNotificationType() {
            return "UNKNOWN";
        }

        @Override
        public void handle(ChoiceBankCallback callback) {
            log.info("No handler for notificationType={}", callback.getNotificationType());
        }
    }
}
