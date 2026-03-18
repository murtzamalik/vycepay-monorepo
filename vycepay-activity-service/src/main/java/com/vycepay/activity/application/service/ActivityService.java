package com.vycepay.activity.application.service;

import org.springframework.stereotype.Service;

import com.vycepay.activity.domain.model.ActivityLog;
import com.vycepay.activity.infrastructure.persistence.ActivityLogRepository;

/**
 * Logs customer actions for compliance audit trail.
 */
@Service
public class ActivityService {

    private final ActivityLogRepository repository;

    public ActivityService(ActivityLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Logs an action.
     *
     * @param customerId  Customer ID (null for anonymous)
     * @param action      Action name (e.g. LOGIN, SEND_MONEY, VIEW_BALANCE)
     * @param resourceType Resource type (e.g. TRANSACTION, WALLET)
     * @param resourceId  Resource ID
     * @param ipAddress   Client IP
     * @param userAgent   Client user agent
     * @param deviceId    Device ID
     */
    public void log(Long customerId, String action, String resourceType, String resourceId,
                    String ipAddress, String userAgent, String deviceId) {
        ActivityLog log = new ActivityLog();
        log.setCustomerId(customerId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setDeviceId(deviceId);
        repository.save(log);
    }
}
