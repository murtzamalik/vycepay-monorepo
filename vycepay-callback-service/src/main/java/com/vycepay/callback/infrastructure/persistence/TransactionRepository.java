package com.vycepay.callback.infrastructure.persistence;

import com.vycepay.callback.domain.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository for transaction records. */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByChoiceTxId(String choiceTxId);

    Optional<Transaction> findByChoiceRequestId(String choiceRequestId);
}
