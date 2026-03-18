package com.vycepay.transaction.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.vycepay.transaction.domain.model.Transaction;

/**
 * Repository for transaction records. Extends JpaSpecificationExecutor for complex queries.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByExternalIdAndCustomerId(String externalId, Long customerId);

    Page<Transaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
}
