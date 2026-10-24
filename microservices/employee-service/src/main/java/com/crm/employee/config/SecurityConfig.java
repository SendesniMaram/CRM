package com.crm.employee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.crm.security.JwtAuthenticationFilter;
import com.crm.security.RestAuthenticationEntryPoint;
import com.crm.security.CustomAccessDeniedHandler;

@Configuration
@EnableWebSecurity
@Import(com.crm.employee.security.ServiceAuthenticationConfiguration.class)
public class SecurityConfig {

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter> userJwtServletRegistration(
            JwtAuthenticationFilter filter) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // Security chains own filter placement; never run the user filter on service requests.
        return registration;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain internalServiceSecurityFilterChain(HttpSecurity http,
            com.crm.employee.security.ServiceJwtVerifier verifier) throws Exception {
        return http.securityMatcher("/api/internal", "/api/internal/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/internal/employees/*/profile-summary")
                        .hasAuthority(com.crm.employee.security.ServiceJwtVerifier.AUTHORITY)
                        .anyRequest().denyAll())
                .addFilterBefore(new com.crm.employee.security.ServiceJwtAuthenticationFilter(verifier),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   CustomAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/test").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/employees/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/employees/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/employees/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/employees/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
