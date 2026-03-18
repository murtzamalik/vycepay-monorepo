package com.vycepay.wallet.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vycepay.wallet.domain.model.Wallet;

/**
 * Repository for wallet records.
 */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByCustomerId(Long customerId);

    Optional<Wallet> findByChoiceAccountId(String choiceAccountId);
}
