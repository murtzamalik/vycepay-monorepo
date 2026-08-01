package com.vycepay.callback.infrastructure.persistence;

import com.vycepay.callback.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Read access to customer for external_id resolution and compose validation.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByExternalId(String externalId);

    List<Customer> findByIdIn(Collection<Long> ids);
}
