package com.vycepay.callback.application.notification;

import com.vycepay.callback.domain.model.SmsOutbox;
import com.vycepay.callback.domain.port.SmsOutboxPort;
import com.vycepay.common.sms.port.SmsPort;
import com.vycepay.common.sms.port.SmsSendRequest;
import com.vycepay.common.sms.port.SmsSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsOutboxRetryJobTest {

    @Mock SmsOutboxPort smsOutboxService;
    @Mock SmsPort smsPort;

    SmsOutboxRetryJob job;

    @BeforeEach
    void setUp() {
        job = new SmsOutboxRetryJob(smsOutboxService, smsPort, 50);
    }

    @Test
    void pollAndRetry_marksSentOnSuccess() {
        SmsOutbox row = baseRow();
        when(smsOutboxService.claimDue(50)).thenReturn(List.of(row));
        when(smsPort.send(any())).thenReturn(SmsSendResult.sent("uid-1"));

        job.pollAndRetry();

        verify(smsOutboxService).markSent(1L, "uid-1");
        verify(smsOutboxService, never()).markRetryOrDead(any(), any());
        verify(smsOutboxService, never()).deferDisabled(any());
    }

    @Test
    void pollAndRetry_marksRetryOnFailed() {
        SmsOutbox row = baseRow();
        when(smsOutboxService.claimDue(50)).thenReturn(List.of(row));
        when(smsPort.send(any())).thenReturn(SmsSendResult.failed("503"));

        job.pollAndRetry();

        verify(smsOutboxService).markRetryOrDead(eq(1L), eq("503"));
    }

    @Test
    void pollAndRetry_defersWhenDisabled() {
        SmsOutbox row = baseRow();
        when(smsOutboxService.claimDue(50)).thenReturn(List.of(row));
        when(smsPort.send(any())).thenReturn(SmsSendResult.skipped("SMS_DISABLED"));

        job.pollAndRetry();

        verify(smsOutboxService).deferDisabled(1L);
        verify(smsOutboxService, never()).markRetryOrDead(any(), any());
        verify(smsOutboxService, never()).markSent(any(), any());
    }

    @Test
    void pollAndRetry_emptyClaim_noop() {
        when(smsOutboxService.claimDue(50)).thenReturn(List.of());

        job.pollAndRetry();

        verify(smsPort, never()).send(any(SmsSendRequest.class));
    }

    private static SmsOutbox baseRow() {
        SmsOutbox row = new SmsOutbox();
        row.setId(1L);
        row.setDedupeKey("TX:UTRANS1");
        row.setRecipient("254712345678");
        row.setMessageBody("Sent KES 10.00");
        row.setStatus(SmsOutbox.STATUS_SENDING);
        return row;
    }
}
