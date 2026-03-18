package com.vycepay.auth.infrastructure.persistence;

import com.vycepay.auth.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for customer records.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByExternalId(String externalId);

    Optional<Customer> findByMobileCountryCodeAndMobile(String mobileCountryCode, String mobile);
}
