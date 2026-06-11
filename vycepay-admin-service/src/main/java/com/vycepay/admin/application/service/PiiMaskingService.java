package com.vycepay.admin.application.service;

import org.springframework.stereotype.Service;

/** Applies server-side masking before DTOs leave the admin service. */
@Service
public class PiiMaskingService {
    public String maskMobile(String countryCode, String mobile, boolean canViewPii) {
        if (canViewPii || mobile == null || mobile.length() <= 2) { return countryCode != null ? countryCode + mobile : mobile; }
        return (countryCode != null ? countryCode + " " : "") + "*** *** " + mobile.substring(mobile.length() - 2);
    }
    public String maskEmail(String email, boolean canViewPii) {
        if (canViewPii || email == null || email.isBlank()) { return email; }
        int at = email.indexOf('@');
        if (at <= 1) { return "***"; }
        return email.charAt(0) + "***" + email.substring(at);
    }
    public String maskIdNumber(String idNumber, boolean canViewPii) { return canViewPii || idNumber == null || idNumber.isBlank() ? idNumber : "*******"; }
    public String maskAccount(String account, boolean canViewPii) { return canViewPii || account == null || account.length() <= 4 ? account : "****" + account.substring(account.length() - 4); }
}
