package com.vycepay.activity.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.vycepay.activity.domain.model.ActivityLog;

/**
 * Repository for activity log records.
 */
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
}
