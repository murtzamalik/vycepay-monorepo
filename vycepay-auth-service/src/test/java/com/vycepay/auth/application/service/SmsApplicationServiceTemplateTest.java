package com.vycepay.auth.application.service;

import com.vycepay.auth.domain.model.OtpPurpose;
import com.vycepay.auth.domain.model.SmsMessage;
import com.vycepay.auth.infrastructure.persistence.SmsDeliveryAttemptRepository;
import com.vycepay.auth.infrastructure.persistence.SmsMessageRepository;
import com.vycepay.common.sms.port.SmsPort;
import com.vycepay.common.sms.port.SmsSendRequest;
import com.vycepay.common.sms.port.SmsSendResult;
import com.vycepay.common.sms.template.SmsTemplateKeys;
import com.vycepay.common.sms.template.SmsTemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsApplicationServiceTemplateTest {

    @Mock SmsPort smsPort;
    @Mock SmsMessageRepository smsMessageRepository;
    @Mock SmsDeliveryAttemptRepository attemptRepository;

    @Test
    void sendAuthOtp_usesTemplateBodyWhenPresent() {
        SmsTemplateService templates = new SmsTemplateService(key -> Optional.of("TMPL {otp} {purpose_label}"));
        SmsApplicationService service = new SmsApplicationService(
                smsPort, smsMessageRepository, attemptRepository, templates);

        when(smsMessageRepository.save(any())).thenAnswer(inv -> {
            SmsMessage m = inv.getArgument(0);
            if (m.getId() == null) m.setId(1L);
            return m;
        });
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(smsPort.send(any())).thenReturn(SmsSendResult.sent("uid"));

        service.sendAuthOtp("254", "712345678", OtpPurpose.DEVICE_BIND, "998877", 5L, "AUTO", null);

        ArgumentCaptor<SmsSendRequest> captor = ArgumentCaptor.forClass(SmsSendRequest.class);
        verify(smsPort).send(captor.capture());
        assertEquals("254712345678", captor.getValue().recipient());
        assertEquals("TMPL 998877 new device login", captor.getValue().message());
    }

    @Test
    void sendAuthOtp_fallsBackToDefaultWhenInactive() {
        SmsTemplateService templates = new SmsTemplateService(key -> Optional.empty());
        SmsApplicationService service = new SmsApplicationService(
                smsPort, smsMessageRepository, attemptRepository, templates);

        when(smsMessageRepository.save(any())).thenAnswer(inv -> {
            SmsMessage m = inv.getArgument(0);
            if (m.getId() == null) m.setId(2L);
            return m;
        });
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(smsPort.send(any())).thenReturn(SmsSendResult.skipped("SMS_DISABLED"));

        service.sendAuthOtp("254", "712345678", OtpPurpose.SIGNUP, "123456", 9L, "AUTO", null);

        ArgumentCaptor<SmsSendRequest> captor = ArgumentCaptor.forClass(SmsSendRequest.class);
        verify(smsPort).send(captor.capture());
        assertTrue(captor.getValue().message().contains("123456"));
        assertTrue(captor.getValue().message().contains("verification"));
    }

    @Test
    void templateKeyFor_mapsPurposes() {
        assertEquals(SmsTemplateKeys.OTP_SIGNUP, SmsApplicationService.templateKeyFor(OtpPurpose.SIGNUP));
        assertEquals(SmsTemplateKeys.OTP_DEVICE_BIND, SmsApplicationService.templateKeyFor(OtpPurpose.DEVICE_BIND));
        assertEquals(SmsTemplateKeys.OTP_PIN_RESET, SmsApplicationService.templateKeyFor(OtpPurpose.PIN_RESET));
        assertEquals(SmsTemplateKeys.OTP_CREDENTIALS_MIGRATE,
                SmsApplicationService.templateKeyFor(OtpPurpose.CREDENTIALS_MIGRATE));
    }
}
