package com.vycepay.activity.api.v1;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vycepay.activity.application.service.ActivityService;
import com.vycepay.activity.domain.model.ActivityLog;
import com.vycepay.activity.infrastructure.persistence.ActivityLogRepository;
import com.vycepay.activity.infrastructure.persistence.CustomerRepository;
import com.vycepay.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Activity/audit log API. Internal or admin use.
 */
@RestController
@RequestMapping("/api/v1/activity")
public class ActivityController {

    private final ActivityService activityService;
    private final ActivityLogRepository activityLogRepository;
    private final CustomerRepository customerRepository;

    public ActivityController(ActivityService activityService,
                              ActivityLogRepository activityLogRepository,
                              CustomerRepository customerRepository) {
        this.activityService = activityService;
        this.activityLogRepository = activityLogRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Logs an action. Called by other services or from request context.
     */
    @PostMapping("/log")
    public ResponseEntity<Void> log(
            @RequestHeader(value = "X-Customer-Id", required = false) String externalId,
            @RequestBody LogRequest request) {
        Long customerId = externalId != null
                ? customerRepository.findByExternalId(externalId).map(c -> c.getId()).orElse(null)
                : null;
        activityService.log(customerId, request.getAction(), request.getResourceType(),
                request.getResourceId(), request.getIpAddress(), request.getUserAgent(),
                request.getDeviceId());
        return ResponseEntity.ok().build();
    }

    /**
     * Lists activity for customer.
     */
    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> list(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(
                activityLogRepository.findByCustomerIdOrderByCreatedAtDesc(
                        customer.getId(), PageRequest.of(page, size))
                        .map(this::toMap));
    }

    private Map<String, Object> toMap(ActivityLog log) {
        return Map.of(
                "id", log.getId(),
                "action", log.getAction(),
                "resourceType", log.getResourceType() != null ? log.getResourceType() : "",
                "resourceId", log.getResourceId() != null ? log.getResourceId() : "",
                "createdAt", log.getCreatedAt().toString());
    }

    public static class LogRequest {
        private String action;
        private String resourceType;
        private String resourceId;
        private String ipAddress;
        private String userAgent;
        private String deviceId;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getResourceId() {
            return resourceId;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }
    }
}
