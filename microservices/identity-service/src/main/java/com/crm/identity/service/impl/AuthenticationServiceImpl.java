package com.crm.identity.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crm.identity.dto.LoginRequest;
import com.crm.identity.dto.LoginResponse;
import com.crm.identity.dto.RefreshTokenRequest;
import com.crm.identity.dto.RefreshTokenResponse;
import com.crm.identity.dto.RegisterRequest;
import com.crm.identity.entity.RefreshToken;
import com.crm.identity.entity.Role;
import com.crm.identity.entity.User;
import com.crm.identity.enums.RoleType;
import com.crm.identity.exception.InvalidCredentialsException;
import com.crm.identity.repository.RefreshTokenRepository;
import com.crm.identity.repository.RoleRepository;
import com.crm.identity.repository.UserRepository;
import com.crm.identity.security.JwtService;
import com.crm.identity.service.IAuthenticationService;

/**
 * Authentication service implementation.
 */
@Service
@Transactional
public class AuthenticationServiceImpl implements IAuthenticationService {

    private static final long REFRESH_TOKEN_DURATION_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final jakarta.persistence.EntityManager entityManager;

    public AuthenticationServiceImpl(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     PasswordEncoder passwordEncoder,
                                     RefreshTokenRepository refreshTokenRepository,
                                     JwtService jwtService, jakarta.persistence.EntityManager entityManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.entityManager = entityManager;
    }


