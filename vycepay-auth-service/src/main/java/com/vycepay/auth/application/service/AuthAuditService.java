package com.vycepay.auth.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.vycepay.auth.domain.model.AuthAuditEvent;
import com.vycepay.auth.infrastructure.persistence.AuthAuditEventRepository;

/**
 * Persists auth security audit events (no secrets).
 */
@Service
public class AuthAuditService {

    private static final Logger log = LoggerFactory.getLogger(AuthAuditService.class);

    private final AuthAuditEventRepository repository;

    public AuthAuditService(AuthAuditEventRepository repository) {
        this.repository = repository;
    }

    public void record(Long customerId, String eventType, String outcome, String identifierMasked, String detail) {
        try {
            AuthAuditEvent event = new AuthAuditEvent();
            event.setCustomerId(customerId);
            event.setEventType(eventType);
            event.setOutcome(outcome);
            event.setIdentifierMasked(identifierMasked);
            event.setDetail(detail);
            repository.save(event);
        } catch (Exception e) {
            log.warn("Failed to persist auth audit event type={} outcome={}", eventType, outcome, e);
        }
        log.info("auth_audit eventType={} outcome={} customerId={} identifier={}",
                eventType, outcome, customerId, identifierMasked);
    }
}
