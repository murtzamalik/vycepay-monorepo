package com.vycepay.transaction.application.facade;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.errors.ChoiceBankErrorCatalog;
import com.vycepay.common.choicebank.errors.ChoiceBankResponseAssessor;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.exception.ChoiceBankUpstreamException;
import com.vycepay.transaction.api.v1.dto.ValidateAccountResponse;
import com.vycepay.transaction.application.TransactionChoiceOutcome;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        // catalog also injected into facade for soft 10000/10001 title-fetch mapping
        ChoiceBankErrorCatalog catalog = new ChoiceBankErrorCatalog();
        catalog.loadFromClasspath();
        ChoiceBankResponseAssessor assessor = new ChoiceBankResponseAssessor(catalog);
        facade = new TransactionFacade(
                bankingProvider, transactionRepository, walletRepository, assessor, catalog, activityRecorder);
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
                        new BigDecimal("50.00"), null, null, "idem-frozen", false));

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

        TransactionChoiceOutcome outcome = facade.applyTransfer(1L, 2L, "choice-acc", "01", "0123456789", 4,
                new BigDecimal("100.00"), null, "note", "idem-ok", false);

        assertEquals("CHOICE TITLE", outcome.transaction().getPayeeAccountName());

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
                        new BigDecimal("50.00"), null, null, "idem-13000", false));

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
                        new BigDecimal("10.00"), null, null, "idem-notx", false));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void applyTransfer_paybill_usesMpesaBusinessTransferWithReference() {
        when(transactionRepository.findByIdempotencyKey("idem-paybill")).thenReturn(Optional.empty());
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(successValidate("Equity Paybill Account", 0, 0));
        ChoiceBankResponse transferResp = new ChoiceBankResponse();
        transferResp.setCode("00000");
        transferResp.setRequestId("cb-b2b-1");
        transferResp.setData(Map.of("txId", "tx-b2b-1"));
        when(bankingProvider.post(eq("trans/v2/applyForMpesaBusinessTransfer"), any())).thenReturn(transferResp);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionChoiceOutcome outcome = facade.applyTransfer(
                1L, 2L, "choice-acc", "M-PESA", "247247", 1,
                new BigDecimal("10.00"), "0820176326076", "optional", "idem-paybill", false);

        assertEquals("247247", outcome.transaction().getPayeeAccountId());
        assertEquals("Equity Paybill Account", outcome.transaction().getPayeeAccountName());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(bankingProvider).post(eq("trans/v2/applyForMpesaBusinessTransfer"), paramsCaptor.capture());
        verify(bankingProvider, never()).post(eq("trans/v2/applyForTransfer"), any());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("choice-acc", params.get("payerAccountId"));
        assertEquals("247247", params.get("payeeShortCode"));
        assertEquals(0, params.get("payType"));
        assertEquals("0820176326076", params.get("payeeReferenNumber"));
        assertEquals(new BigDecimal("10.00"), params.get("amount"));
        assertEquals("optional", params.get("description"));
    }

    @Test
    void applyTransfer_till_usesMpesaBusinessTransferWithoutReference() {
        when(transactionRepository.findByIdempotencyKey("idem-till")).thenReturn(Optional.empty());
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(successValidate("Till Merchant", 0, 0));
        ChoiceBankResponse transferResp = new ChoiceBankResponse();
        transferResp.setCode("00000");
        transferResp.setRequestId("cb-b2b-2");
        transferResp.setData(Map.of("txId", "tx-b2b-2"));
        when(bankingProvider.post(eq("trans/v2/applyForMpesaBusinessTransfer"), any())).thenReturn(transferResp);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        facade.applyTransfer(
                1L, 2L, "choice-acc", "M-PESA", "987654", 2,
                new BigDecimal("50.00"), "should-be-ignored", "Buy goods", "idem-till", false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(bankingProvider).post(eq("trans/v2/applyForMpesaBusinessTransfer"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("987654", params.get("payeeShortCode"));
        assertEquals(1, params.get("payType"));
        assertTrue(!params.containsKey("payeeReferenNumber"));
        assertEquals("Buy goods", params.get("description"));
    }

    @Test
    void applyTransfer_paybillMissingReference_throwsBeforeChoice() {
        when(transactionRepository.findByIdempotencyKey("idem-noref")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.applyTransfer(1L, 2L, "choice-acc", "M-PESA", "247247", 1,
                        new BigDecimal("10.00"), null, null, "idem-noref", false));

        assertEquals("INVALID_PAYEE_REFERENCE", ex.getCode());
        verify(bankingProvider, never()).post(any(), any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void applyTransfer_mpesaMobile_stillUsesApplyForTransfer() {
        when(transactionRepository.findByIdempotencyKey("idem-mobile")).thenReturn(Optional.empty());
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(successValidate("Mobile User", 0, 0));
        ChoiceBankResponse transferResp = new ChoiceBankResponse();
        transferResp.setCode("00000");
        transferResp.setRequestId("cb-mobile");
        transferResp.setData(Map.of("txId", "tx-mobile"));
        when(bankingProvider.post(eq("trans/v2/applyForTransfer"), any())).thenReturn(transferResp);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        facade.applyTransfer(
                1L, 2L, "choice-acc", "M-PESA", "712345678", 3,
                new BigDecimal("20.00"), null, null, "idem-mobile", false);

        verify(bankingProvider).post(eq("trans/v2/applyForTransfer"), any());
        verify(bankingProvider, never()).post(eq("trans/v2/applyForMpesaBusinessTransfer"), any());
    }

    @Test
    void validateAccount_when10000OnMpesaMobile_returnsSoftUnavailable() {
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(choiceError("10000", null));

        ValidateAccountResponse result = facade.validateAccount("712345678", 3, null);

        assertFalse(result.isValid());
        assertTrue(result.isTitleFetchUnavailable());
        assertTrue(result.isSkipAllowed());
        assertEquals("10000", result.getChoiceCode());
        assertNull(result.getAccountName());
        assertEquals("712345678", result.getAccountId());
    }

    @Test
    void validateAccount_when10001OnMpesaMobile_returnsSoftUnavailable() {
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(choiceError("10001", "System is busy, please retry later"));

        ValidateAccountResponse result = facade.validateAccount("712345678", 3, null);

        assertTrue(result.isTitleFetchUnavailable());
        assertTrue(result.isSkipAllowed());
        assertEquals("10001", result.getChoiceCode());
        assertEquals("System is busy, please retry later", result.getMessage());
    }

    @Test
    void validateAccount_when10000OnPesalink_throwsUpstream() {
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(choiceError("10000", null));

        ChoiceBankUpstreamException ex = assertThrows(ChoiceBankUpstreamException.class,
                () -> facade.validateAccount("0123456789", 4, "01"));

        assertEquals("CHOICE_SYSTEM_ERROR", ex.getCode());
        assertEquals("10000", ex.getChoiceCode());
    }

    @Test
    void applyTransfer_skipConfirmed_whenValidate10000_omitsPayeeAccountName() {
        when(transactionRepository.findByIdempotencyKey("idem-skip")).thenReturn(Optional.empty());
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(choiceError("10000", null));
        ChoiceBankResponse transferResp = new ChoiceBankResponse();
        transferResp.setCode("00000");
        transferResp.setRequestId("cb-skip");
        transferResp.setData(Map.of("txId", "tx-skip"));
        when(bankingProvider.post(eq("trans/v2/applyForTransfer"), any())).thenReturn(transferResp);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionChoiceOutcome outcome = facade.applyTransfer(
                1L, 2L, "choice-acc", "M-PESA", "712345678", 3,
                new BigDecimal("20.00"), null, null, "idem-skip", true);

        assertNull(outcome.transaction().getPayeeAccountName());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(bankingProvider).post(eq("trans/v2/applyForTransfer"), paramsCaptor.capture());
        assertFalse(paramsCaptor.getValue().containsKey("payeeAccountName"));
    }

    @Test
    void applyTransfer_skipFalse_whenValidate10000_throwsAndDoesNotPersist() {
        when(transactionRepository.findByIdempotencyKey("idem-noskip")).thenReturn(Optional.empty());
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(choiceError("10000", null));

        ChoiceBankUpstreamException ex = assertThrows(ChoiceBankUpstreamException.class,
                () -> facade.applyTransfer(1L, 2L, "choice-acc", "M-PESA", "712345678", 3,
                        new BigDecimal("20.00"), null, null, "idem-noskip", false));

        assertEquals("CHOICE_SYSTEM_ERROR", ex.getCode());
        verify(bankingProvider, never()).post(eq("trans/v2/applyForTransfer"), any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void applyTransfer_skipTrue_whenValidateSucceeds_usesAccountName() {
        when(transactionRepository.findByIdempotencyKey("idem-skip-ok")).thenReturn(Optional.empty());
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(successValidateMobile("REAL NAME"));
        ChoiceBankResponse transferResp = new ChoiceBankResponse();
        transferResp.setCode("00000");
        transferResp.setRequestId("cb-skip-ok");
        transferResp.setData(Map.of("txId", "tx-skip-ok"));
        when(bankingProvider.post(eq("trans/v2/applyForTransfer"), any())).thenReturn(transferResp);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionChoiceOutcome outcome = facade.applyTransfer(
                1L, 2L, "choice-acc", "M-PESA", "712345678", 3,
                new BigDecimal("20.00"), null, null, "idem-skip-ok", true);

        assertEquals("REAL NAME", outcome.transaction().getPayeeAccountName());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(bankingProvider).post(eq("trans/v2/applyForTransfer"), paramsCaptor.capture());
        assertEquals("REAL NAME", paramsCaptor.getValue().get("payeeAccountName"));
    }

    @Test
    void applyTransfer_skipTrueOnPesalink_throwsNotAllowed() {
        when(transactionRepository.findByIdempotencyKey("idem-skip-4")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.applyTransfer(1L, 2L, "choice-acc", "01", "0123456789", 4,
                        new BigDecimal("50.00"), null, null, "idem-skip-4", true));

        assertEquals("TITLE_FETCH_SKIP_NOT_ALLOWED", ex.getCode());
        verify(bankingProvider, never()).post(any(), any());
    }

    @Test
    void applyTransfer_skipConfirmed_paybill_whenValidate10001_stillUsesB2bPath() {
        when(transactionRepository.findByIdempotencyKey("idem-skip-pb")).thenReturn(Optional.empty());
        when(bankingProvider.post(eq("account/validateAccount"), any()))
                .thenReturn(choiceError("10001", "System is busy, please retry later"));
        ChoiceBankResponse transferResp = new ChoiceBankResponse();
        transferResp.setCode("00000");
        transferResp.setRequestId("cb-skip-pb");
        transferResp.setData(Map.of("txId", "tx-skip-pb"));
        when(bankingProvider.post(eq("trans/v2/applyForMpesaBusinessTransfer"), any())).thenReturn(transferResp);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        facade.applyTransfer(
                1L, 2L, "choice-acc", "M-PESA", "247247", 1,
                new BigDecimal("10.00"), "0820176326076", null, "idem-skip-pb", true);

        verify(bankingProvider).post(eq("trans/v2/applyForMpesaBusinessTransfer"), any());
        verify(bankingProvider, never()).post(eq("trans/v2/applyForTransfer"), any());
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

    private static ChoiceBankResponse successValidateMobile(String accountName) {
        ChoiceBankResponse resp = new ChoiceBankResponse();
        resp.setCode("00000");
        resp.setRequestId("cb-val-m");
        resp.setData(Map.of(
                "accountType", 3,
                "accountId", "712345678",
                "accountName", accountName,
                "freezeStatus", 0,
                "restrictStatus", 0));
        return resp;
    }

    private static ChoiceBankResponse choiceError(String code, String msg) {
        ChoiceBankResponse resp = new ChoiceBankResponse();
        resp.setCode(code);
        resp.setMsg(msg);
        resp.setRequestId("cb-err");
        resp.setData(Map.of());
        return resp;
    }
}
