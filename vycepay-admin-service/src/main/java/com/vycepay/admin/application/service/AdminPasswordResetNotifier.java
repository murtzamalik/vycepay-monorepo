package com.vycepay.admin.application.service;

/** Delivers password reset links without exposing raw reset tokens through API responses. */
public interface AdminPasswordResetNotifier {
    void notifyResetToken(Long adminUserId, String email, String rawToken);
}
