package com.vycepay.auth.application.service;

import com.vycepay.auth.domain.model.OtpPurpose;
import com.vycepay.auth.domain.model.OtpVerification;
import com.vycepay.auth.domain.model.SmsMessage;
import com.vycepay.auth.infrastructure.persistence.OtpVerificationRepository;
import com.vycepay.auth.infrastructure.persistence.SmsMessageRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpServiceSmsTest {

    @Test
    void sendOtp_invokesSmsPortAndReturnsCode() {
        AtomicLong ids = new AtomicLong(1);
        List<String> captured = new ArrayList<>();
        OtpVerificationRepository otpRepo = new SaveOnlyOtpRepo(ids);
        SmsMessageRepository smsRepo = unusedSmsRepo();
        AuthOtpSmsPort sms = (cc, mobile, purpose, otpCode, otpId, trigger, adminId) -> {
            captured.add(cc + mobile + ":" + otpCode + ":" + trigger);
            SmsMessage m = new SmsMessage();
            m.setId(1L);
            m.setStatus("SKIPPED");
            return m;
        };

        OtpService service = new OtpService(otpRepo, smsRepo, sms, 6, 5, "123456");
        String code = service.sendOtp("254", "712345678", OtpPurpose.SIGNUP);

        assertEquals("123456", code);
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).startsWith("254712345678:123456:AUTO"));
    }

    @Test
    void sendOtp_softFailsWhenSmsThrows() {
        AtomicLong ids = new AtomicLong(1);
        OtpVerificationRepository otpRepo = new SaveOnlyOtpRepo(ids);
        AuthOtpSmsPort sms = (cc, mobile, purpose, otpCode, otpId, trigger, adminId) -> {
            throw new RuntimeException("provider down");
        };

        OtpService service = new OtpService(otpRepo, unusedSmsRepo(), sms, 6, 5, "123456");
        assertEquals("123456", service.sendOtp("254", "712345678", OtpPurpose.DEVICE_BIND));
    }

    @Test
    void buildOtpMessage_includesCode() {
        String msg = SmsApplicationService.buildOtpMessage(OtpPurpose.PIN_RESET, "654321");
        assertTrue(msg.contains("654321"));
        assertTrue(msg.contains("PIN reset"));
    }

    private static SmsMessageRepository unusedSmsRepo() {
        return new SmsMessageRepository() {
            @Override public long countAdminAuthOtpResendsSince(String recipient, java.time.Instant since) { return 0; }
            @Override public SmsMessage findTopByRecipientAndPurposeAndOtpPurposeOrderByIdDesc(String r, String p, String o) { return null; }
            @Override public <S extends SmsMessage> S save(S entity) { return entity; }
            @Override public Optional<SmsMessage> findById(Long id) { return Optional.empty(); }
            @Override public boolean existsById(Long id) { return false; }
            @Override public List<SmsMessage> findAll() { return List.of(); }
            @Override public List<SmsMessage> findAllById(Iterable<Long> ids) { return List.of(); }
            @Override public long count() { return 0; }
            @Override public void deleteById(Long id) { }
            @Override public void delete(SmsMessage entity) { }
            @Override public void deleteAllById(Iterable<? extends Long> ids) { }
            @Override public void deleteAll(Iterable<? extends SmsMessage> entities) { }
            @Override public void deleteAll() { }
            @Override public <S extends SmsMessage> List<S> saveAll(Iterable<S> entities) { return List.of(); }
            @Override public void flush() { }
            @Override public <S extends SmsMessage> S saveAndFlush(S entity) { return entity; }
            @Override public <S extends SmsMessage> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
            @Override public void deleteAllInBatch(Iterable<SmsMessage> entities) { }
            @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { }
            @Override public void deleteAllInBatch() { }
            @Override public SmsMessage getOne(Long id) { return null; }
            @Override public SmsMessage getById(Long id) { return null; }
            @Override public SmsMessage getReferenceById(Long id) { return null; }
            @Override public <S extends SmsMessage> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
            @Override public <S extends SmsMessage> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
            @Override public <S extends SmsMessage> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
            @Override public <S extends SmsMessage> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
            @Override public <S extends SmsMessage> long count(org.springframework.data.domain.Example<S> example) { return 0; }
            @Override public <S extends SmsMessage> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
            @Override public <S extends SmsMessage, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
            @Override public List<SmsMessage> findAll(org.springframework.data.domain.Sort sort) { return List.of(); }
            @Override public org.springframework.data.domain.Page<SmsMessage> findAll(org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        };
    }

    private static final class SaveOnlyOtpRepo implements OtpVerificationRepository {
        private final AtomicLong ids;

        private SaveOnlyOtpRepo(AtomicLong ids) {
            this.ids = ids;
        }

        @Override
        public OtpVerification save(OtpVerification entity) {
            if (entity.getId() == null) {
                entity.setId(ids.getAndIncrement());
            }
            return entity;
        }

        @Override public Optional<OtpVerification> findLatestValidOtp(String cc, String mobile, String purpose) { return Optional.empty(); }
        @Override public List<OtpVerification> findAll() { return List.of(); }
        @Override public List<OtpVerification> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long id) { }
        @Override public void delete(OtpVerification entity) { }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { }
        @Override public void deleteAll(Iterable<? extends OtpVerification> entities) { }
        @Override public void deleteAll() { }
        @Override public <S extends OtpVerification> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override public Optional<OtpVerification> findById(Long id) { return Optional.empty(); }
        @Override public boolean existsById(Long id) { return false; }
        @Override public void flush() { }
        @Override public <S extends OtpVerification> S saveAndFlush(S entity) { return entity; }
        @Override public <S extends OtpVerification> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<OtpVerification> entities) { }
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { }
        @Override public void deleteAllInBatch() { }
        @Override public OtpVerification getOne(Long id) { return null; }
        @Override public OtpVerification getById(Long id) { return null; }
        @Override public OtpVerification getReferenceById(Long id) { return null; }
        @Override public <S extends OtpVerification> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends OtpVerification> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        @Override public <S extends OtpVerification> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends OtpVerification> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends OtpVerification> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends OtpVerification> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends OtpVerification, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<OtpVerification> findAll(org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public org.springframework.data.domain.Page<OtpVerification> findAll(org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
    }
}
