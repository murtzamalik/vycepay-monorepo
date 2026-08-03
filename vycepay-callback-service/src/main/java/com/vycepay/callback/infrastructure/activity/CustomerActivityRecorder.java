package com.vycepay.callback.infrastructure.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fail-soft writer for customer activity_log rows from callback handlers.
 * Never throws into callback processing.
 */
@Component
public class CustomerActivityRecorder {

    private static final Logger log = LoggerFactory.getLogger(CustomerActivityRecorder.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Inserts an activity_log row. Swallows all exceptions.
     */
    public void record(Long customerId, String action, String resourceType, String resourceId) {
        try {
            entityManager.createNativeQuery(
                            "INSERT INTO activity_log (customer_id, action, resource_type, resource_id, created_at) VALUES (?, ?, ?, ?, NOW())")
                    .setParameter(1, customerId)
                    .setParameter(2, action)
                    .setParameter(3, resourceType)
                    .setParameter(4, resourceId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Failed to record activity action={} resourceType={} resourceId={}: {}",
                    action, resourceType, resourceId, e.getMessage());
        }
    }
}
