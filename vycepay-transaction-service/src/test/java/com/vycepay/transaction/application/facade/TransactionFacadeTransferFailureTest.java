package com.vycepay.transaction.application.facade;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.errors.ChoiceBankErrorCatalog;
import com.vycepay.common.choicebank.errors.ChoiceBankResponseAssessor;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.exception.ChoiceBankUpstreamException;
import com.vycepay.transaction.domain.model.Transaction;
import com.vycepay.transaction.infrastructure.persistence.TransactionRepository;
import com.vycepay.transaction.infrastructure.persistence.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ensures Choice business failures from applyForTransfer never persist a transaction row.
 */
@ExtendWith(MockitoExtension.class)
class TransactionFacadeTransferFailureTest {

    @Mock
    private BankingProviderPort bankingProvider;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WalletRepository walletRepository;

    private TransactionFacade facade;

    @BeforeEach
    void setUp() throws IOException {
        ChoiceBankErrorCatalog catalog = new ChoiceBankErrorCatalog();
        catalog.loadFromClasspath();
        ChoiceBankResponseAssessor assessor = new ChoiceBankResponseAssessor(catalog);
        facade = new TransactionFacade(bankingProvider, transactionRepository, walletRepository, assessor);
    }

    @Test
    void applyTransfer_whenChoiceReturns13000_doesNotPersistTransaction() {
        when(transactionRepository.findByIdempotencyKey("idem-13000")).thenReturn(Optional.empty());
        ChoiceBankResponse resp = new ChoiceBankResponse();
        resp.setCode("13000");
        resp.setMsg("Account not found");
        resp.setRequestId("cb-req-1");
        resp.setData(null);
        when(bankingProvider.post(eq("trans/v2/applyForTransfer"), any())).thenReturn(resp);

        ChoiceBankUpstreamException ex = assertThrows(ChoiceBankUpstreamException.class,
                () -> facade.applyTransfer(1L, 2L, "choice-acc", "BANK", "254700000000", "Payee",
                        new BigDecimal("50.00"), null, "idem-13000"));

        assertEquals("CHOICE_ACCOUNT_NOT_FOUND", ex.getCode());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void applyTransfer_whenSuccessButMissingTxId_doesNotPersistTransaction() {
        when(transactionRepository.findByIdempotencyKey("idem-notx")).thenReturn(Optional.empty());
        ChoiceBankResponse resp = new ChoiceBankResponse();
        resp.setCode("00000");
        resp.setRequestId("cb-req-2");
        resp.setData(Map.of());
        when(bankingProvider.post(eq("trans/v2/applyForTransfer"), any())).thenReturn(resp);

        assertThrows(BusinessException.class,
                () -> facade.applyTransfer(1L, 2L, "choice-acc", "BANK", "254700000000", "Payee",
                        new BigDecimal("10.00"), null, "idem-notx"));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getBankCodes_whenSuccessButDataIsNotMap_throwsControlledError() {
        ChoiceBankResponse resp = new ChoiceBankResponse();
        resp.setCode("00000");
        resp.setRequestId("cb-req-3");
        resp.setData("not-a-map");
        when(bankingProvider.post(eq("staticData/getBankCodes"), any())).thenReturn(resp);

        BusinessException ex = assertThrows(BusinessException.class, () -> facade.getBankCodes());
        assertEquals("CHOICE_INVALID_RESPONSE", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.BAD_GATEWAY, ex.getHttpStatus());
    }
}
