package com.vycepay.callback.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.callback.application.push.CallbackPushPublisher;
import com.vycepay.callback.application.push.PushMessageFactory;
import com.vycepay.callback.application.service.InboundMoneyEventService;
import com.vycepay.callback.domain.model.ChoiceBankCallback;
import com.vycepay.callback.domain.model.Transaction;
import com.vycepay.callback.domain.model.Wallet;
import com.vycepay.callback.infrastructure.activity.CustomerActivityRecorder;
import com.vycepay.callback.infrastructure.persistence.TransactionRepository;
import com.vycepay.callback.infrastructure.persistence.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionResultHandlerTest {

    @Mock TransactionRepository transactionRepository;
    @Mock WalletRepository walletRepository;

    RecordingPublisher publisher;
    TransactionResultHandler handler;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        publisher = new RecordingPublisher();
        InboundMoneyEventService inbound = new InboundMoneyEventService(
                transactionRepository, walletRepository, new NoOpActivityRecorder());
        handler = new TransactionResultHandler(
                transactionRepository, objectMapper, publisher, inbound);
    }

    @Test
    void handle_found_updatesAndPushes() {
        Transaction tx = new Transaction();
        tx.setCustomerId(10L);
        tx.setExternalId("local-uuid");
        tx.setChoiceTxId("UTRANS02880586c4b020018212");
        when(transactionRepository.findByChoiceTxId("UTRANS02880586c4b020018212"))
                .thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChoiceBankCallback callback = ChoiceBankCallback.builder()
                .choiceRequestId("req-1")
                .notificationType("0002")
                .rawPayload(sample0002Payload())
                .build();
        callback.setId(55L);

        handler.handle(callback);

        verify(transactionRepository).save(tx);
        assertEquals("8", tx.getStatus());
        assertEquals(1, publisher.calls.size());
        assertEquals(10L, publisher.calls.get(0).customerId());
        assertEquals("0002", publisher.calls.get(0).notificationType());
        assertEquals("local-uuid", publisher.calls.get(0).params().get("externalId"));
        verify(walletRepository, never()).findByChoiceAccountId(any());
    }

    @Test
    void handle_notFound_upsertsAndPushes() {
        when(transactionRepository.findByChoiceTxId("UTRANS02880586c4b020018212"))
                .thenReturn(Optional.empty());
        when(transactionRepository.findByChoiceRequestId("UTRANS02880586c4b020018212"))
                .thenReturn(Optional.empty());

        Wallet wallet = new Wallet();
        wallet.setId(5L);
        wallet.setCustomerId(10L);
        wallet.setChoiceAccountId("46012001324475");
        when(walletRepository.findByChoiceAccountId("46012001324475")).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        ChoiceBankCallback callback = ChoiceBankCallback.builder()
                .choiceRequestId("req-1")
                .notificationType("0002")
                .rawPayload(sample0002Payload())
                .build();
        callback.setId(55L);

        handler.handle(callback);

        assertEquals(1, publisher.calls.size());
        assertEquals(10L, publisher.calls.get(0).customerId());
        assertTrue(publisher.calls.get(0).params().containsKey("externalId"));
        verify(transactionRepository).save(any());
    }

    @Test
    void handle_notFound_noWallet_noPush() {
        when(transactionRepository.findByChoiceTxId("UTRANS02880586c4b020018212"))
                .thenReturn(Optional.empty());
        when(transactionRepository.findByChoiceRequestId("UTRANS02880586c4b020018212"))
                .thenReturn(Optional.empty());
        when(walletRepository.findByChoiceAccountId("46012001324475")).thenReturn(Optional.empty());

        ChoiceBankCallback callback = ChoiceBankCallback.builder()
                .choiceRequestId("req-1")
                .notificationType("0002")
                .rawPayload(sample0002Payload())
                .build();
        callback.setId(55L);

        handler.handle(callback);

        assertTrue(publisher.calls.isEmpty());
        verify(transactionRepository, never()).save(any());
    }

    private static String sample0002Payload() {
        return """
                {
                  "requestId": "req-1",
                  "notificationType": "0002",
                  "params": {
                    "txId": "UTRANS02880586c4b020018212",
                    "accountId": "46012001324475",
                    "amount": "50.00",
                    "currency": "KES",
                    "txStatus": 8,
                    "paymentChannel": "PAY_BILL",
                    "updateTime": 1787822801126
                  }
                }
                """;
    }

    static final class NoOpActivityRecorder extends CustomerActivityRecorder {
        @Override
        public void record(Long customerId, String action, String resourceType, String resourceId) {
            // no-op for unit tests
        }
    }

    static final class RecordingPublisher extends CallbackPushPublisher {
        final List<Call> calls = new ArrayList<>();

        RecordingPublisher() {
            super(new PushMessageFactory(), null);
        }

        @Override
        public void publishBestEffort(Long customerId, String notificationType, Map<String, Object> params,
                                      Long choiceCallbackId) {
            calls.add(new Call(customerId, notificationType, params, choiceCallbackId));
        }

        record Call(Long customerId, String notificationType, Map<String, Object> params, Long choiceCallbackId) {}
    }
}
