package com.vycepay.common.sms.template;

import java.util.Optional;

/**
 * Loads the active SMS template body for a catalog key.
 */
public interface SmsTemplatePort {

    /**
     * @return active body for key, or empty when missing / inactive
     */
    Optional<String> findActiveBody(String templateKey);
}
