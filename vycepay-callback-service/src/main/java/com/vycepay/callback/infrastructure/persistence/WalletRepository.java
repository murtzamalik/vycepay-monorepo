package com.vycepay.callback.infrastructure.persistence;

import com.vycepay.callback.domain.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository for wallet records. */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByChoiceAccountId(String choiceAccountId);
}
