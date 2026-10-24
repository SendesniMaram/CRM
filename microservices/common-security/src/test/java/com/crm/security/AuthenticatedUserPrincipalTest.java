package com.crm.security;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.assertj.core.api.Assertions.*;

class AuthenticatedUserPrincipalTest {
    @Test void identityAndAuthoritiesRemainImmutableAndUsernameCompatible() {
        var authorities = new ArrayList<>(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
        var principal = new AuthenticatedUserPrincipal(103L, "employee-name", authorities);
        authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
        assertThat(AuthenticatedUserPrincipal.require(authentication)).isSameAs(principal);
        assertThat(principal.userId()).isEqualTo(103L);
        assertThat(authentication.getName()).isEqualTo("employee-name");
        assertThat(authentication.getCredentials()).isNull();
        assertThat(principal.authorities()).extracting("authority").containsExactly("ROLE_EMPLOYEE");
        assertThatThrownBy(() -> principal.authorities().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void accessorRejectsMissingAnonymousLegacyAndUnauthenticatedIdentities() {
        var principal = new AuthenticatedUserPrincipal(42L, "alice", List.of());
        assertThatThrownBy(() -> AuthenticatedUserPrincipal.require(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> AuthenticatedUserPrincipal.require(
                new UsernamePasswordAuthenticationToken("alice", null, List.of())))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> AuthenticatedUserPrincipal.require(
                new UsernamePasswordAuthenticationToken(principal, null)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> AuthenticatedUserPrincipal.require(
                new AnonymousAuthenticationToken("test-key", principal, List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test void invalidIdentityCannotBeRepresented() {
        assertThatThrownBy(() -> new AuthenticatedUserPrincipal(0, "alice", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthenticatedUserPrincipal(-1, "alice", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthenticatedUserPrincipal(42, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthenticatedUserPrincipal(42, " ", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
