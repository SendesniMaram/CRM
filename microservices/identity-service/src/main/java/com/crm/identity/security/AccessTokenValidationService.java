package com.crm.identity.security;

import com.crm.identity.enums.RoleType;
import com.crm.identity.exception.InvalidCredentialsException;
import com.crm.identity.repository.UserRepository;
import java.util.List;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads current account state for each access check; no network calls or positive cache. */
@Service
public class AccessTokenValidationService {
    private final JwtService jwt;
    private final UserRepository users;

    public AccessTokenValidationService(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public ValidatedAccount validate(String token) {
        var claims = jwt.parseAccessToken(token);
        var user = users.findById(claims.userId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid access token"));
        var currentRoles = user.getRole().stream().map(role -> role.getRoleType())
                .map(RoleType::name).toList();
        if (!user.isEnabled() || !user.isAccountNonLocked()
                || !claims.username().equals(user.getUsername())
                || !claims.expiresAt().isAfter(Instant.now())
                || !currentRoles.containsAll(claims.roles())) {
            throw new InvalidCredentialsException("Invalid access token");
        }
        // Keep signed roles only: a new database role cannot elevate this token.
        return new ValidatedAccount(claims.userId(), claims.username(), claims.roles());
    }

    public record ValidatedAccount(long userId, String username, List<String> roles) {}
}
