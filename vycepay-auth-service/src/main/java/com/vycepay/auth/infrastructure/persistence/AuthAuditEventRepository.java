package com.vycepay.auth.infrastructure.persistence;

import com.vycepay.auth.domain.model.AuthAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persists auth security audit events.
 */
public interface AuthAuditEventRepository extends JpaRepository<AuthAuditEvent, Long> {
}
