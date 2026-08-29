package com.vycepay.callback.infrastructure.persistence;

import com.vycepay.callback.domain.model.CustomerNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence for customer notification inbox rows.
 */
public interface CustomerNotificationRepository extends JpaRepository<CustomerNotification, Long> {

    Optional<CustomerNotification> findByPublicIdAndCustomerIdAndDeletedAtIsNull(String publicId, Long customerId);

    Optional<CustomerNotification> findByIdAndDeletedAtIsNull(Long id);

    Optional<CustomerNotification> findByCustomerIdAndDedupeKey(Long customerId, String dedupeKey);

    Page<CustomerNotification> findByCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    long countByCustomerIdAndDeletedAtIsNullAndReadAtIsNull(Long customerId);
}
