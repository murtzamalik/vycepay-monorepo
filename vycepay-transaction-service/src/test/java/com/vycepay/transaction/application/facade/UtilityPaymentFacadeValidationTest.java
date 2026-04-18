package com.vycepay.transaction.application.facade;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.errors.ChoiceBankErrorCatalog;
import com.vycepay.common.choicebank.errors.ChoiceBankResponseAssessor;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.transaction.domain.model.Transaction;
import com.vycepay.transaction.infrastructure.persistence.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilityPaymentFacadeValidationTest {

    @Mock
    private BankingProviderPort bankingProvider;
    @Mock
    private TransactionRepository transactionRepository;

    private UtilityPaymentFacade facade;

    @BeforeEach
    void setUp() throws IOException {
        ChoiceBankErrorCatalog catalog = new ChoiceBankErrorCatalog();
        catalog.loadFromClasspath();
        ChoiceBankResponseAssessor assessor = new ChoiceBankResponseAssessor(catalog);
        facade = new UtilityPaymentFacade(bankingProvider, transactionRepository, assessor);
    }

    @Test
    void billPayment_whenSuccessButMissingTransactionId_doesNotPersist() {
        when(transactionRepository.findByIdempotencyKey("idem-utility-no-id")).thenReturn(Optional.empty());
        ChoiceBankResponse resp = new ChoiceBankResponse();
        resp.setCode("00000");
        resp.setRequestId("cb-req-utility");
        resp.setData(Map.of());
        when(bankingProvider.post(eq("utilityPayment/v2/billPayment"), any())).thenReturn(resp);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.billPayment(1L, 10L, "choice-account-1", Map.of("amount", 100), "idem-utility-no-id"));
        assertEquals("CHOICE_INVALID_RESPONSE", ex.getCode());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
