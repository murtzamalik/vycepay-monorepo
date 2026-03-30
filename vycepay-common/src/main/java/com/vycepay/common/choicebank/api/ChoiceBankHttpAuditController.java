package com.vycepay.common.choicebank.api;

import com.vycepay.common.choicebank.ChoiceBankHttpAuditStore;
import com.vycepay.common.choicebank.dto.ChoiceBankHttpTraceDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only view of recent outbound Choice BaaS HTTP calls for this service instance.
 * Unauthenticated: configure Spring Security to permit {@code /internal/choice-bank/**}.
 */
@RestController
@RequestMapping(value = "/internal/choice-bank", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnBean(ChoiceBankHttpAuditStore.class)
public class ChoiceBankHttpAuditController {

    private final ChoiceBankHttpAuditStore auditStore;

    public ChoiceBankHttpAuditController(ChoiceBankHttpAuditStore auditStore) {
        this.auditStore = auditStore;
    }

    /**
     * Returns recent traces (newest first). In-memory only; not shared across replicas.
     */
    @GetMapping("/http-traces")
    public List<ChoiceBankHttpTraceDto> httpTraces() {
        return auditStore.getRecentTraces();
    }
}
