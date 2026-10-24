package com.crm.identity.service.impl;

import com.crm.identity.dto.RefreshTokenRequest;
import com.crm.identity.entity.RefreshToken;
import com.crm.identity.entity.Role;
import com.crm.identity.entity.User;
import com.crm.identity.enums.RoleType;
import com.crm.identity.exception.InvalidCredentialsException;
import com.crm.identity.repository.RefreshTokenRepository;
import com.crm.identity.repository.RoleRepository;
import com.crm.identity.repository.UserRepository;
import com.crm.identity.security.JwtService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenSecurityTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock PasswordEncoder encoder;
    @Mock RefreshTokenRepository tokens;
    @Mock JwtService jwt;
    @InjectMocks AuthenticationServiceImpl service;

    private RefreshToken token(boolean enabled, boolean unlocked) {
        User user = new User();
        user.setId(101L);
        user.setUsername("synthetic-user");
        user.setEmail("synthetic@example.test");
        user.setEnabled(enabled);
        user.setAccountNonLocked(unlocked);
        user.getRole().add(new Role("EMPLOYEE", RoleType.EMPLOYEE));
        RefreshToken stored = new RefreshToken();
        stored.setToken("synthetic-refresh");
        stored.setExpiryDate(LocalDateTime.now().plusDays(1));
        stored.setUser(user);
        when(tokens.findByTokenForUpdate(stored.getToken())).thenReturn(Optional.of(stored));
        return stored;
    }

    private RefreshTokenRequest request() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("synthetic-refresh");
        return request;
    }

    @ParameterizedTest
    @CsvSource({"false,true", "true,false", "false,false"})
    void inactiveAccountRevokesAllRefreshSessionsWithoutIssuingTokens(boolean enabled, boolean unlocked) {
        RefreshToken stored = token(enabled, unlocked);
        assertThatThrownBy(() -> service.refreshToken(request())).isInstanceOf(InvalidCredentialsException.class);
        verify(tokens).revokeAllByUser(stored.getUser());
        verify(tokens, never()).save(any());
        verifyNoInteractions(jwt);
    }

    @Test
    void activeAccountRotatesTokenAndPreservesRolesAndResponseContract() {
        RefreshToken stored = token(true, true);
        when(tokens.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwt.generateToken(101L, "synthetic-user", "synthetic@example.test", true, List.of("EMPLOYEE")))
                .thenReturn("synthetic-access");
        var result = service.refreshToken(request());
        assertThat(stored.isRevoked()).isTrue();
        assertThat(result.getAccessToken()).isEqualTo("synthetic-access");
        assertThat(result.getRefreshToken()).isNotBlank().isNotEqualTo(stored.getToken());
        assertThat(result.getRoles()).containsExactly("EMPLOYEE");
        assertThat(result.getType()).isEqualTo("Bearer");
        assertThat(result.getExpiration()).isEqualTo(JwtService.ACCESS_TOKEN_DURATION_MS);
        verify(tokens, times(2)).save(any());
        verify(tokens, never()).revokeAllByUser(any());
    }

    @Test
    void revokedTokenCannotBeConsumed() {
        RefreshToken stored = token(true, true);
        stored.setRevoked(true);
        assertThatThrownBy(() -> service.refreshToken(request())).isInstanceOf(InvalidCredentialsException.class);
        verify(tokens, never()).save(any());
        verifyNoInteractions(jwt);
    }

    @Test
    void expiredTokenIsRevokedWithoutIssuingTokens() {
        RefreshToken stored = token(true, true);
        stored.setExpiryDate(LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> service.refreshToken(request())).isInstanceOf(InvalidCredentialsException.class);
        assertThat(stored.isRevoked()).isTrue();
        verify(tokens).save(stored);
        verifyNoInteractions(jwt);
    }

    @Test
    void unknownTokenCannotIssueTokens() {
        when(tokens.findByTokenForUpdate(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refreshToken(request())).isInstanceOf(InvalidCredentialsException.class);
        verify(tokens, never()).save(any());
        verifyNoInteractions(jwt);
    }

    @Test
    void missingAccountCannotIssueTokens() {
        RefreshToken stored = token(true, true);
        stored.setUser(null);
        assertThatThrownBy(() -> service.refreshToken(request())).isInstanceOf(InvalidCredentialsException.class);
        verify(tokens, never()).save(any());
        verifyNoInteractions(jwt);
    }
}
