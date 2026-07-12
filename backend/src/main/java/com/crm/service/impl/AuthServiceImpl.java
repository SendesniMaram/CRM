package com.crm.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.crm.dto.LoginRequest;
import com.crm.dto.LoginResponse;
import com.crm.dto.RegisterRequest;
import com.crm.entity.Role;
import com.crm.entity.User;
import com.crm.enums.RoleType;
import com.crm.exception.BadRequestException;
import com.crm.repository.RoleRepository;
import com.crm.repository.UserRepository;
import com.crm.security.jwt.JwtService;
import com.crm.security.service.UserPrincipal;
import com.crm.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserRepository userRepository,
            RoleRepository roleRepository) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        Role role = resolveRole(request.getRoleType());

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setRole(role);

        userRepository.save(user);

        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        return buildLoginResponse(user);
    }

    private Role resolveRole(String roleType) {
        if (roleType == null || roleType.isBlank()) {
            return roleRepository.findByRoleType(RoleType.EMPLOYEE)
                    .orElseThrow(() -> new BadRequestException("Default role EMPLOYEE not found"));
        }

        try {
            RoleType parsedRoleType = RoleType.valueOf(roleType.toUpperCase());
            return roleRepository.findByRoleType(parsedRoleType)
                    .orElseThrow(() -> new BadRequestException("Role not found: " + roleType));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role type: " + roleType);
        }
    }

    private LoginResponse buildLoginResponse(User user) {
        UserPrincipal principal = UserPrincipal.create(user);
        String token = jwtService.generateToken(principal);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole() != null ? user.getRole().getRoleType().name() : null);
        response.setExpiration(86400000L);
        return response;
    }
}
