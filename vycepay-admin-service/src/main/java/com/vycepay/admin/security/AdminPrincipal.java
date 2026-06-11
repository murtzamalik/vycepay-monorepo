package com.vycepay.admin.security;

import java.util.Set;

/** Authenticated backoffice operator loaded from admin_session and admin role permissions. */
public record AdminPrincipal(Long id, String externalId, String username, String email, String fullName,
                             Set<String> roles, Set<String> permissions, String jti) {
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
