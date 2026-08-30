package com.vycepay.transaction.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.common.exception.ChoiceBankUpstreamException;
import com.vycepay.common.exception.GlobalExceptionHandler;
import com.vycepay.common.exception.VyceErrorCatalog;
import com.vycepay.transaction.api.v1.dto.SendMoneyRequest;
import com.vycepay.transaction.application.facade.TransactionFacade;
import com.vycepay.transaction.domain.model.Customer;
import com.vycepay.transaction.domain.model.Wallet;
import com.vycepay.transaction.infrastructure.persistence.CustomerRepository;
import com.vycepay.transaction.infrastructure.persistence.KycVerificationRepository;
import com.vycepay.transaction.infrastructure.persistence.TransactionRepository;
import com.vycepay.transaction.infrastructure.persistence.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies {@link GlobalExceptionHandler} maps {@link ChoiceBankUpstreamException} to HTTP status and body fields.
 * Prefer Choice {@code msg} as customer-facing {@code message}.
 */
@ExtendWith(MockitoExtension.class)
class TransactionControllerChoiceUpstreamWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TransactionFacade transactionFacade;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private KycVerificationRepository kycVerificationRepository;

    @BeforeEach
    void setUp() throws Exception {
        VyceErrorCatalog catalog = new VyceErrorCatalog();
        catalog.loadFromClasspath();
        TransactionController controller = new TransactionController(
                transactionFacade, customerRepository, walletRepository,
                transactionRepository, kycVerificationRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(catalog))
                .build();
    }

    @Test
    void send_whenChoiceUpstream_returnsMappedStatusAndFields() throws Exception {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setExternalId("cust-ext-1");
        Wallet wallet = new Wallet();
        wallet.setId(10L);
        wallet.setCustomerId(1L);
        wallet.setChoiceAccountId("choice-acc-1");

        when(customerRepository.findByExternalId("cust-ext-1")).thenReturn(Optional.of(customer));
        when(walletRepository.findByCustomerId(1L)).thenReturn(Optional.of(wallet));
        when(transactionFacade.applyTransfer(anyLong(), anyLong(), anyString(), anyString(), anyString(), any(),
                any(), any(), any(), anyString()))
                .thenThrow(new ChoiceBankUpstreamException(
                        "CHOICE_ACCOUNT_NOT_FOUND",
                        "Account not found",
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "13000",
                        "Account not found",
                        "cb-req-99",
                        "trans/v2/applyForTransfer",
                        false));

        SendMoneyRequest body = new SendMoneyRequest();
        body.setPayeeBankCode("MPESA");
        body.setPayeeAccountId("254700000000");
        body.setAccountType(3);
        body.setAmount(new BigDecimal("25.00"));

        mockMvc.perform(post("/api/v1/transactions/send")
                        .header("X-Customer-Id", "cust-ext-1")
                        .header("Idempotency-Key", "idem-web-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHOICE_ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Account not found"))
                .andExpect(jsonPath("$.choiceCode").value("13000"))
                .andExpect(jsonPath("$.choicePath").value("trans/v2/applyForTransfer"))
                .andExpect(jsonPath("$.choiceRequestId").value("cb-req-99"))
                .andExpect(jsonPath("$.retryable").value(false));
    }
}
