package com.vycepay.wallet.application.facade;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.errors.ChoiceBankErrorCatalog;
import com.vycepay.common.choicebank.errors.ChoiceBankResponseAssessor;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.security.port.SensitiveDataEncryptionPort;
import com.vycepay.wallet.application.WalletAccountContext;
import com.vycepay.wallet.domain.model.Customer;
import com.vycepay.wallet.domain.model.KycVerification;
import com.vycepay.wallet.domain.model.Wallet;
import com.vycepay.wallet.infrastructure.persistence.CustomerRepository;
import com.vycepay.wallet.infrastructure.persistence.KycVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountManagementFacadeEmailTest {

    @Mock
    private BankingProviderPort bankingProvider;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private KycVerificationRepository kycVerificationRepository;
    @Mock
    private SensitiveDataEncryptionPort encryptionPort;

    private AccountManagementFacade facade;

    @BeforeEach
    void setUp() throws IOException {
        ChoiceBankErrorCatalog catalog = new ChoiceBankErrorCatalog();
        catalog.loadFromClasspath();
        facade = new AccountManagementFacade(bankingProvider, new ChoiceBankResponseAssessor(catalog),
                customerRepository, kycVerificationRepository, encryptionPort);
    }

    @Test
    void addOrUpdateEmail_whenKycPresent_sendsIdentityFromKycNotClientAndPersists() {
        when(encryptionPort.decrypt("enc-id")).thenReturn("12345678");
        when(bankingProvider.post(eq("user/addOrUpdateEmail"), any())).thenReturn(success(Map.of("applicationId", "app-1")));

        facade.addOrUpdateEmail(ctxWithKyc(), "  new@example.com ");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(bankingProvider).post(eq("user/addOrUpdateEmail"), params.capture());
        assertEquals("personal", params.getValue().get("onboardType"));
        assertEquals("101", params.getValue().get("personalIdType"));
        assertEquals("12345678", params.getValue().get("documentNumber"));
        assertEquals("new@example.com", params.getValue().get("email"));
        assertEquals(4, params.getValue().size());

        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(saved.capture());
        assertEquals("new@example.com", saved.getValue().getEmail());
    }

    @Test
    void addOrUpdateEmail_whenKycMissing_doesNotCallChoice() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.addOrUpdateEmail(ctxWithoutKyc(), "a@b.co"));
        assertEquals("KYC_IDENTITY_MISSING", ex.getCode());
        verify(bankingProvider, never()).post(any(), any());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void addOrUpdateEmail_whenEmailInvalid_doesNotCallChoice() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.addOrUpdateEmail(ctxWithKyc(), "not-an-email"));
        assertEquals("INVALID_EMAIL", ex.getCode());
        verify(bankingProvider, never()).post(any(), any());
    }

    @Test
    void verifyEmailAddress_sendsKycIdentityOnly() {
        when(encryptionPort.decrypt("enc-id")).thenReturn("12345678");
        when(bankingProvider.post(eq("account/verifyEmailAddress"), any())).thenReturn(success(Map.of("applicationId", "app-2")));

        facade.verifyEmailAddress(ctxWithKyc());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(bankingProvider).post(eq("account/verifyEmailAddress"), params.capture());
        assertEquals("personal", params.getValue().get("onboardType"));
        assertEquals("101", params.getValue().get("personalIdType"));
        assertEquals("12345678", params.getValue().get("documentNumber"));
        assertEquals(3, params.getValue().size());
    }

    @Test
    void verifyEmailOrMobile_injectsIdentityAndNormalizesType() {
        when(encryptionPort.decrypt("enc-id")).thenReturn("12345678");
        when(bankingProvider.post(eq("account/verifyEmailOrMobile"), any())).thenReturn(success(Map.of()));

        facade.verifyEmailOrMobile(ctxWithKyc(), "EMAIL");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(bankingProvider).post(eq("account/verifyEmailOrMobile"), params.capture());
        assertEquals("email", params.getValue().get("verifyType"));
        assertEquals("12345678", params.getValue().get("documentNumber"));
    }

    @Test
    void verifyEmailOrMobile_whenVerifyTypeInvalid_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.verifyEmailOrMobile(ctxWithKyc(), "whatsapp"));
        assertEquals("INVALID_VERIFY_TYPE", ex.getCode());
        verify(bankingProvider, never()).post(any(), any());
    }

    @Test
    void addOrUpdateEmail_whenLocalIdMissing_backfillsFromChoiceGetUserKyc() {
        when(encryptionPort.encrypt("12345678")).thenReturn("enc-id");
        when(bankingProvider.post(eq("onboarding/getUserKyc"), any()))
                .thenReturn(success(Map.of("idType", "101", "idNumber", "12345678")));
        when(bankingProvider.post(eq("user/addOrUpdateEmail"), any()))
                .thenReturn(success(Map.of("applicationId", "app-1")));

        facade.addOrUpdateEmail(ctxWithKycNoId(), "hannanali29@gmail.com");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(bankingProvider).post(eq("user/addOrUpdateEmail"), params.capture());
        assertEquals("12345678", params.getValue().get("documentNumber"));
        verify(kycVerificationRepository).save(any(KycVerification.class));
    }

    private static WalletAccountContext ctxWithKyc() {
        Customer customer = new Customer();
        customer.setId(10L);
        customer.setExternalId("cust-ext");
        Wallet wallet = new Wallet();
        wallet.setChoiceAccountId("46012001327585");
        KycVerification kyc = new KycVerification();
        kyc.setIdType("101");
        kyc.setIdNumber("enc-id");
        return new WalletAccountContext(10L, customer, wallet, kyc);
    }

    private static WalletAccountContext ctxWithKycNoId() {
        Customer customer = new Customer();
        customer.setId(10L);
        customer.setExternalId("cust-ext");
        Wallet wallet = new Wallet();
        wallet.setChoiceAccountId("46012001327585");
        KycVerification kyc = new KycVerification();
        kyc.setChoiceOnboardingRequestId("ONBRD-1");
        kyc.setIdType("101");
        return new WalletAccountContext(10L, customer, wallet, kyc);
    }

    private static WalletAccountContext ctxWithoutKyc() {
        Customer customer = new Customer();
        customer.setId(10L);
        Wallet wallet = new Wallet();
        wallet.setChoiceAccountId("46012001327585");
        return new WalletAccountContext(10L, customer, wallet, null);
    }

    private static ChoiceBankResponse success(Object data) {
        ChoiceBankResponse resp = new ChoiceBankResponse();
        resp.setCode("00000");
        resp.setMsg("Completed successfully");
        resp.setRequestId("cb-req");
        resp.setData(data);
        return resp;
    }
}
