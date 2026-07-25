package com.crm.employee.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Skeleton implementation of UserDetailsService for employee-service.
 * Currently returns a placeholder user — will be connected to a real user store later.
 */
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Placeholder: returns a default user with ROLE_EMPLOYEE
        return User.withUsername(username)
                .password("{noop}placeholder")
                .roles("EMPLOYEE")
                .build();
    }
}
