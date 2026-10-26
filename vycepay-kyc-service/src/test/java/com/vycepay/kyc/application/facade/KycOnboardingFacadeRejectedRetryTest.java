package com.vycepay.kyc.application.facade;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.errors.ChoiceBankErrorCatalog;
import com.vycepay.common.choicebank.errors.ChoiceBankResponseAssessor;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.kyc.application.KycSubmitOutcome;
import com.vycepay.kyc.application.service.OnboardingCredentialsService;
import com.vycepay.kyc.domain.model.Customer;
import com.vycepay.kyc.domain.model.KycVerification;
import com.vycepay.kyc.infrastructure.activity.CustomerActivityRecorder;
import com.vycepay.kyc.infrastructure.persistence.CustomerRepository;
import com.vycepay.kyc.infrastructure.persistence.KycVerificationRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Facade tests for rejected-KYC retry (credential overwrite + unsuspend).
 * Uses a real {@link ChoiceBankResponseAssessor} to avoid mocking issues on newer JDKs.
 */
@ExtendWith(MockitoExtension.class)
class KycOnboardingFacadeRejectedRetryTest {

    @Mock
    private BankingProviderPort bankingProvider;
    @Mock
    private ChoiceBankErrorCatalog errorCatalog;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private KycVerificationRepository kycRepository;
    @Mock
    private OnboardingCredentialsService onboardingCredentialsService;
    @Mock
    private CustomerActivityRecorder activityRecorder;

    private KycOnboardingFacade facade;

    @BeforeEach
    void setUp() {
        ChoiceBankResponseAssessor choiceAssessor = new ChoiceBankResponseAssessor(errorCatalog);
        facade = new KycOnboardingFacade(
                bankingProvider, choiceAssessor, customerRepository, kycRepository,
                onboardingCredentialsService, null, activityRecorder, null, null);
    }

    @Test
    void rejectedSuspended_overwritesCredentials_setsActive_andResubmits() {
        Customer customer = new Customer();
        customer.setId(10L);
        customer.setStatus("SUSPENDED");
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(kycRepository.existsByCustomerIdAndStatus(10L, "7")).thenReturn(false);
        KycVerification rejected = new KycVerification();
        rejected.setStatus("3");
        when(kycRepository.findByCustomerIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(rejected));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        when(bankingProvider.post(anyString(), anyMap())).thenReturn(successResponse("ONB-NEW-1"));
        when(kycRepository.save(any(KycVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        KycSubmitOutcome outcome = facade.submitOnboarding(
                10L, "ext-1", Map.of("mobile", "711666173"), null, "mohadz", "1234", null);

        assertEquals("ONB-NEW-1", outcome.onboardingRequestId());
        assertEquals("ACTIVE", customer.getStatus());
        verify(onboardingCredentialsService).ensureCredentialsForSubmit(customer, "mohadz", "1234", true);
        verify(activityRecorder).record(10L, "KYC_RESUBMITTED", "KYC", "ONB-NEW-1");

        ArgumentCaptor<KycVerification> kycCaptor = ArgumentCaptor.forClass(KycVerification.class);
        verify(kycRepository).save(kycCaptor.capture());
        assertEquals("ONB-NEW-1", kycCaptor.getValue().getChoiceOnboardingRequestId());
        assertEquals("1", kycCaptor.getValue().getStatus());
    }

    @Test
    void pendingKyc_doesNotAllowOverwrite() {
        Customer customer = new Customer();
        customer.setId(11L);
        customer.setStatus("ACTIVE");
        when(customerRepository.findById(11L)).thenReturn(Optional.of(customer));
        when(kycRepository.existsByCustomerIdAndStatus(11L, "7")).thenReturn(false);
        KycVerification pending = new KycVerification();
        pending.setStatus("1");
        when(kycRepository.findByCustomerIdOrderByCreatedAtDesc(11L)).thenReturn(List.of(pending));

        when(bankingProvider.post(anyString(), anyMap())).thenReturn(successResponse("ONB-2"));
        when(kycRepository.save(any(KycVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        facade.submitOnboarding(11L, "ext-2", Map.of(), null, "user2", "1111", null);

        verify(onboardingCredentialsService).ensureCredentialsForSubmit(customer, "user2", "1111", false);
        verify(activityRecorder).record(11L, "KYC_SUBMITTED", "KYC", "ONB-2");
        verify(customerRepository, never()).save(customer);
    }

    @Test
    void approvedKyc_doesNotAllowOverwrite() {
        Customer customer = new Customer();
        customer.setId(12L);
        customer.setStatus("ACTIVE");
        when(customerRepository.findById(12L)).thenReturn(Optional.of(customer));
        when(kycRepository.existsByCustomerIdAndStatus(12L, "7")).thenReturn(true);

        when(bankingProvider.post(anyString(), anyMap())).thenReturn(successResponse("ONB-3"));
        when(kycRepository.save(any(KycVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        facade.submitOnboarding(12L, "ext-3", Map.of(), null, "user3", "2222", null);

        verify(onboardingCredentialsService).ensureCredentialsForSubmit(customer, "user3", "2222", false);
        verify(kycRepository, never()).findByCustomerIdOrderByCreatedAtDesc(anyLong());
    }

    @Test
    void deactivated_throwsBeforeCredentials() {
        Customer customer = new Customer();
        customer.setId(13L);
        customer.setStatus("DEACTIVATED");
        when(customerRepository.findById(13L)).thenReturn(Optional.of(customer));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.submitOnboarding(13L, "ext-4", Map.of(), null, "u", "1234", null));

        assertEquals("CUSTOMER_DEACTIVATED", ex.getCode());
        verify(onboardingCredentialsService, never())
                .ensureCredentialsForSubmit(any(), anyString(), anyString(), anyBoolean());
        verify(bankingProvider, never()).post(anyString(), anyMap());
    }

    private static ChoiceBankResponse successResponse(String onboardingRequestId) {
        ChoiceBankResponse response = new ChoiceBankResponse();
        response.setCode("00000");
        response.setMsg("ok");
        response.setData(Map.of("onboardingRequestId", onboardingRequestId));
        return response;
    }
}
