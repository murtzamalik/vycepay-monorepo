package com.vycepay.callback.application.notification;

import com.vycepay.callback.domain.model.SmsOutbox;
import com.vycepay.callback.infrastructure.persistence.SmsOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsOutboxServiceTest {

    @Mock SmsOutboxRepository outboxRepository;

    SmsOutboxService service;

    @BeforeEach
    void setUp() {
        service = new SmsOutboxService(outboxRepository, 10);
    }

    @Test
    void parkFailed_insertsNewPendingRow() {
        when(outboxRepository.findByDedupeKey("TX:UTRANS1")).thenReturn(Optional.empty());
        when(outboxRepository.save(any())).thenAnswer(inv -> {
            SmsOutbox o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        service.parkFailed(5L, 9L, "TX:UTRANS1", "254712345678", "Sent KES 10.00", "Connection refused");

        ArgumentCaptor<SmsOutbox> captor = ArgumentCaptor.forClass(SmsOutbox.class);
        verify(outboxRepository).save(captor.capture());
        SmsOutbox saved = captor.getValue();
        assertEquals(SmsOutbox.STATUS_PENDING, saved.getStatus());
        assertEquals("TX:UTRANS1", saved.getDedupeKey());
        assertEquals("254712345678", saved.getRecipient());
        assertEquals(5L, saved.getCustomerId());
        assertEquals(9L, saved.getNotificationId());
        assertEquals(0, saved.getAttemptCount());
    }

    @Test
    void parkFailed_alreadySent_isNoOp() {
        SmsOutbox existing = new SmsOutbox();
        existing.setId(2L);
        existing.setDedupeKey("TX:UTRANS1");
        existing.setStatus(SmsOutbox.STATUS_SENT);
        when(outboxRepository.findByDedupeKey("TX:UTRANS1")).thenReturn(Optional.of(existing));

        service.parkFailed(5L, 9L, "TX:UTRANS1", "254712345678", "body", "err");

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void parkFailed_existingPending_refreshesAndResets() {
        SmsOutbox existing = new SmsOutbox();
        existing.setId(3L);
        existing.setDedupeKey("TX:UTRANS1");
        existing.setStatus(SmsOutbox.STATUS_DEAD);
        existing.setAttemptCount(10);
        when(outboxRepository.findByDedupeKey("TX:UTRANS1")).thenReturn(Optional.of(existing));
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.parkFailed(5L, 11L, "TX:UTRANS1", "254700000000", "new body", "503");

        ArgumentCaptor<SmsOutbox> captor = ArgumentCaptor.forClass(SmsOutbox.class);
        verify(outboxRepository).save(captor.capture());
        assertEquals(SmsOutbox.STATUS_PENDING, captor.getValue().getStatus());
        assertEquals("new body", captor.getValue().getMessageBody());
        assertEquals(11L, captor.getValue().getNotificationId());
    }

    @Test
    void claimDue_marksSending() {
        when(outboxRepository.resetStuckSending(any(), any())).thenReturn(0);
        SmsOutbox due = new SmsOutbox();
        due.setId(7L);
        due.setStatus(SmsOutbox.STATUS_PENDING);
        when(outboxRepository.findDueForRetry(any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(due));
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<SmsOutbox> claimed = service.claimDue(50);

        assertEquals(1, claimed.size());
        assertEquals(SmsOutbox.STATUS_SENDING, claimed.get(0).getStatus());
    }

    @Test
    void markRetryOrDead_backoffThenDead() {
        SmsOutbox row = new SmsOutbox();
        row.setId(8L);
        row.setDedupeKey("TX:X");
        row.setAttemptCount(0);
        row.setMaxAttempts(2);
        row.setStatus(SmsOutbox.STATUS_SENDING);
        when(outboxRepository.findById(8L)).thenReturn(Optional.of(row));
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markRetryOrDead(8L, "fail1");
        assertEquals(SmsOutbox.STATUS_PENDING, row.getStatus());
        assertEquals(1, row.getAttemptCount());
        assertTrue(row.getNextAttemptAt().isAfter(Instant.now().minusSeconds(1)));

        service.markRetryOrDead(8L, "fail2");
        assertEquals(SmsOutbox.STATUS_DEAD, row.getStatus());
        assertEquals(2, row.getAttemptCount());
    }

    @Test
    void deferDisabled_keepsAttemptCount() {
        SmsOutbox row = new SmsOutbox();
        row.setId(9L);
        row.setAttemptCount(3);
        row.setStatus(SmsOutbox.STATUS_SENDING);
        when(outboxRepository.findById(9L)).thenReturn(Optional.of(row));
        when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deferDisabled(9L);

        assertEquals(3, row.getAttemptCount());
        assertEquals(SmsOutbox.STATUS_PENDING, row.getStatus());
        assertEquals("SMS_DISABLED", row.getLastError());
    }
}
