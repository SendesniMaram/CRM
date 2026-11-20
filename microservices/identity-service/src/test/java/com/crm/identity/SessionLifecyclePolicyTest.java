package com.crm.identity;

import com.crm.identity.dto.*;
import com.crm.identity.entity.*;
import com.crm.identity.enums.RoleType;
import com.crm.identity.exception.InvalidCredentialsException;
import com.crm.identity.repository.*;
import com.crm.identity.security.*;
import com.crm.identity.service.impl.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real services and JWT crypto over simulated repositories; no Spring context, DB or migration. */
class SessionLifecyclePolicyTest {
    final UserRepository users = mock(UserRepository.class);
    final RoleRepository roles = mock(RoleRepository.class);
    final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    final PasswordEncoder encoder = mock(PasswordEncoder.class);
    final EntityManager em = mock(EntityManager.class);
    final JwtService jwt = new JwtService("synthetic-session-lifecycle-test-secret-32-bytes");
    final AuthenticationServiceImpl auth = new AuthenticationServiceImpl(users, roles, encoder, tokens, jwt, em);
    final IdentityServiceImpl identity = new IdentityServiceImpl(users, roles, encoder, tokens, em);
    final AccessTokenValidationService validation = new AccessTokenValidationService(jwt, users, tokens);
    final List<RefreshToken> stored = new ArrayList<>();
    User worker, administrator;

