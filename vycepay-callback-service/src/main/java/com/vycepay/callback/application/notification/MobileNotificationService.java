package com.vycepay.callback.application.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.callback.domain.model.Customer;
import com.vycepay.callback.domain.model.CustomerNotification;
import com.vycepay.callback.infrastructure.persistence.CustomerNotificationRepository;
import com.vycepay.callback.infrastructure.persistence.CustomerRepository;
import com.vycepay.common.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mobile-facing inbox operations scoped by customer external_id from BFF.
 */
@Service
public class MobileNotificationService {

    private final CustomerRepository customerRepository;
    private final CustomerNotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public MobileNotificationService(CustomerRepository customerRepository,
                                     CustomerNotificationRepository notificationRepository,
                                     ObjectMapper objectMapper) {
        this.customerRepository = customerRepository;
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> list(String externalId, int page, int size) {
        Long customerId = resolveCustomerId(externalId);
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 50);
        Page<CustomerNotification> result = notificationRepository
                .findByCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(customerId, PageRequest.of(p, s));
        List<Map<String, Object>> items = result.getContent().stream().map(this::toMobileDto).toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("page", p);
        out.put("size", s);
        out.put("totalElements", result.getTotalElements());
        out.put("totalPages", result.getTotalPages());
        return out;
    }

    public Map<String, Object> unreadCount(String externalId) {
        Long customerId = resolveCustomerId(externalId);
        long count = notificationRepository.countByCustomerIdAndDeletedAtIsNullAndReadAtIsNull(customerId);
        return Map.of("unreadCount", count);
    }

    @Transactional
    public Map<String, Object> markRead(String externalId, String publicId) {
        Long customerId = resolveCustomerId(externalId);
        CustomerNotification n = notificationRepository
                .findByPublicIdAndCustomerIdAndDeletedAtIsNull(publicId, customerId)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND",
                        "Notification not found", HttpStatus.NOT_FOUND));
        if (n.getReadAt() == null) {
            n.setReadAt(Instant.now());
            notificationRepository.save(n);
        }
        return toMobileDto(n);
    }

    @Transactional
    public void softDelete(String externalId, String publicId) {
        Long customerId = resolveCustomerId(externalId);
        CustomerNotification n = notificationRepository
                .findByPublicIdAndCustomerIdAndDeletedAtIsNull(publicId, customerId)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND",
                        "Notification not found", HttpStatus.NOT_FOUND));
        n.setDeletedAt(Instant.now());
        notificationRepository.save(n);
    }

    private Long resolveCustomerId(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            throw new BusinessException("UNAUTHORIZED", "Missing customer context", HttpStatus.UNAUTHORIZED);
        }
        return customerRepository.findByExternalId(externalId.trim())
                .map(Customer::getId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND",
                        "Customer not found", HttpStatus.NOT_FOUND));
    }

    private Map<String, Object> toMobileDto(CustomerNotification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getPublicId());
        m.put("pushType", n.getPushType());
        m.put("notificationType", n.getNotificationType());
        m.put("title", n.getTitle());
        m.put("body", n.getBody());
        m.put("data", parseData(n.getDataJson()));
        m.put("read", n.getReadAt() != null);
        m.put("readAt", n.getReadAt() != null ? n.getReadAt().toString() : null);
        m.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> parseData(String dataJson) {
        if (dataJson == null || dataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(dataJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
