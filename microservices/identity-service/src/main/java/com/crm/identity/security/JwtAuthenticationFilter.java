package com.crm.identity.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.crm.identity.entity.Role;
import com.crm.identity.entity.User;
import com.crm.identity.exception.InvalidCredentialsException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Stateless JWT authentication filter for identity-service.
 * <p>
 * Validates the JWT signature and populates the SecurityContext with the
 * username loaded from the database and the authorities built from the user's
 * roles stored in the {@code roles} table (the User entity does not implement
 * {@code UserDetails}).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService customUserDetailsService) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = authorizationHeader.substring("Bearer ".length());

            String username = jwtService.extractUsername(token);
            boolean valid = username != null && jwtService.isTokenValid(token, username);

            if (valid) {
                User userEntity;
                try {
                    userEntity = customUserDetailsService.loadUserByUsername(username);
                } catch (InvalidCredentialsException ex) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                // JWT claims are an issuance snapshot; Identity can check current state.
                if (!userEntity.isEnabled() || !userEntity.isAccountNonLocked()) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                List<SimpleGrantedAuthority> authorities = userEntity.getRole().stream()
                        .map(Role::getRoleType)
                        .map(rt -> "ROLE_" + rt.name())
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userEntity, null, authorities);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}