    User account(long id, String name, RoleType role) {
        User user = new User(); user.setId(id); user.setUsername(name); user.setEmail(name + "@example.test");
        user.setPassword("synthetic-hash"); user.getRole().add(new Role(role.name(), role)); return user;
    }
    @BeforeEach void setup() {
        worker = account(2, "worker", RoleType.EMPLOYEE);
        administrator = account(1, "administrator", RoleType.SUPER_ADMIN);
        when(users.findByUsername("worker")).thenReturn(Optional.of(worker));
        when(users.findById(2L)).thenAnswer(i -> Optional.of(worker));
        when(users.lockById(2L)).thenAnswer(i -> Optional.of(worker));
        when(users.lockById(1L)).thenAnswer(i -> Optional.of(administrator));
        when(users.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(roles.lockByRoleType(RoleType.SUPER_ADMIN)).thenReturn(Optional.of(new Role("SUPER_ADMIN", RoleType.SUPER_ADMIN)));
        when(roles.findByRoleType(any())).thenAnswer(i -> Optional.of(new Role(i.<RoleType>getArgument(0).name(), i.getArgument(0))));
        when(encoder.matches("synthetic-password", "synthetic-hash")).thenReturn(true);
        when(tokens.save(any())).thenAnswer(i -> { RefreshToken row = i.getArgument(0); if (!stored.contains(row)) stored.add(row); return row; });
        when(tokens.findByToken(anyString())).thenAnswer(i -> stored.stream().filter(t -> t.getToken().equals(i.getArgument(0))).findFirst());
        when(tokens.findByTokenForUpdate(anyString())).thenAnswer(i -> stored.stream().filter(t -> t.getToken().equals(i.getArgument(0))).findFirst());
        when(tokens.hasActiveSession(anyLong(), anyString(), any())).thenAnswer(i -> stored.stream().anyMatch(t ->
                t.getUser().getId().equals(i.<Long>getArgument(0)) && Objects.equals(t.getSessionId(), i.getArgument(1))
                && !t.isRevoked() && t.getExpiryDate().isAfter(i.getArgument(2))));
        when(tokens.revokeAllByUser(any())).thenAnswer(i -> revoke(i.getArgument(0), null));
        when(tokens.revokeSession(any(), anyString())).thenAnswer(i -> revoke(i.getArgument(0), i.getArgument(1)));
    }
    int revoke(User user, String sid) {
        int count = 0;
        for (RefreshToken row : stored) if (row.getUser().getId().equals(user.getId()) && !row.isRevoked()
                && (sid == null || sid.equals(row.getSessionId()))) { row.setRevoked(true); count++; }
        return count;
    }
    LoginResponse login() {
        LoginRequest request = new LoginRequest(); request.setUsername("worker"); request.setPassword("synthetic-password");
        return auth.login(request);
    }
    RefreshTokenRequest refresh(String token) { RefreshTokenRequest r = new RefreshTokenRequest(); r.setRefreshToken(token); return r; }

    @Test void loginIssuesASessionBoundAccessTokenAndRevokesPreviousSessionsWithoutDeletion() {
        var first = login(); assertEquals(2L, validation.validate(first.getToken()).userId());
        var second = login(); assertNotEquals(jwt.parseAccessToken(first.getToken()).sessionId(), jwt.parseAccessToken(second.getToken()).sessionId());
        assertThrows(InvalidCredentialsException.class, () -> validation.validate(first.getToken()));
        assertThrows(InvalidCredentialsException.class, () -> auth.refreshToken(refresh(first.getRefreshToken())));
        assertDoesNotThrow(() -> validation.validate(second.getToken()));
        verify(tokens, never()).deleteByUser(any()); assertEquals(2, stored.size());
    }
    @Test void logoutRevokesAccessAndRefreshAndIsIdempotentWithoutAffectingANewLogin() {
        var first = login(); auth.logout(refresh(first.getRefreshToken())); auth.logout(refresh(first.getRefreshToken()));
        assertThrows(InvalidCredentialsException.class, () -> validation.validate(first.getToken()));
        assertThrows(InvalidCredentialsException.class, () -> auth.refreshToken(refresh(first.getRefreshToken())));
        var next = login(); auth.logout(refresh(first.getRefreshToken()));
        assertDoesNotThrow(() -> validation.validate(next.getToken()));
        assertDoesNotThrow(() -> auth.logout(refresh("unknown-synthetic-token")));
    }
    @Test void rotationRetainsSessionAndOldRefreshCannotRenewButCanLogoutThatSession() {
        var first = login(); var rotated = auth.refreshToken(refresh(first.getRefreshToken()));
        assertEquals(jwt.parseAccessToken(first.getToken()).sessionId(), jwt.parseAccessToken(rotated.getAccessToken()).sessionId());
        assertThrows(InvalidCredentialsException.class, () -> auth.refreshToken(refresh(first.getRefreshToken())));
        assertDoesNotThrow(() -> validation.validate(first.getToken())); // Access expires normally while session is active.
        auth.logout(refresh(first.getRefreshToken())); // Covers logout racing with rotation.
        assertThrows(InvalidCredentialsException.class, () -> validation.validate(rotated.getAccessToken()));
        assertThrows(InvalidCredentialsException.class, () -> auth.refreshToken(refresh(rotated.getRefreshToken())));
    }
    @Test void disableAndReenableNeverRestoreOldSessions() {
        var first = login(); identity.updateUserStatus(2L, false, administrator);
        assertThrows(InvalidCredentialsException.class, () -> auth.refreshToken(refresh(first.getRefreshToken())));
        identity.updateUserStatus(2L, true, administrator);
        assertThrows(InvalidCredentialsException.class, () -> validation.validate(first.getToken()));
        assertThrows(InvalidCredentialsException.class, () -> auth.refreshToken(refresh(first.getRefreshToken())));
        assertDoesNotThrow(() -> validation.validate(login().getToken()));
    }
    @Test void roleChangesInEitherDirectionRevokeOldTokensAndRequireLogin() {
        var first = login(); identity.updateUserRoles(2L, new AdminUserRolesRequest(Set.of(RoleType.CLIENT)), administrator);
        assertThrows(InvalidCredentialsException.class, () -> validation.validate(first.getToken()));
        assertThrows(InvalidCredentialsException.class, () -> auth.refreshToken(refresh(first.getRefreshToken())));
        var second = login(); assertEquals(List.of("CLIENT"), validation.validate(second.getToken()).roles());
        identity.updateUserRoles(2L, new AdminUserRolesRequest(Set.of(RoleType.CLIENT, RoleType.ADMIN)), administrator);
        assertThrows(InvalidCredentialsException.class, () -> validation.validate(second.getToken()));
        assertThrows(InvalidCredentialsException.class, () -> auth.refreshToken(refresh(second.getRefreshToken())));
    }
    @Test void unchangedRolesDoNotRevokeAnActiveSession() {
        var first = login(); identity.updateUserRoles(2L, new AdminUserRolesRequest(Set.of(RoleType.EMPLOYEE)), administrator);
        assertDoesNotThrow(() -> validation.validate(first.getToken()));
    }
    @Test void expiredAndLegacyRefreshRowsCannotRenew() {
        var first = login(); stored.get(0).setExpiryDate(LocalDateTime.now().minusSeconds(1));
        assertThrows(InvalidCredentialsException.class, () -> auth.refreshToken(refresh(first.getRefreshToken())));
        assertTrue(stored.get(0).isRevoked());
        var second = login(); stored.get(1).setSessionId(null);
        assertThrows(InvalidCredentialsException.class, () -> auth.refreshToken(refresh(second.getRefreshToken())));
    }
}
