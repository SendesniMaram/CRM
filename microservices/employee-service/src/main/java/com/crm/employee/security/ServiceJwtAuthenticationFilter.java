package com.crm.employee.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Constructed inside the internal chain only; never a globally registered servlet filter. */
public final class ServiceJwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Pattern RESOURCE = Pattern.compile("/api/internal/employees/([^/]+)/profile-summary");
    private final ServiceJwtVerifier verifier;
    public ServiceJwtAuthenticationFilter(ServiceJwtVerifier verifier) { this.verifier = verifier; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        response.setHeader("Cache-Control", "no-store");
        // Require direct container TLS evidence. X-Forwarded-Proto and client headers are not proof of TLS.
        if (!request.isSecure() || !(request.getAttribute("jakarta.servlet.request.cipher_suite") instanceof String cipher)
                || cipher.isBlank()) { reject(response, 403); return; }
        if (!verifier.configured()) { reject(response, 503); return; }
        var values = Collections.list(request.getHeaders("Authorization"));
        if (values.size() != 1 || !values.get(0).startsWith("Bearer ")) { reject(response, 401); return; }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        var resource = RESOURCE.matcher(path);
        if (!resource.matches()) { reject(response, 403); return; }
        try {
            var principal = verifier.verify(values.get(0).substring(7), request.getMethod(), resource.group(1));
            if (request.getQueryString() != null) { reject(response, 400); return; }
            var authentication = new UsernamePasswordAuthenticationToken(principal, null,
                    List.of(new SimpleGrantedAuthority(ServiceJwtVerifier.AUTHORITY)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BadCredentialsException ex) { reject(response, 401); return;
        } catch (AccessDeniedException ex) { reject(response, 403); return;
        } catch (IllegalArgumentException ex) { reject(response, 400); return; }
        chain.doFilter(request, response);
    }
    private static void reject(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status); response.setContentType("application/json");
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"Internal service access refused\"}");
    }
}
