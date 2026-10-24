package com.crm.identity.security;

import com.crm.identity.entity.Role;
import com.crm.identity.entity.User;
import com.crm.identity.enums.RoleType;
import com.crm.identity.exception.InvalidCredentialsException;
import com.crm.identity.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessTokenValidationServiceTest {
    @Mock JwtService jwt;
    @Mock UserRepository users;

    @Test void rereadsCurrentStateAndDoesNotAddNewDatabaseRoles() {
        var service = new AccessTokenValidationService(jwt, users);
        when(jwt.parseAccessToken("synthetic-token"))
                .thenReturn(new JwtService.AccessTokenClaims(101, "employee", List.of("EMPLOYEE"), Instant.now().plusSeconds(60)));
        User user = new User(); user.setId(101L); user.setUsername("employee");
        user.getRole().add(new Role("EMPLOYEE", RoleType.EMPLOYEE));
        user.getRole().add(new Role("ADMIN", RoleType.ADMIN));
        when(users.findById(101L)).thenReturn(Optional.of(user));
        assertThat(service.validate("synthetic-token").roles()).containsExactly("EMPLOYEE");
        user.setAccountNonLocked(false);
        assertThatThrownBy(() -> service.validate("synthetic-token")).isInstanceOf(InvalidCredentialsException.class);
        verify(users, times(2)).findById(101L);
        verify(users, never()).findByUsername(anyString());
        verify(users, never()).findByEmail(anyString());
    }

    @Test void invalidCryptographyNeverQueriesAnAccount() {
        var service = new AccessTokenValidationService(jwt, users);
        when(jwt.parseAccessToken("invalid")).thenThrow(new InvalidCredentialsException("Invalid access token"));
        assertThatThrownBy(() -> service.validate("invalid")).isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(users);
    }

    @Test void tokenExpiringDuringAccountLookupCannotBeAccepted() {
        var service = new AccessTokenValidationService(jwt, users);
        // Model a successful parse followed by a lookup completing after expiration, without sleeping.
        when(jwt.parseAccessToken("token")).thenReturn(new JwtService.AccessTokenClaims(
                101, "employee", List.of("EMPLOYEE"), Instant.now().minusSeconds(1)));
        User user = new User(); user.setUsername("employee");
        user.getRole().add(new Role("EMPLOYEE", RoleType.EMPLOYEE));
        when(users.findById(101L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.validate("token")).isInstanceOf(InvalidCredentialsException.class);
    }
}
