package com.vycepay.wallet.application.service;

import com.vycepay.wallet.domain.model.KycVerification;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class WalletAccountContextServiceTest {

    @Test
    void selectKyc_prefersWalletMatchWithIdNumber() {
        KycVerification newerWithoutId = kyc("acct-new", null);
        KycVerification walletWithId = kyc("acct-wallet", "123");
        KycVerification olderWithId = kyc("other", "999");

        KycVerification picked = WalletAccountContextService.selectKyc(
                List.of(newerWithoutId, walletWithId, olderWithId), "acct-wallet");

        assertSame(walletWithId, picked);
    }

    @Test
    void selectKyc_whenNoMatch_usesAnyRowWithId() {
        KycVerification blank = kyc(null, null);
        KycVerification withId = kyc("other", "123");

        assertSame(withId, WalletAccountContextService.selectKyc(List.of(blank, withId), "acct-wallet"));
    }

    @Test
    void selectKyc_empty_returnsNull() {
        assertNull(WalletAccountContextService.selectKyc(List.of(), "acct"));
    }

    @Test
    void selectKyc_onlyBlank_returnsNewest() {
        KycVerification newest = kyc("acct-wallet", null);
        KycVerification older = kyc("acct-wallet", "  ");
        assertEquals(newest, WalletAccountContextService.selectKyc(List.of(newest, older), "acct-wallet"));
    }

    private static KycVerification kyc(String accountId, String idNumber) {
        KycVerification k = new KycVerification();
        k.setChoiceAccountId(accountId);
        k.setIdNumber(idNumber);
        return k;
    }
}
