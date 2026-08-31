package com.vycepay.wallet.application.service;

import com.vycepay.common.exception.BusinessException;
import com.vycepay.wallet.application.WalletAccountContext;
import com.vycepay.wallet.domain.model.KycVerification;
import com.vycepay.wallet.infrastructure.persistence.CustomerRepository;
import com.vycepay.wallet.infrastructure.persistence.KycVerificationRepository;
import com.vycepay.wallet.infrastructure.persistence.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

    /**
     * Loads customer, wallet, and KYC (preferring the wallet's row that has an ID number)
     * for Choice Bank account operations.
     */
@Service
public class WalletAccountContextService {

    private final CustomerRepository customerRepository;
    private final WalletRepository walletRepository;
    private final KycVerificationRepository kycVerificationRepository;

    public WalletAccountContextService(CustomerRepository customerRepository,
                                       WalletRepository walletRepository,
                                       KycVerificationRepository kycVerificationRepository) {
        this.customerRepository = customerRepository;
        this.walletRepository = walletRepository;
        this.kycVerificationRepository = kycVerificationRepository;
    }

    /**
     * Resolves context for the authenticated customer (X-Customer-Id external id).
     */
    public WalletAccountContext requireContext(String externalId) {
        var customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        var wallet = walletRepository.findByCustomerId(customer.getId())
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Wallet not found", HttpStatus.NOT_FOUND));
        List<KycVerification> list = kycVerificationRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        KycVerification kyc = selectKyc(list, wallet.getChoiceAccountId());
        return new WalletAccountContext(customer.getId(), customer, wallet, kyc);
    }

    /**
     * Prefer the KYC row that belongs to this wallet and already has an ID number.
     * Newest-first list from the repository.
     */
    static KycVerification selectKyc(List<KycVerification> list, String walletAccountId) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        KycVerification matchingWithId = null;
        KycVerification anyWithId = null;
        KycVerification matching = null;
        for (KycVerification kyc : list) {
            boolean matchesAccount = walletAccountId != null
                    && walletAccountId.equals(kyc.getChoiceAccountId());
            boolean hasId = hasIdNumber(kyc);
            if (matchesAccount && hasId) {
                return kyc;
            }
            if (anyWithId == null && hasId) {
                anyWithId = kyc;
            }
            if (matching == null && matchesAccount) {
                matching = kyc;
            }
        }
        if (anyWithId != null) {
            return anyWithId;
        }
        if (matching != null) {
            return matching;
        }
        return list.get(0);
    }

    private static boolean hasIdNumber(KycVerification kyc) {
        return kyc.getIdNumber() != null && !kyc.getIdNumber().isBlank();
    }
}
