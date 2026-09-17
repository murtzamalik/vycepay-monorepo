package com.vycepay.wallet.application.service;

import com.vycepay.common.choicebank.errors.ChoiceBankResult;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.wallet.application.WalletAccountContext;
import com.vycepay.wallet.application.facade.AccountManagementFacade;
import com.vycepay.wallet.domain.model.Customer;
import com.vycepay.wallet.domain.model.Wallet;
import com.vycepay.wallet.infrastructure.persistence.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletBalanceRefreshServiceTest {

    @Mock
    private WalletAccountContextService contextService;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private AccountManagementFacade accountManagementFacade;

    @Test
    void extractBalance_prefersBalanceKey() {
        assertEquals(new BigDecimal("100.50"),
                WalletBalanceRefreshService.extractBalance(Map.of("balance", "100.50")));
    }

    @Test
    void extractBalance_fallsBackToAvailableBalance() {
        BigDecimal extracted = WalletBalanceRefreshService.extractBalance(Map.of("availableBalance", 42.00));
        assertEquals(0, new BigDecimal("42.00").compareTo(extracted));
    }

    @Test
    void extractBalance_nestedAccount() {
        assertEquals(new BigDecimal("7.25"),
                WalletBalanceRefreshService.extractBalance(
                        Map.of("account", Map.of("currentBalance", "7.25"))));
    }

    @Test
    void extractBalance_missing_returnsNull() {
        assertNull(WalletBalanceRefreshService.extractBalance(Map.of("email", "a@b.com")));
    }

    @Test
    void refreshFromChoice_updatesCache() {
        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setChoiceAccountId("46012001327510");
        wallet.setBalanceCache(BigDecimal.ZERO);
        wallet.setCurrency("KES");
        wallet.setStatus("ACTIVE");

        Customer customer = new Customer();
        customer.setId(10L);
        WalletAccountContext ctx = new WalletAccountContext(10L, customer, wallet, null);

        Map<String, Object> choiceData = new HashMap<>();
        choiceData.put("balance", "322.00");
        choiceData.put("accountId", "46012001327510");

        when(contextService.requireContext("cust-ext")).thenReturn(ctx);
        when(accountManagementFacade.getAccountDetails(ctx))
                .thenReturn(new ChoiceBankResult(choiceData, "OK", "req-1"));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletBalanceRefreshService service =
                new WalletBalanceRefreshService(contextService, walletRepository, accountManagementFacade);

        Wallet updated = service.refreshFromChoice("cust-ext");

        assertEquals(new BigDecimal("322.00"), updated.getBalanceCache());
        assertNotNull(updated.getLastBalanceUpdateAt());
        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());
        assertEquals(new BigDecimal("322.00"), captor.getValue().getBalanceCache());
    }

    @Test
    void refreshFromChoice_choiceWithoutBalance_doesNotSave() {
        Wallet wallet = new Wallet();
        wallet.setChoiceAccountId("46012001327510");
        wallet.setBalanceCache(new BigDecimal("10.00"));
        WalletAccountContext ctx = new WalletAccountContext(10L, new Customer(), wallet, null);

        when(contextService.requireContext("cust-ext")).thenReturn(ctx);
        when(accountManagementFacade.getAccountDetails(ctx))
                .thenReturn(new ChoiceBankResult(Map.of("accountId", "x"), "OK", "req-1"));

        WalletBalanceRefreshService service =
                new WalletBalanceRefreshService(contextService, walletRepository, accountManagementFacade);

        assertThrows(BusinessException.class, () -> service.refreshFromChoice("cust-ext"));
        verify(walletRepository, never()).save(any());
        assertEquals(new BigDecimal("10.00"), wallet.getBalanceCache());
    }
}
