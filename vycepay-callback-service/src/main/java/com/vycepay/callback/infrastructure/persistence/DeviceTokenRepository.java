package com.vycepay.callback.infrastructure.persistence;

import com.vycepay.callback.domain.model.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Access to shared {@code device_token} rows for FCM fan-out and invalid-token cleanup.
 */
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findByCustomerId(Long customerId);

    Optional<DeviceToken> findByCustomerIdAndFcmToken(Long customerId, String fcmToken);

    void deleteByFcmToken(String fcmToken);
}
