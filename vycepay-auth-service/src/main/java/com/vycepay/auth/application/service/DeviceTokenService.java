package com.vycepay.auth.application.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.vycepay.auth.domain.model.DeviceToken;
import com.vycepay.auth.infrastructure.persistence.DeviceTokenRepository;

import jakarta.persistence.EntityManager;

/**
 * Manages FCM device tokens: one active push target per customer.
 * Mobile registers via verify-otp / login; logout clears all tokens.
 */
@Service
public class DeviceTokenService {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenService.class);
    private static final String DEFAULT_PLATFORM = "ANDROID";

    private final DeviceTokenRepository deviceTokenRepository;
    private final EntityManager entityManager;

    public DeviceTokenService(DeviceTokenRepository deviceTokenRepository, EntityManager entityManager) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.entityManager = entityManager;
    }

    /**
     * Ensures a single FCM token row for the customer.
     * Same token again updates platform only (avoids uk_customer_token duplicate on login).
     * No-op when {@code fcmToken} is null or blank.
     *
     * @param customerId VycePay customer id
     * @param fcmToken   Firebase registration token
     * @param platform   ANDROID or IOS; defaults to ANDROID when blank
     */
    @Transactional
    public void replaceTokenForCustomer(Long customerId, String fcmToken, String platform) {
        if (customerId == null || !StringUtils.hasText(fcmToken)) {
            return;
        }
        String trimmed = fcmToken.trim();
        String resolvedPlatform = StringUtils.hasText(platform) ? platform.trim() : DEFAULT_PLATFORM;

        Optional<DeviceToken> sameToken = deviceTokenRepository.findByCustomerIdAndFcmToken(customerId, trimmed);
        List<DeviceToken> existing = deviceTokenRepository.findByCustomerId(customerId);

        for (DeviceToken row : existing) {
            if (sameToken.isPresent() && row.getId().equals(sameToken.get().getId())) {
                continue;
            }
            deviceTokenRepository.delete(row);
        }
        // Flush deletes before insert so uk_customer_token is not violated in the same txn
        entityManager.flush();

        if (sameToken.isPresent()) {
            DeviceToken token = sameToken.get();
            token.setPlatform(resolvedPlatform);
            deviceTokenRepository.save(token);
        } else {
            DeviceToken token = new DeviceToken();
            token.setCustomerId(customerId);
            token.setFcmToken(trimmed);
            token.setPlatform(resolvedPlatform);
            deviceTokenRepository.save(token);
        }
        log.info("Replaced FCM token for customerId={} platform={}", customerId, resolvedPlatform);
    }

    /**
     * Clears all push tokens for the customer (logout).
     */
    @Transactional
    public void clearTokensForCustomer(Long customerId) {
        if (customerId == null) {
            return;
        }
        deviceTokenRepository.deleteByCustomerId(customerId);
        entityManager.flush();
        log.info("Cleared FCM tokens for customerId={}", customerId);
    }
}
