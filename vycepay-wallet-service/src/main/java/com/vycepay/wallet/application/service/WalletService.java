package com.vycepay.wallet.application.service;

import org.springframework.stereotype.Service;

import com.vycepay.wallet.domain.model.Wallet;
import com.vycepay.wallet.infrastructure.persistence.CustomerRepository;
import com.vycepay.wallet.infrastructure.persistence.WalletRepository;

/**
 * Provides wallet/balance for customers.
 * Balance is cached from Choice Bank callback 0003.
 */
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final CustomerRepository customerRepository;

    public WalletService(WalletRepository walletRepository, CustomerRepository customerRepository) {
        this.walletRepository = walletRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Returns wallet for customer by external ID.
     *
     * @param externalId Customer external ID from JWT
     * @return Wallet or empty if not yet created (KYC pending)
     */
    public java.util.Optional<Wallet> getWalletByCustomerExternalId(String externalId) {
        return customerRepository.findByExternalId(externalId)
                .flatMap(c -> walletRepository.findByCustomerId(c.getId()));
    }
}
