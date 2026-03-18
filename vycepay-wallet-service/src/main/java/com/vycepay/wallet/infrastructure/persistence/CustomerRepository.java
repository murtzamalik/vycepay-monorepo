package com.vycepay.wallet.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vycepay.wallet.domain.model.Customer;

/**
 * Repository for customer records.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByExternalId(String externalId);
}
