package com.crm.identity.security;

import com.crm.identity.dto.ApiError;
import com.crm.identity.exception.InvalidCredentialsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Dedicated to the internal security chain; never registered as a global servlet filter. */
public class AccessTokenValidationFilter extends OncePerRequestFilter {
    private final AccessTokenValidationService validation;
    private final ObjectMapper json;

    public AccessTokenValidationFilter(AccessTokenValidationService validation, ObjectMapper json) {
        this.validation = validation;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        // Authenticate only the bearer presented on this request, even if a context exists.
        SecurityContextHolder.clearContext();
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank()) {
            reject(request, response, 401);
            return;
        }
        AccessTokenValidationService.ValidatedAccount account;
        try {
            account = validation.validate(authorization.substring(7));
        } catch (InvalidCredentialsException ex) {
            reject(request, response, 401);
            return;
        } catch (RuntimeException ex) {
            // A DB/transaction or unexpected validation failure cannot grant authentication.
            reject(request, response, 503);
            return;
        }
        var authorities = account.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(account.username(), null, authorities));
        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), new ApiError(status,
                status == 401 ? "Unauthorized" : "Service Unavailable",
                status == 401 ? "Access token invalid" : "Access token validation unavailable",
                request.getRequestURI()));
    }
}
