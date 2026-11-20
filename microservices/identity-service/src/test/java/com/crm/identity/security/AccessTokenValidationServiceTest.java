package com.crm.identity.security;

import com.crm.identity.entity.Role;
import com.crm.identity.entity.User;
import com.crm.identity.enums.RoleType;
import com.crm.identity.exception.InvalidCredentialsException;
import com.crm.identity.repository.UserRepository;
import com.crm.identity.repository.RefreshTokenRepository;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** No context or database: final policy exercised by both HTTP filters below. */
class AccessTokenValidationServiceTest {
    static final String SID = "11111111-1111-4111-8111-111111111111";
    final JwtService jwt = mock(JwtService.class);
    final UserRepository users = mock(UserRepository.class);
    final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    final AccessTokenValidationService service = new AccessTokenValidationService(jwt, users, tokens);
    User user;
    @BeforeEach void setup() {
        user = new User(); user.setId(101L); user.setUsername("employee");
        user.getRole().add(new Role("EMPLOYEE", RoleType.EMPLOYEE));
        when(jwt.parseAccessToken("token")).thenReturn(new JwtService.AccessTokenClaims(101, "employee", List.of("EMPLOYEE"), Instant.now().plusSeconds(60), SID));
        when(users.findById(101L)).thenReturn(Optional.of(user));
        when(tokens.hasActiveSession(eq(101L),eq(SID),any())).thenReturn(true);
    }
    @Test void acceptsTheSameIdentityAndSignedRolesForInternalAndPublicRequests() {
        assertThat(service.validate("token").roles()).containsExactly("EMPLOYEE");
        assertThat(service.authenticatedUser("token")).isSameAs(user);
        verify(users, never()).findByUsername(anyString());
        verify(users, never()).findByEmail(anyString());
    }
    @Test void addedAndRemovedRolesInvalidateOldTokensInsteadOfAddingPrivileges() {
        user.getRole().add(new Role("ADMIN", RoleType.ADMIN));
        assertThatThrownBy(() -> service.validate("token")).isInstanceOf(InvalidCredentialsException.class);
        user.getRole().clear();
        assertThatThrownBy(() -> service.authenticatedUser("token")).isInstanceOf(InvalidCredentialsException.class);
    }
    @ParameterizedTest @ValueSource(strings={"disabled","locked","renamed","deleted","revoked","expired"})
    void rejectsUnavailableOrObsoleteIdentity(String state) {
        switch(state) {
            case "disabled" -> user.setEnabled(false);
            case "locked" -> user.setAccountNonLocked(false);
            case "renamed" -> user.setUsername("different");
            case "deleted" -> when(users.findById(101L)).thenReturn(Optional.empty());
            case "revoked" -> when(tokens.hasActiveSession(eq(101L),eq(SID),any())).thenReturn(false);
            case "expired" -> when(jwt.parseAccessToken("token")).thenReturn(new JwtService.AccessTokenClaims(101,"employee",List.of("EMPLOYEE"),Instant.now().minusSeconds(1),SID));
        }
        assertThatThrownBy(() -> service.validate("token")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.authenticatedUser("token")).isInstanceOf(InvalidCredentialsException.class);
    }
    @Test void invalidCryptographyNeverQueriesAnAccount() {
        when(jwt.parseAccessToken("invalid")).thenThrow(new InvalidCredentialsException("Invalid access token"));
        assertThatThrownBy(() -> service.validate("invalid")).isInstanceOf(InvalidCredentialsException.class);
        verify(users,never()).findById(anyLong()); verifyNoInteractions(tokens);
    }
}
