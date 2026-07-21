package com.vycepay.auth.application.service;

import com.vycepay.auth.domain.model.DeviceToken;
import com.vycepay.auth.infrastructure.persistence.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Manages FCM device tokens: one active push target per customer.
 * Mobile registers via verify-otp; logout clears all tokens.
 */
@Service
public class DeviceTokenService {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenService.class);
    private static final String DEFAULT_PLATFORM = "ANDROID";

    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceTokenService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    /**
     * Replaces all tokens for the customer with a single new FCM token.
     * No-op when {@code fcmToken} is null or blank (login still succeeds without push).
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
        String resolvedPlatform = StringUtils.hasText(platform) ? platform.trim() : DEFAULT_PLATFORM;
        deviceTokenRepository.deleteByCustomerId(customerId);
        DeviceToken token = new DeviceToken();
        token.setCustomerId(customerId);
        token.setFcmToken(fcmToken.trim());
        token.setPlatform(resolvedPlatform);
        deviceTokenRepository.save(token);
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
        log.info("Cleared FCM tokens for customerId={}", customerId);
    }
}
