package com.crm.hr.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Shared existing HR authorization rules, independent of association resolution. */
final class HrAuthorization {
    private HrAuthorization() {}

    static Authentication authentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new AccessDeniedException("Authentication required");
        }
        return auth;
    }

    static boolean isAdmin() {
        return authentication().getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    static void requireAdmin() {
        if (!isAdmin()) throw new AccessDeniedException("Administrative access required");
    }
}
