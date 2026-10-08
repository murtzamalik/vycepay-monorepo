package com.vycepay.admin.application.service;

import com.vycepay.admin.api.v1.dto.AdminRequests.SmsBulkRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.admin.config.AdminProperties;
import com.vycepay.admin.infrastructure.notification.CallbackNotificationClient;
import com.vycepay.admin.infrastructure.sms.AuthSmsClient;
import com.vycepay.admin.security.AdminPrincipal;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.sms.port.SmsBalanceResult;
import com.vycepay.common.sms.port.SmsPort;
import com.vycepay.common.sms.port.SmsSendRequest;
import com.vycepay.common.sms.port.SmsSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ALL_CUSTOMERS bulk SMS without Mockito (Java 27 ByteBuddy limitation).
 */
class AdminMutationServiceBulkSmsTest {

    private RecordingJdbcTemplate jdbcTemplate;
    private StubAudienceService audienceService;
    private StubDispatchService dispatchService;
    private StubAuditService auditService;
    private AdminMutationService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new RecordingJdbcTemplate();
        audienceService = new StubAudienceService(jdbcTemplate);
        dispatchService = new StubDispatchService(jdbcTemplate, new NoopSmsPort());
        auditService = new StubAuditService(jdbcTemplate);
        AdminSecurityContext securityContext = new AdminSecurityContext() {
            @Override
            public AdminPrincipal currentAdmin() {
                return new AdminPrincipal(9L, "ext", "ops", "ops@example.com", "Ops",
                        Set.of(), Set.of("sms:bulk"), "jti-1");
            }
        };
        PlatformTransactionManager txManager = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
        ObjectMapper mapper = new ObjectMapper();
        AdminProperties props = new AdminProperties();
        RestTemplateBuilder builder = new RestTemplateBuilder();
        service = new AdminMutationService(
                jdbcTemplate, securityContext, auditService, new BCryptPasswordEncoder(),
                new CallbackNotificationClient(builder, props, mapper),
                new AuthSmsClient(builder, props, mapper),
                new NoopSmsPort(), audienceService, dispatchService, txManager);
    }

    @Test
    void allCustomers_noRecipients_throws() {
        audienceService.next = new BulkSmsAudienceService.ResolvedAudience(2, 2, Collections.emptyList());
        SmsBulkRequest body = new SmsBulkRequest(
                "ALL_CUSTOMERS", null, "Hello all customers", "Ops broadcast reason");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.bulkSms(body, new MockHttpServletRequest()));
        assertEquals("SMS_NO_RECIPIENTS", ex.getCode());
        assertTrue(dispatchService.dispatchedBatches.isEmpty());
    }

    @Test
    void allCustomers_overCap_throws() {
        List<BulkSmsAudienceService.AudienceRecipient> tooMany = new ArrayList<>();
        for (int i = 0; i < BulkSmsAudienceService.MAX_ALL_CUSTOMERS + 1; i++) {
            tooMany.add(new BulkSmsAudienceService.AudienceRecipient(i + 1L, "254712345678"));
        }
        audienceService.next = new BulkSmsAudienceService.ResolvedAudience(tooMany.size(), 0, tooMany);
        SmsBulkRequest body = new SmsBulkRequest(
                "ALL_CUSTOMERS", null, "Hello all customers", "Ops broadcast reason");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.bulkSms(body, new MockHttpServletRequest()));
        assertEquals("SMS_AUDIENCE_TOO_LARGE", ex.getCode());
        assertTrue(dispatchService.dispatchedBatches.isEmpty());
    }

    @Test
    void allCustomers_enqueuesAndDispatchesAsync() {
        List<BulkSmsAudienceService.AudienceRecipient> recipients = List.of(
                new BulkSmsAudienceService.AudienceRecipient(1L, "254712345678"),
                new BulkSmsAudienceService.AudienceRecipient(2L, "254723456789"));
        audienceService.next = new BulkSmsAudienceService.ResolvedAudience(3, 1, recipients);

        SmsBulkRequest body = new SmsBulkRequest(
                "ALL_CUSTOMERS", null, "Hello all customers", "Ops broadcast reason");
        Map<String, Object> result = service.bulkSms(body, new MockHttpServletRequest());

        assertEquals("ALL_CUSTOMERS", result.get("audience"));
        assertEquals("ACCEPTED", result.get("status"));
        assertEquals(2, result.get("total"));
        assertEquals(1, result.get("skipped"));
        assertEquals(0, result.get("sent"));
        assertEquals(2, jdbcTemplate.insertCount);
        assertEquals(1, dispatchService.dispatchedBatches.size());
        assertEquals(9L, dispatchService.dispatchedBatches.get(0).adminId());
        assertEquals(1, auditService.actions.size());
        assertEquals("SMS_BULK", auditService.actions.get(0));
    }

    static final class StubAudienceService extends BulkSmsAudienceService {
        BulkSmsAudienceService.ResolvedAudience next;

        StubAudienceService(JdbcTemplate jdbcTemplate) {
            super(jdbcTemplate);
        }

        @Override
        public ResolvedAudience resolve() {
            return next;
        }
    }

    static final class StubDispatchService extends BulkSmsDispatchService {
        final List<DispatchCall> dispatchedBatches = new ArrayList<>();

        StubDispatchService(JdbcTemplate jdbcTemplate, SmsPort smsPort) {
            super(jdbcTemplate, smsPort);
        }

        @Override
        public void dispatchBatch(String batchId, Long adminId) {
            dispatchedBatches.add(new DispatchCall(batchId, adminId));
        }
    }

    record DispatchCall(String batchId, Long adminId) {
    }

    static final class StubAuditService extends AdminAuditService {
        final List<String> actions = new ArrayList<>();

        StubAuditService(JdbcTemplate jdbcTemplate) {
            super(jdbcTemplate);
        }

        @Override
        public void log(AdminPrincipal admin, String action, String entityType, String entityId,
                        String reason, String detail, jakarta.servlet.http.HttpServletRequest request) {
            actions.add(action);
        }
    }

    static final class RecordingJdbcTemplate extends JdbcTemplate {
        int insertCount;
        private final AtomicLong seq = new AtomicLong(100);

        @Override
        public int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder) {
            insertCount++;
            generatedKeyHolder.getKeyList().add(Map.of("GENERATED_KEY", seq.getAndIncrement()));
            return 1;
        }
    }

    static final class NoopSmsPort implements SmsPort {
        @Override
        public SmsSendResult send(SmsSendRequest request) {
            return SmsSendResult.sent("noop");
        }

        @Override
        public SmsBalanceResult balance() {
            return SmsBalanceResult.failed("noop");
        }
    }
}
