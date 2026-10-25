package com.crm.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Stateless JWT authentication filter shared by all microservices.
 * <p>
 * Requires strict local claims and a fresh Identity confirmation before setting
 * the signed principal and authorities. No business chain runs on validation failure.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AccountValidationClient validationClient;

    public JwtAuthenticationFilter(AccountValidationClient validationClient) {
        this.validationClient = java.util.Objects.requireNonNull(validationClient);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))
                && (path.equals("/actuator/health") || path.equals("/api/test")
                    || path.equals("/v3/api-docs") || path.startsWith("/v3/api-docs/")
                    || path.startsWith("/swagger-ui/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        SecurityContextHolder.clearContext();
        AccountValidationResult result = validationClient.validate(request.getHeader("Authorization"));
        if (result.status() != AccountValidationResult.Status.VALID) {
            response.setHeader("Cache-Control", "no-store");
            if (result.status() == AccountValidationResult.Status.INVALID) {
                new RestAuthenticationEntryPoint().commence(request, response,
                        new org.springframework.security.authentication.BadCredentialsException("Invalid access token"));
            } else {
                response.setStatus(503);
                response.setContentType("application/json");
                new com.fasterxml.jackson.databind.ObjectMapper().writeValue(response.getOutputStream(),
                        new ApiError(503, "Service Unavailable", "Account validation unavailable", request.getRequestURI()));
            }
            return;
        }
        List<SimpleGrantedAuthority> authorities = result.claims().roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        var principal = new AuthenticatedUserPrincipal(result.claims().userId(), result.claims().username(), authorities);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}

