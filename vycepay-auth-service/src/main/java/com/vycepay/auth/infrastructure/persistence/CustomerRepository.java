package com.vycepay.auth.infrastructure.persistence;

import com.vycepay.auth.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for customer records.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByExternalId(String externalId);

    Optional<Customer> findByMobileCountryCodeAndMobile(String mobileCountryCode, String mobile);

    Optional<Customer> findByUsernameNormalized(String usernameNormalized);

    boolean existsByUsernameNormalized(String usernameNormalized);

    /**
     * Batch lookup for contact verify: customers with username set, excluding the caller.
     */
    @Query("""
            SELECT c FROM Customer c
            WHERE c.mobileCountryCode = :cc
              AND c.mobile IN :mobiles
              AND c.username IS NOT NULL
              AND c.username <> ''
              AND c.id <> :excludeCustomerId
            """)
    List<Customer> findWithUsernameByCountryAndMobilesExcluding(
            @Param("cc") String mobileCountryCode,
            @Param("mobiles") Collection<String> mobiles,
            @Param("excludeCustomerId") Long excludeCustomerId);
}
