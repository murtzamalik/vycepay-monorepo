package com.vycepay.admin.application.service;

import com.vycepay.admin.security.AdminPrincipal;
import com.vycepay.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the authenticated admin principal at service boundaries. */
@Component
public class AdminSecurityContext {
    public AdminPrincipal currentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AdminPrincipal principal)) {
            throw new BusinessException("ADMIN_UNAUTHENTICATED", "Admin authentication is required", HttpStatus.UNAUTHORIZED);
        }
        return principal;
    }
    public boolean hasPermission(String permission) { return currentAdmin().hasPermission(permission); }
}
