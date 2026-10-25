package com.crm.security;

import java.security.Principal;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Immutable identity built by the JWT filter only after Identity confirmation. */
public record AuthenticatedUserPrincipal(long userId, String username,
        List<SimpleGrantedAuthority> authorities) implements Principal {
    public AuthenticatedUserPrincipal {
        if (userId <= 0 || username == null || username.isBlank()) {
            throw new IllegalArgumentException("A validated user identity is required");
        }
        authorities = List.copyOf(authorities);
    }

    /** Preserve the existing username contract of Authentication.getName(). */
    @Override
    public String getName() {
        return username;
    }

    /** No fallback to request headers, usernames or unvalidated token claims. */
    public static AuthenticatedUserPrincipal require(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new AccessDeniedException("Validated user identity required");
        }
        return principal;
    }
}
