package com.crm.identity.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
public class AuthenticationServiceImpl implements IAuthenticationService {

    private static final long REFRESH_TOKEN_DURATION_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public AuthenticationServiceImpl(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     PasswordEncoder passwordEncoder,
                                     RefreshTokenRepository refreshTokenRepository,
                                     JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
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
        refreshTokenRepository.deleteByUser(user);

        // Générer un nouveau refresh token persisté
        RefreshToken refreshToken = createRefreshToken(user);

        // Construire la réponse
        LoginResponse response = new LoginResponse();
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
        response.setExpiration(86400000L); // 24 heures, aligné sur le backend
        response.setRefreshToken(refreshToken.getToken());
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

        // Résoudre le rôle demandé (par défaut CLIENT si non spécifié)
        Role role = resolveRole(request.getRoleType());

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
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new IllegalArgumentException("refreshToken is required");
        }

        // Rechercher le refresh token en base
        Optional<RefreshToken> optionalToken = refreshTokenRepository.findByToken(request.getRefreshToken());
        RefreshToken storedToken = optionalToken.orElseThrow(
                () -> new InvalidCredentialsException("invalid or expired refresh token")
        );

        // Vérifier que le token n'est pas révoqué
        if (storedToken.isRevoked()) {
            throw new InvalidCredentialsException("refresh token has been revoked");
        }

        // Vérifier que le token n'a pas expiré
        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new InvalidCredentialsException("refresh token has expired");
        }

        User user = storedToken.getUser();

        if (user == null) {
            throw new InvalidCredentialsException("user not found");
        }

        // Révoquer l'ancien refresh token (rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Générer un nouveau refresh token
        RefreshToken newRefreshToken = createRefreshToken(user);

        // Générer un nouveau JWT access token
        String newAccessToken = jwtService.generateToken(
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.getRole().stream()
                        .map(r -> r.getRoleType().name())
                        .toList()
        );

        // Construire la réponse
        RefreshTokenResponse response = new RefreshTokenResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken.getToken());
        response.setType("Bearer");
        response.setRoles(user.getRole().stream()
                .map(r -> r.getRoleType().name())
                .toList());
        response.setExpiration(86400000L);

        return response;
    }


    /**
     * Crée et persist un nouveau refresh token pour l'utilisateur donné.
     */
    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(REFRESH_TOKEN_DURATION_MS / 1000));
        refreshToken.setRevoked(false);
        refreshToken.setUser(user);
        return refreshTokenRepository.save(refreshToken);
    }


    /**
     * Résout le rôle à partir du nom donné en entrée.
     * Si roleType est null ou vide, retourne le rôle CLIENT par défaut.
     *
     * @param roleType le nom du rôle (ex: "admin", "employee", "client")
     * @return l'entité Role correspondante
     * @throws IllegalStateException si le rôle n'est pas trouvé
     */
    private Role resolveRole(String roleType) {
        if (roleType == null || roleType.isBlank()) {
            // Par défaut, attribuer le rôle CLIENT
            return roleRepository.findByRoleType(RoleType.CLIENT)
                    .orElseThrow(() -> new IllegalStateException("Default role CLIENT not found"));
        }

        try {
            RoleType parsedRoleType = RoleType.valueOf(roleType.toUpperCase());
            return roleRepository.findByRoleType(parsedRoleType)
                    .orElseThrow(() -> new IllegalStateException("Role not found: " + roleType));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid role type: " + roleType);
        }
    }

}

