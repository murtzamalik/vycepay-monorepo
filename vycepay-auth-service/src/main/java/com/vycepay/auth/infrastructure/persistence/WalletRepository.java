package com.vycepay.auth.infrastructure.persistence;

import com.vycepay.auth.domain.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Read-only wallet lookups for contact verification.
 */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * Finds wallets for the given customers filtered by status (e.g. ACTIVE).
     */
    List<Wallet> findByCustomerIdInAndStatus(Collection<Long> customerIds, String status);
}
