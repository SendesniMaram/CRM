package com.crm.identity.service.impl;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.crm.identity.dto.LoginRequest;
import com.crm.identity.dto.RegisterRequest;
import com.crm.identity.entity.User;
import com.crm.identity.repository.RoleRepository;
import com.crm.identity.repository.UserRepository;
import com.crm.identity.service.IAuthenticationService;

/**
 * Authentication service implementation.
 */
@Service
public class AuthenticationServiceImpl implements IAuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public com.crm.identity.dto.LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        // Recherche par username (prioritaire). Si vide, on tente email.
        com.crm.identity.entity.User user;
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            var found = userRepository.findByUsername(request.getUsername());
            user = found.orElse(null);
        } else {
            var found = userRepository.findByEmail(request.getEmail());
            user = found.orElse(null);
        }

        if (user == null) {
            throw new com.crm.identity.exception.InvalidCredentialsException("invalid credentials");
        }

        boolean passwordValid = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!passwordValid) {
            throw new com.crm.identity.exception.InvalidCredentialsException("invalid credentials");
        }

        com.crm.identity.dto.LoginResponse response = new com.crm.identity.dto.LoginResponse();
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setEnabled(user.isEnabled());
        return response;
    }


    @Override
    public void register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        Optional<User> byUsername = userRepository.findByUsername(request.getUsername());
        if (byUsername.isPresent()) {
            throw new IllegalStateException("username already exists");
        }

        Optional<User> byEmail = userRepository.findByEmail(request.getEmail());
        if (byEmail.isPresent()) {
            throw new IllegalStateException("email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);


        userRepository.save(user);
    }

}

