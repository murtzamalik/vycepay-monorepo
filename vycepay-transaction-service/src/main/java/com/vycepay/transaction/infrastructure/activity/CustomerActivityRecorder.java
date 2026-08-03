package com.vycepay.transaction.infrastructure.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fail-soft writer for customer activity_log rows.
 * Never throws into the payment path — logging failures are warned only.
 */
@Component
public class CustomerActivityRecorder {

    private static final Logger log = LoggerFactory.getLogger(CustomerActivityRecorder.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Inserts an activity_log row. Swallows all exceptions.
     *
     * @param customerId   customer PK (nullable)
     * @param action       e.g. TRANSFER_CREATED
     * @param resourceType e.g. TRANSACTION
     * @param resourceId   external resource id
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
