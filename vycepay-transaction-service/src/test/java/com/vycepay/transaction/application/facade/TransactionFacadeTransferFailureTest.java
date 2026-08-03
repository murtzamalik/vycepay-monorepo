package com.vycepay.transaction.application.facade;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.errors.ChoiceBankErrorCatalog;
import com.vycepay.common.choicebank.errors.ChoiceBankResponseAssessor;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.exception.ChoiceBankUpstreamException;
import com.vycepay.transaction.api.v1.dto.ValidateAccountResponse;
import com.vycepay.transaction.domain.model.Transaction;
import com.vycepay.transaction.infrastructure.activity.CustomerActivityRecorder;
import com.vycepay.transaction.infrastructure.persistence.TransactionRepository;
import com.vycepay.transaction.infrastructure.persistence.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ensures Choice business failures from applyForTransfer never persist a transaction row,
 * and Hakikisha validate gates frozen / restrict-in accounts before transfer.
 */
@ExtendWith(MockitoExtension.class)
class TransactionFacadeTransferFailureTest {

    @Mock
    private BankingProviderPort bankingProvider;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private CustomerActivityRecorder activityRecorder;

    private TransactionFacade facade;

    @BeforeEach
    void setUp() throws IOException {
        ChoiceBankErrorCatalog catalog = new ChoiceBankErrorCatalog();
        catalog.loadFromClasspath();
        ChoiceBankResponseAssessor assessor = new ChoiceBankResponseAssessor(catalog);
        facade = new TransactionFacade(bankingProvider, transactionRepository, walletRepository, assessor, activityRecorder);
    }

    @Test
    void validateAccount_whenSuccess_mapsAccountName() {
        ChoiceBankResponse resp = successValidate("JOHN DOE", 0, 0);
        when(bankingProvider.post(eq("account/validateAccount"), any())).thenReturn(resp);

        ValidateAccountResponse result = facade.validateAccount("0123456789", 4, "01");

        assertEquals("JOHN DOE", result.getAccountName());
        assertEquals("0123456789", result.getAccountId());
        assertEquals(4, result.getAccountType());
        assertEquals(0, result.getFreezeStatus());
        assertEquals(0, result.getRestrictStatus());
        assertTrue(result.isValid());
    }

    @Test
    void validateAccount_whenType4WithoutBankCode_throwsLocally() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.validateAccount("0123456789", 4, null));

        assertEquals("BANK_CODE_REQUIRED", ex.getCode());
        verify(bankingProvider, never()).post(any(), any());
    }

    @Test
    void validateAccount_whenFrozen_throwsAndDoesNotTransfer() {
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(successValidate("FROZEN USER", 1, 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.validateAccount("0999", 3, null));

        assertEquals("ACCOUNT_FROZEN", ex.getCode());
    }

    @Test
    void validateAccount_whenRestrictIn_throws() {
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(successValidate("RESTRICTED", 0, 1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.validateAccount("0999", 3, null));

        assertEquals("ACCOUNT_RESTRICT_IN", ex.getCode());
    }

    @Test
    void applyTransfer_whenFrozen_doesNotCallApplyForTransferOrPersist() {
        when(transactionRepository.findByIdempotencyKey("idem-frozen")).thenReturn(Optional.empty());
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(successValidate("FROZEN USER", 1, 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.applyTransfer(1L, 2L, "choice-acc", "01", "0123456789", 4,
                        new BigDecimal("50.00"), null, "idem-frozen"));

        assertEquals("ACCOUNT_FROZEN", ex.getCode());
        verify(bankingProvider, never()).post(eq("trans/v2/applyForTransfer"), any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void applyTransfer_revalidatesAndOverwritesPayeeName() {
        when(transactionRepository.findByIdempotencyKey("idem-ok")).thenReturn(Optional.empty());
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(successValidate("CHOICE TITLE", 0, 0));
        ChoiceBankResponse transferResp = new ChoiceBankResponse();
        transferResp.setCode("00000");
        transferResp.setRequestId("cb-req-ok");
        transferResp.setData(Map.of("txId", "tx-123"));
        when(bankingProvider.post(eq("trans/v2/applyForTransfer"), any())).thenReturn(transferResp);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction tx = facade.applyTransfer(1L, 2L, "choice-acc", "01", "0123456789", 4,
                new BigDecimal("100.00"), "note", "idem-ok");

        assertEquals("CHOICE TITLE", tx.getPayeeAccountName());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(bankingProvider).post(eq("trans/v2/applyForTransfer"), paramsCaptor.capture());
        assertEquals("CHOICE TITLE", paramsCaptor.getValue().get("payeeAccountName"));
    }

    @Test
    void applyTransfer_whenChoiceReturns13000_doesNotPersistTransaction() {
        when(transactionRepository.findByIdempotencyKey("idem-13000")).thenReturn(Optional.empty());
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(successValidate("Payee", 0, 0));
        ChoiceBankResponse resp = new ChoiceBankResponse();
        resp.setCode("13000");
        resp.setMsg("Account not found");
        resp.setRequestId("cb-req-1");
        resp.setData(null);
        when(bankingProvider.post(eq("trans/v2/applyForTransfer"), any())).thenReturn(resp);

        ChoiceBankUpstreamException ex = assertThrows(ChoiceBankUpstreamException.class,
                () -> facade.applyTransfer(1L, 2L, "choice-acc", "BANK", "254700000000", 3,
                        new BigDecimal("50.00"), null, "idem-13000"));

        assertEquals("CHOICE_ACCOUNT_NOT_FOUND", ex.getCode());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void applyTransfer_whenSuccessButMissingTxId_doesNotPersistTransaction() {
        when(transactionRepository.findByIdempotencyKey("idem-notx")).thenReturn(Optional.empty());
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(successValidate("Payee", 0, 0));
        ChoiceBankResponse resp = new ChoiceBankResponse();
        resp.setCode("00000");
        resp.setRequestId("cb-req-2");
        resp.setData(Map.of());
        when(bankingProvider.post(eq("trans/v2/applyForTransfer"), any())).thenReturn(resp);

        assertThrows(BusinessException.class,
                () -> facade.applyTransfer(1L, 2L, "choice-acc", "BANK", "254700000000", 3,
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

    private static ChoiceBankResponse successValidate(String accountName, int freeze, int restrict) {
        ChoiceBankResponse resp = new ChoiceBankResponse();
        resp.setCode("00000");
        resp.setRequestId("cb-val-1");
        resp.setData(Map.of(
                "accountType", 4,
                "accountId", "0123456789",
                "accountName", accountName,
                "freezeStatus", freeze,
                "restrictStatus", restrict));
        return resp;
    }
}
