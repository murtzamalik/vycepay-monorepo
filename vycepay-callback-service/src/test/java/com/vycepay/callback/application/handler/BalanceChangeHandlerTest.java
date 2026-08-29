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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class BalanceChangeHandlerTest {

    @Mock WalletRepository walletRepository;
    @Mock TransactionRepository transactionRepository;

    RecordingPublisher publisher;
    BalanceChangeHandler handler;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        publisher = new RecordingPublisher();
        InboundMoneyEventService inbound = new InboundMoneyEventService(
                transactionRepository, walletRepository, new NoOpActivityRecorder());
        handler = new BalanceChangeHandler(walletRepository, objectMapper, inbound, publisher);
    }

    @Test
    void handle_updatesBalance_upsertsAndPushes() {
        Wallet wallet = new Wallet();
        wallet.setId(5L);
        wallet.setCustomerId(42L);
        wallet.setChoiceAccountId("46012001324475");
        when(walletRepository.findByChoiceAccountId("46012001324475")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.findByChoiceTxId("UTRANS029874c90bc020008212"))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });

        ChoiceBankCallback callback = ChoiceBankCallback.builder()
                .choiceRequestId("1870277914143047688212")
                .notificationType("0003")
                .rawPayload(samplePayload())
                .build();
        callback.setId(77L);

        handler.handle(callback);

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertEquals(new BigDecimal("322.00"), walletCaptor.getValue().getBalanceCache());

        assertEquals(1, publisher.calls.size());
        RecordingPublisher.Call call = publisher.calls.get(0);
        assertEquals(42L, call.customerId);
        assertEquals("0003", call.notificationType);
        assertEquals(77L, call.choiceCallbackId);
        assertTrue(call.params.containsKey("externalId"));
        verify(transactionRepository).save(any());
    }

    @Test
    void handle_walletMissing_noPush() {
        when(walletRepository.findByChoiceAccountId("46012001324475")).thenReturn(Optional.empty());

        ChoiceBankCallback callback = ChoiceBankCallback.builder()
                .choiceRequestId("1870277914143047688212")
                .notificationType("0003")
                .rawPayload(samplePayload())
                .build();
        callback.setId(77L);

        handler.handle(callback);

        verify(walletRepository, never()).save(any());
        assertTrue(publisher.calls.isEmpty());
        verify(transactionRepository, never()).save(any());
    }

    private static String samplePayload() {
        return """
                {
                  "requestId": "1870277914143047688212",
                  "notificationType": "0003",
                  "params": {
                    "txId": "UTRANS029874c90bc020008212",
                    "accountId": "46012001324475",
                    "amount": "30.00",
                    "balance": "322.00",
                    "currency": "KES",
                    "paymentChannel": "PAY_BILL",
                    "oppoAccountName": "ROSE WUGHANGA MWALUKUKU",
                    "completeTime": 1787822801126
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