    @Override
    public LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        // Recherche par email (prioritaire pour le login)
        User user;
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            var found = userRepository.findByEmail(request.getEmail());
            user = found.orElse(null);
        } else if (request.getUsername() != null && !request.getUsername().isBlank()) {
            var found = userRepository.findByUsername(request.getUsername());
            user = found.orElse(null);
        } else {
            user = null;
        }

        if (user == null) {
            throw new InvalidCredentialsException("invalid credentials");
        }

        user = userRepository.lockById(user.getId()).orElseThrow(() -> new InvalidCredentialsException("invalid credentials"));

        entityManager.refresh(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        // Vérifier que le compte est activé
        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("account is disabled");
        }

        // Vérifier que le compte n'est pas verrouillé
        if (!user.isAccountNonLocked()) {
            throw new InvalidCredentialsException("account is locked");
        }

        // Vérifier le mot de passe
        boolean passwordValid = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!passwordValid) {
            throw new InvalidCredentialsException("invalid credentials");
        }

        // Révoquer les anciens refresh tokens de l'utilisateur
        refreshTokenRepository.revokeAllByUser(user);

        // Générer un nouveau refresh token persisté
        RefreshToken refreshToken = createRefreshToken(user, UUID.randomUUID().toString());

        // Construire la réponse
        LoginResponse response = new LoginResponse();
        response.setAuthenticatedUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setEnabled(user.isEnabled());
        response.setRole(user.getRole().stream()
                .findFirst()
                .map(r -> r.getRoleType().name())
                .orElse(null));
        response.setRoles(user.getRole().stream()
                .map(r -> r.getRoleType().name())
                .toList());
        response.setExpiration(JwtService.ACCESS_TOKEN_DURATION_MS);
        response.setRefreshToken(refreshToken.getToken());
        response.setToken(issueAccessToken(user, refreshToken.getSessionId()));
        response.setType("Bearer");
        return response;
    }


    @Override
    public void register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        // Vérifier que le username est unique
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("username already exists");
        }

        // Vérifier que l'email est unique
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("email already exists");
        }

        // Toute inscription publique reçoit uniquement le rôle CLIENT.
        Role role = resolveClientRole();

        // Construire l'utilisateur
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.getRole().add(role);

        // Persister l'utilisateur dans identity_db
        userRepository.save(user);
    }


    @Override
    // Revocations on rejected refreshes must commit despite the 401 exception.
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new IllegalArgumentException("refreshToken is required");
        }

        // Rechercher le refresh token en base
        User user = lockSessionOwner(request.getRefreshToken());
        Optional<RefreshToken> optionalToken = refreshTokenRepository.findByTokenForUpdate(request.getRefreshToken());
        RefreshToken storedToken = optionalToken.orElseThrow(
                () -> new InvalidCredentialsException("invalid or expired refresh token")
        );

        entityManager.refresh(storedToken, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        // Vérifier que le token n'est pas révoqué
        if (storedToken.isRevoked()) {
            throw new InvalidCredentialsException("refresh token has been revoked");
        }

        // Vérifier que le token n'a pas expiré
        if (storedToken.getExpiryDate() == null || !storedToken.getExpiryDate().isAfter(LocalDateTime.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new InvalidCredentialsException("refresh token has expired");
        }

        if (storedToken.getUser() == null || !user.getId().equals(storedToken.getUser().getId())
                || !JwtService.canonicalSession(storedToken.getSessionId())) throw new InvalidCredentialsException("Invalid session");

        // Revoke every refresh session for an inactive account, not just this token.
        // This also prevents these sessions from reviving if the account is re-enabled.
        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            refreshTokenRepository.revokeAllByUser(user);
            throw new InvalidCredentialsException("account is unavailable");
        }

        // Révoquer l'ancien refresh token (rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Générer un nouveau refresh token
        RefreshToken newRefreshToken = createRefreshToken(user, storedToken.getSessionId());

        // Générer un nouveau JWT access token
        String newAccessToken = issueAccessToken(user, newRefreshToken.getSessionId());

        // Construire la réponse
        RefreshTokenResponse response = new RefreshTokenResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken.getToken());
        response.setType("Bearer");
        response.setRoles(user.getRole().stream()
                .map(r -> r.getRoleType().name())
                .toList());
        response.setExpiration(JwtService.ACCESS_TOKEN_DURATION_MS);

        return response;
    }


    /**
     * Crée et persist un nouveau refresh token pour l'utilisateur donné.
     */
    private RefreshToken createRefreshToken(User user, String sessionId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setSessionId(sessionId);
        refreshToken.setToken(UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(REFRESH_TOKEN_DURATION_MS / 1000));
        refreshToken.setRevoked(false);
        refreshToken.setUser(user);
        return refreshTokenRepository.save(refreshToken);
    }


    private String issueAccessToken(User user, String sessionId) {
        return jwtService.generateSessionToken(user.getId(), user.getUsername(), user.getEmail(), user.isEnabled(),
                user.getRole().stream().map(r -> r.getRoleType().name()).toList(), sessionId);
    }

    // All session writes take the User lock before any refresh row lock.
    private User lockSessionOwner(String token) {
        var stored = refreshTokenRepository.findByToken(token).orElseThrow(() -> new InvalidCredentialsException("Invalid session"));
        if (stored.getUser() == null) throw new InvalidCredentialsException("Invalid session");
        User user = userRepository.lockById(stored.getUser().getId()).orElseThrow(() -> new InvalidCredentialsException("Invalid session"));
        entityManager.refresh(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return user;
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank())
            throw new IllegalArgumentException("refreshToken is required");
        // Idempotent and non-enumerating. A rotated token can revoke its own session family only.
        var existing = refreshTokenRepository.findByToken(request.getRefreshToken());
        if (existing.isEmpty() || existing.get().getUser() == null) return;
        var owner = userRepository.lockById(existing.get().getUser().getId());
        if (owner.isEmpty()) return;
        entityManager.refresh(owner.get(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        var stored = refreshTokenRepository.findByTokenForUpdate(request.getRefreshToken());
        stored.ifPresent(row -> entityManager.refresh(row, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE));
        if (stored.isPresent() && JwtService.canonicalSession(stored.get().getSessionId()))
            refreshTokenRepository.revokeSession(owner.get(), stored.get().getSessionId());
    }

    private Role resolveClientRole() {
        return roleRepository.findByRoleType(RoleType.CLIENT)
                .orElseThrow(() -> new IllegalStateException("Default role CLIENT not found"));
    }

}

