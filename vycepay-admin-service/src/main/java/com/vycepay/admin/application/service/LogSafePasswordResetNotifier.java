package com.vycepay.admin.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Development-safe fallback that records reset intent but never logs the raw reset token. */
@Service
public class LogSafePasswordResetNotifier implements AdminPasswordResetNotifier {
    private static final Logger log = LoggerFactory.getLogger(LogSafePasswordResetNotifier.class);

    @Override
    public void notifyResetToken(Long adminUserId, String email, String rawToken) {
        log.info("Admin password reset token created for adminUserId={} emailHash={} delivery=pending-email-provider",
                adminUserId, Integer.toHexString(String.valueOf(email).hashCode()));
    }
}
