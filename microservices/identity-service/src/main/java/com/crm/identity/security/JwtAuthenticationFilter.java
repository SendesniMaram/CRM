package com.crm.identity.security;

import com.crm.identity.exception.InvalidCredentialsException;
import com.crm.identity.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Uses the same account/session validation as the internal endpoint. */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final AccessTokenValidationService validation;
    private final ObjectMapper json;
    public JwtAuthenticationFilter(AccessTokenValidationService validation, ObjectMapper json) {
        this.validation = validation; this.json = json;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/api/auth/") || path.equals("/api/test") || path.equals("/actuator/health");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ") || header.indexOf('\r') >= 0 || header.indexOf('\n') >= 0) {
            reject(request,response,401); return;
        }
        try {
            var user = validation.authenticatedUser(header.substring(7));
            var authorities = user.getRole().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.getRoleType().name())).toList();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities));
        } catch (InvalidCredentialsException ex) { reject(request,response,401); return;
        } catch (RuntimeException ex) { reject(request,response,503); return; }
        chain.doFilter(request,response);
    }
    private void reject(HttpServletRequest request, HttpServletResponse response, int status) throws IOException {
        response.setHeader("Cache-Control", "no-store"); response.setStatus(status); response.setContentType("application/json");
        json.writeValue(response.getOutputStream(), new ApiError(status, status == 401 ? "Unauthorized" : "Service Unavailable",
                status == 401 ? "Access token invalid" : "Access token validation unavailable", request.getRequestURI()));
    }
}
