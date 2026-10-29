package com.vycepay.common.apprelease;

import java.util.Optional;

/**
 * Loads an active app release policy row by platform.
 */
public interface AppReleasePolicyPort {

    /**
     * @param platform ANDROID or IOS (case-insensitive)
     * @return active policy, or empty when missing / inactive
     */
    Optional<AppReleasePolicy> findActiveByPlatform(String platform);
}
