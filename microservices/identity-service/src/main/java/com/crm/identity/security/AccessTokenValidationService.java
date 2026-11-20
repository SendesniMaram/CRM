package com.crm.identity.security;

import com.crm.identity.entity.User;
import com.crm.identity.exception.InvalidCredentialsException;
import com.crm.identity.repository.UserRepository;
import com.crm.identity.repository.RefreshTokenRepository;
import java.util.List;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Final policy shared by Identity requests and all downstream access checks. No positive cache. */
@Service
public class AccessTokenValidationService {
    private final JwtService jwt;
    private final UserRepository users;
    private final RefreshTokenRepository sessions;
    public AccessTokenValidationService(JwtService jwt, UserRepository users, RefreshTokenRepository sessions) {
        this.jwt = jwt; this.users = users; this.sessions = sessions;
    }
    @Transactional(readOnly = true)
    public User authenticatedUser(String token) {
        var claims = jwt.parseAccessToken(token);
        User user = users.findById(claims.userId()).orElseThrow(() -> invalid());
        var roles = user.getRole().stream().map(r -> r.getRoleType().name()).collect(java.util.stream.Collectors.toSet());
        if (user.isPasswordChangeRequired() || !user.isEnabled() || !user.isAccountNonLocked() || !claims.username().equals(user.getUsername())
                || !claims.expiresAt().isAfter(Instant.now()) || !roles.equals(new java.util.HashSet<>(claims.roles()))
                || !sessions.hasActiveSession(claims.userId(), claims.sessionId(), LocalDateTime.now())) throw invalid();
        return user;
    }
    @Transactional(readOnly = true)
    public ValidatedAccount validate(String token) {
        User user = authenticatedUser(token);
        return new ValidatedAccount(user.getId(), user.getUsername(),
                user.getRole().stream().map(r -> r.getRoleType().name()).sorted().toList());
    }
    private InvalidCredentialsException invalid() { return new InvalidCredentialsException("Invalid access token"); }
    public record ValidatedAccount(long userId, String username, List<String> roles) {}
}
