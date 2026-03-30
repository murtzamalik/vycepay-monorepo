package com.vycepay.wallet.application;

import com.vycepay.wallet.domain.model.Customer;
import com.vycepay.wallet.domain.model.KycVerification;
import com.vycepay.wallet.domain.model.Wallet;

/**
 * Resolved customer, wallet, and latest KYC row for Choice account APIs.
 */
public record WalletAccountContext(
        Long customerId,
        Customer customer,
        Wallet wallet,
        KycVerification latestKyc) {

    public String choiceAccountId() {
        return wallet.getChoiceAccountId();
    }

    public String choiceUserIdOrThrow() {
        if (latestKyc == null || latestKyc.getChoiceUserId() == null || latestKyc.getChoiceUserId().isBlank()) {
            return null;
        }
        return latestKyc.getChoiceUserId();
    }
}
