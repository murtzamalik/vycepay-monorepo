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
 * Loads customer, wallet, and latest KYC for Choice Bank account operations.
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
        KycVerification latest = list.isEmpty() ? null : list.get(0);
        return new WalletAccountContext(customer.getId(), customer, wallet, latest);
    }
}
