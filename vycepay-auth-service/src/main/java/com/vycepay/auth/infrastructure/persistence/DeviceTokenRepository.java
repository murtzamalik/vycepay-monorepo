package com.vycepay.auth.infrastructure.persistence;

import com.vycepay.auth.domain.model.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for FCM device token records.
 */
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findByCustomerId(Long customerId);

    Optional<DeviceToken> findByIdAndCustomerId(Long id, Long customerId);

    Optional<DeviceToken> findByCustomerIdAndFcmToken(Long customerId, String fcmToken);
}
