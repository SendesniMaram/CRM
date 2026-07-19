package com.crm.identity.security;

import java.util.Optional;

import com.crm.identity.entity.User;
import com.crm.identity.exception.InvalidCredentialsException;
import com.crm.identity.repository.UserRepository;

/**
 * Service to prepare Spring Security integration.
 * <p>
 * For now it only loads the {@link User} entity.
 */
public class CustomUserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User loadUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new InvalidCredentialsException("username is empty");
        }

        Optional<User> byUsername = userRepository.findByUsername(username);
        if (byUsername.isPresent()) {
            return byUsername.get();
        }

        Optional<User> byEmail = userRepository.findByEmail(username);
        if (byEmail.isPresent()) {
            return byEmail.get();
        }

        throw new InvalidCredentialsException("User not found");
    }
}

