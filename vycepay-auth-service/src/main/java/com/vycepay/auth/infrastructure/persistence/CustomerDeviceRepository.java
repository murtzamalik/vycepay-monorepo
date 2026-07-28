package com.vycepay.auth.infrastructure.persistence;

import com.vycepay.auth.domain.model.CustomerDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for the single bound device per customer.
 */
public interface CustomerDeviceRepository extends JpaRepository<CustomerDevice, Long> {

    Optional<CustomerDevice> findByCustomerId(Long customerId);
}
