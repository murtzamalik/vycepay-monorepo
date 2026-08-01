package com.vycepay.callback.application.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.callback.domain.model.Customer;
import com.vycepay.callback.domain.model.CustomerNotification;
import com.vycepay.callback.domain.model.PushDeliveryLog;
import com.vycepay.callback.domain.model.PushMessage;
import com.vycepay.callback.domain.model.PushSendResult;
import com.vycepay.callback.domain.port.PushNotificationPort;
import com.vycepay.callback.infrastructure.persistence.CustomerNotificationRepository;
import com.vycepay.callback.infrastructure.persistence.CustomerRepository;
import com.vycepay.callback.infrastructure.persistence.PushDeliveryLogRepository;
import com.vycepay.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationOrchestratorTest {

    @Mock CustomerNotificationRepository notificationRepository;
    @Mock PushDeliveryLogRepository deliveryLogRepository;
    @Mock CustomerRepository customerRepository;
    @Mock PushNotificationPort pushNotificationPort;

    NotificationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new NotificationOrchestrator(
                notificationRepository, deliveryLogRepository, customerRepository,
                pushNotificationPort, new ObjectMapper());
    }

    @Test
    void createAndSendFromCallback_persistsInboxAndDelivery() {
        when(notificationRepository.save(any())).thenAnswer(inv -> {
            CustomerNotification n = inv.getArgument(0);
            n.setId(10L);
            return n;
        });
        when(pushNotificationPort.sendToCustomer(eq(1L), any()))
                .thenReturn(PushSendResult.sent(1, 1, 0));
        when(deliveryLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PushMessage message = PushMessage.builder()
                .title("t").body("b").pushType("TRANSACTION_RESULT").notificationType("0002")
                .build();
        orchestrator.createAndSendFromCallback(1L, message, 99L);

        verify(notificationRepository).save(any());
        verify(pushNotificationPort).sendToCustomer(eq(1L), any());
        ArgumentCaptor<PushDeliveryLog> logCaptor = ArgumentCaptor.forClass(PushDeliveryLog.class);
        verify(deliveryLogRepository).save(logCaptor.capture());
        assertEquals(PushSendResult.STATUS_SENT, logCaptor.getValue().getStatus());
        assertEquals(PushDeliveryLog.TRIGGER_AUTO, logCaptor.getValue().getTriggerSource());
    }

    @Test
    void compose_rejectsTooMany() {
        List<Long> ids = java.util.stream.LongStream.rangeClosed(1, 101).boxed().toList();
        assertThrows(BusinessException.class, () ->
                orchestrator.compose(ids, "t", "b", Map.of(), 1L));
        verify(pushNotificationPort, never()).sendToCustomer(any(), any());
    }

    @Test
    void compose_twoCustomers_sameBatch() {
        Customer c1 = new Customer(); c1.setId(1L);
        Customer c2 = new Customer(); c2.setId(2L);
        when(customerRepository.findByIdIn(any())).thenReturn(List.of(c1, c2));
        when(notificationRepository.save(any())).thenAnswer(inv -> {
            CustomerNotification n = inv.getArgument(0);
            n.setId(n.getCustomerId() == 1L ? 11L : 12L);
            return n;
        });
        when(pushNotificationPort.sendToCustomer(any(), any()))
                .thenReturn(PushSendResult.skipped(PushSendResult.SKIP_FIREBASE_DISABLED));
        when(deliveryLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = orchestrator.compose(List.of(1L, 2L), "Hello", "World", null, 7L);

        assertEquals(2, result.get("accepted"));
        verify(notificationRepository, times(2)).save(any());
        ArgumentCaptor<CustomerNotification> captor = ArgumentCaptor.forClass(CustomerNotification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertEquals(captor.getAllValues().get(0).getBatchId(), captor.getAllValues().get(1).getBatchId());
    }

    @Test
    void resend_rateLimited() {
        CustomerNotification n = new CustomerNotification();
        n.setId(5L);
        n.setCustomerId(1L);
        n.setTitle("t");
        n.setBody("b");
        n.setPushType("ADMIN_MESSAGE");
        n.setPublicId("uuid");
        when(notificationRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(java.util.Optional.of(n));
        when(deliveryLogRepository.countResendsSince(eq(5L), any())).thenReturn(5L);

        BusinessException ex = assertThrows(BusinessException.class, () -> orchestrator.resend(5L, 1L));
        assertEquals("RESEND_RATE_LIMITED", ex.getCode());
    }
}
