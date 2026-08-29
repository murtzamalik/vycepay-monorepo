package com.vycepay.callback.application.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.callback.domain.model.Customer;
import com.vycepay.callback.domain.model.CustomerNotification;
import com.vycepay.callback.domain.model.PushDeliveryLog;
import com.vycepay.callback.domain.model.PushMessage;
import com.vycepay.callback.domain.model.PushSendResult;
import com.vycepay.callback.domain.port.PushNotificationPort;
import com.vycepay.callback.infrastructure.persistence.CustomerNotificationRepository;
import com.vycepay.callback.infrastructure.persistence.CustomerRepository;
import com.vycepay.callback.infrastructure.persistence.PushDeliveryLogRepository;
import com.vycepay.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Notification hub: persists inbox rows, sends FCM, and records delivery attempts.
 * Failures in persistence/send are logged and must not break Choice Bank webhook processing.
 */
@Service
public class NotificationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NotificationOrchestrator.class);

    public static final String PUSH_ADMIN_MESSAGE = "ADMIN_MESSAGE";
    public static final String PUSH_TRANSACTION_RESULT = "TRANSACTION_RESULT";
    private static final String DEDUPE_PREFIX_TX = "TX:";
    private static final int MAX_COMPOSE_RECIPIENTS = 100;
    private static final int MAX_RESENDS_PER_HOUR = 5;
    private static final int TITLE_MAX = 128;
    private static final int BODY_MAX = 512;

    private final CustomerNotificationRepository notificationRepository;
    private final PushDeliveryLogRepository deliveryLogRepository;
    private final CustomerRepository customerRepository;
    private final PushNotificationPort pushNotificationPort;
    private final ObjectMapper objectMapper;

    public NotificationOrchestrator(CustomerNotificationRepository notificationRepository,
                                    PushDeliveryLogRepository deliveryLogRepository,
                                    CustomerRepository customerRepository,
                                    PushNotificationPort pushNotificationPort,
                                    ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.deliveryLogRepository = deliveryLogRepository;
        this.customerRepository = customerRepository;
        this.pushNotificationPort = pushNotificationPort;
        this.objectMapper = objectMapper;
    }

    /**
     * Builds inbox + FCM for a callback-driven push. No-op when message is null.
     * Money events ({@code TRANSACTION_RESULT}) are deduped by Choice {@code txId}
     * so paired 0002/0003 callbacks produce a single inbox row and FCM send.
     */
    @Async
    public void createAndSendFromCallback(Long customerId, PushMessage message, Long choiceCallbackId) {
        try {
            if (customerId == null) {
                recordStandaloneSkip(null, PushSendResult.skipped(PushSendResult.SKIP_NO_CUSTOMER),
                        PushDeliveryLog.TRIGGER_AUTO, null);
                return;
            }
            if (message == null) {
                recordStandaloneSkip(customerId, PushSendResult.skipped(PushSendResult.SKIP_UNSUPPORTED_TYPE),
                        PushDeliveryLog.TRIGGER_AUTO, null);
                return;
            }
            String dedupeKey = resolveDedupeKey(message);
            if (dedupeKey != null) {
                Optional<CustomerNotification> existing =
                        notificationRepository.findByCustomerIdAndDedupeKey(customerId, dedupeKey);
                if (existing.isPresent()) {
                    log.debug("Push skipped (already notified) customerId={} dedupeKey={}", customerId, dedupeKey);
                    recordStandaloneSkip(customerId, PushSendResult.skipped(PushSendResult.SKIP_ALREADY_NOTIFIED),
                            PushDeliveryLog.TRIGGER_AUTO, null);
                    return;
                }
            }
            CustomerNotification notification;
            try {
                notification = persistInbox(
                        customerId,
                        CustomerNotification.SOURCE_CALLBACK,
                        message.getPushType(),
                        message.getNotificationType(),
                        truncate(message.getTitle(), TITLE_MAX),
                        truncate(message.getBody(), BODY_MAX),
                        message.getData(),
                        choiceCallbackId,
                        null,
                        null,
                        dedupeKey);
            } catch (DataIntegrityViolationException e) {
                log.debug("Push skipped (dedupe race) customerId={} dedupeKey={}: {}",
                        customerId, dedupeKey, e.getMessage());
                recordStandaloneSkip(customerId, PushSendResult.skipped(PushSendResult.SKIP_ALREADY_NOTIFIED),
                        PushDeliveryLog.TRIGGER_AUTO, null);
                return;
            }
            PushSendResult result = pushNotificationPort.sendToCustomer(customerId, message);
            recordDelivery(notification.getId(), customerId, result, PushDeliveryLog.TRIGGER_AUTO, null);
        } catch (Exception e) {
            log.error("createAndSendFromCallback failed customerId={}: {}", customerId, e.getMessage());
        }
    }

    /**
     * Records a skip without an inbox row (e.g. factory returned null / no customer before message build).
     */
    public void recordSkipOnly(Long customerId, String skipReason) {
        try {
            recordStandaloneSkip(customerId, PushSendResult.skipped(skipReason),
                    PushDeliveryLog.TRIGGER_AUTO, null);
        } catch (Exception e) {
            log.warn("Failed to record push skip: {}", e.getMessage());
        }
    }

    /**
     * Admin compose: create one inbox per customer and send FCM (same batch_id).
     */
    @Transactional
    public Map<String, Object> compose(List<Long> customerIds, String title, String body,
                                       Map<String, String> data, Long adminId) {
        if (customerIds == null || customerIds.isEmpty()) {
            throw new BusinessException("COMPOSE_EMPTY", "At least one customerId is required", HttpStatus.BAD_REQUEST);
        }
        if (customerIds.size() > MAX_COMPOSE_RECIPIENTS) {
            throw new BusinessException("COMPOSE_TOO_MANY",
                    "Maximum " + MAX_COMPOSE_RECIPIENTS + " recipients per request", HttpStatus.BAD_REQUEST);
        }
        String t = requireText(title, "title", TITLE_MAX);
        String b = requireText(body, "body", BODY_MAX);

        Set<Long> uniqueIds = new HashSet<>(customerIds);
        List<Customer> found = customerRepository.findByIdIn(uniqueIds);
        Set<Long> foundIds = found.stream().map(Customer::getId).collect(Collectors.toSet());
        List<Long> missing = uniqueIds.stream().filter(id -> !foundIds.contains(id)).sorted().toList();
        if (!missing.isEmpty()) {
            throw new BusinessException("CUSTOMERS_NOT_FOUND",
                    "Unknown customer ids: " + missing, HttpStatus.BAD_REQUEST);
        }

        String batchId = UUID.randomUUID().toString();
        Map<String, String> safeData = data != null ? new LinkedHashMap<>(data) : Map.of();
        List<Long> notificationIds = new ArrayList<>();
        List<Map<String, Object>> deliveries = new ArrayList<>();

        for (Long customerId : uniqueIds) {
            CustomerNotification notification = persistInbox(
                    customerId,
                    CustomerNotification.SOURCE_ADMIN_COMPOSE,
                    PUSH_ADMIN_MESSAGE,
                    null,
                    t,
                    b,
                    safeData,
                    null,
                    batchId,
                    adminId,
                    null);
            notificationIds.add(notification.getId());

            PushMessage.Builder builder = PushMessage.builder()
                    .title(t)
                    .body(b)
                    .pushType(PUSH_ADMIN_MESSAGE)
                    .notificationType("ADMIN");
            builder.putData("notificationId", notification.getPublicId());
            for (Map.Entry<String, String> e : safeData.entrySet()) {
                builder.putData(e.getKey(), e.getValue());
            }
            PushMessage message = builder.build();

            PushSendResult result = pushNotificationPort.sendToCustomer(customerId, message);
            recordDelivery(notification.getId(), customerId, result, PushDeliveryLog.TRIGGER_COMPOSE, adminId);
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("notificationId", notification.getId());
            d.put("publicId", notification.getPublicId());
            d.put("customerId", customerId);
            d.put("status", result.getStatus());
            deliveries.add(d);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("batchId", batchId);
        out.put("accepted", notificationIds.size());
        out.put("notificationIds", notificationIds);
        out.put("deliveries", deliveries);
        return out;
    }

    /**
     * Resend FCM for an existing inbox row. Rate-limited to 5 RESEND attempts per rolling hour.
     */
    @Transactional
    public Map<String, Object> resend(Long notificationId, Long adminId) {
        CustomerNotification notification = notificationRepository.findByIdAndDeletedAtIsNull(notificationId)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND",
                        "Notification not found", HttpStatus.NOT_FOUND));

        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        long resendCount = deliveryLogRepository.countResendsSince(notificationId, since);
        if (resendCount >= MAX_RESENDS_PER_HOUR) {
            throw new BusinessException("RESEND_RATE_LIMITED",
                    "Maximum " + MAX_RESENDS_PER_HOUR + " resends per hour for this notification",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        Map<String, String> data = parseData(notification.getDataJson());
        PushMessage.Builder builder = PushMessage.builder()
                .title(notification.getTitle())
                .body(notification.getBody())
                .pushType(notification.getPushType())
                .notificationType(notification.getNotificationType() != null
                        ? notification.getNotificationType() : "ADMIN");
        builder.putData("notificationId", notification.getPublicId());
        for (Map.Entry<String, String> e : data.entrySet()) {
            builder.putData(e.getKey(), e.getValue());
        }
        PushMessage message = builder.build();

        PushSendResult result = pushNotificationPort.sendToCustomer(notification.getCustomerId(), message);
        PushDeliveryLog logRow = recordDelivery(notification.getId(), notification.getCustomerId(),
                result, PushDeliveryLog.TRIGGER_RESEND, adminId);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("notificationId", notification.getId());
        out.put("status", result.getStatus());
        out.put("skipReason", result.getSkipReason());
        out.put("successCount", result.getSuccessCount());
        out.put("failureCount", result.getFailureCount());
        out.put("deliveryLogId", logRow != null ? logRow.getId() : null);
        return out;
    }

    private CustomerNotification persistInbox(Long customerId, String source, String pushType,
                                              String notificationType, String title, String body,
                                              Map<String, String> data, Long choiceCallbackId,
                                              String batchId, Long adminId, String dedupeKey) {
        CustomerNotification n = new CustomerNotification();
        n.setCustomerId(customerId);
        n.setSource(source);
        n.setPushType(pushType);
        n.setNotificationType(notificationType);
        n.setTitle(title);
        n.setBody(body);
        n.setDataJson(toJson(data));
        n.setChoiceCallbackId(choiceCallbackId);
        n.setBatchId(batchId);
        n.setCreatedByAdminId(adminId);
        n.setDedupeKey(dedupeKey);
        return notificationRepository.save(n);
    }

    /**
     * Builds TX:{txId} for TRANSACTION_RESULT pushes; null for all other types.
     */
    private static String resolveDedupeKey(PushMessage message) {
        if (message == null || !PUSH_TRANSACTION_RESULT.equals(message.getPushType())) {
            return null;
        }
        Map<String, String> data = message.getData();
        if (data == null) {
            return null;
        }
        String txId = data.get("txId");
        if (txId == null || txId.isBlank() || "null".equalsIgnoreCase(txId)) {
            return null;
        }
        return DEDUPE_PREFIX_TX + txId.trim();
    }

    private PushDeliveryLog recordDelivery(Long notificationId, Long customerId, PushSendResult result,
                                           String triggerSource, Long adminId) {
        try {
            PushDeliveryLog row = new PushDeliveryLog();
            row.setNotificationId(notificationId);
            row.setCustomerId(customerId);
            row.setStatus(result.getStatus());
            row.setSkipReason(result.getSkipReason());
            row.setTokenCount(result.getTokenCount());
            row.setSuccessCount(result.getSuccessCount());
            row.setFailureCount(result.getFailureCount());
            row.setErrorMessage(result.getErrorMessage());
            row.setTriggerSource(triggerSource);
            row.setCreatedByAdminId(adminId);
            return deliveryLogRepository.save(row);
        } catch (Exception e) {
            log.warn("Failed to persist push_delivery_log: {}", e.getMessage());
            return null;
        }
    }

    private void recordStandaloneSkip(Long customerId, PushSendResult result,
                                      String triggerSource, Long adminId) {
        recordDelivery(null, customerId, result, triggerSource, adminId);
    }

    private String toJson(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseData(String dataJson) {
        if (dataJson == null || dataJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(dataJson, Map.class);
            Map<String, String> out = new HashMap<>();
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    out.put(e.getKey(), e.getValue().toString());
                }
            }
            return out;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static String requireText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("INVALID_" + field.toUpperCase(),
                    field + " is required", HttpStatus.BAD_REQUEST);
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new BusinessException("INVALID_" + field.toUpperCase(),
                    field + " max length is " + max, HttpStatus.BAD_REQUEST);
        }
        return trimmed;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
