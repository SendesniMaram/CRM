package com.crm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Auto-configuration exposing the shared JWT security beans.
 * <p>
 * Any microservice depending on {@code common-security} automatically gets:
 * <ul>
 *   <li>{@link JwtService}</li>
 *   <li>{@link JwtAuthenticationFilter}</li>
 *   <li>{@link RestAuthenticationEntryPoint}</li>
 *   <li>{@link CustomAccessDeniedHandler}</li>
 *   <li>{@link PasswordEncoder}</li>
 * </ul>
 * The JWT secret can be overridden with the property {@code app.jwt.secret}.
 */
@AutoConfiguration
public class JwtSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtService jwtService(
            @Value("${app.jwt.secret:${APP_JWT_SECRET:}}") String secret) {
        return new JwtService(secret);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    @ConditionalOnMissingBean
    public RestAuthenticationEntryPoint restAuthenticationEntryPoint() {
        return new RestAuthenticationEntryPoint();
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomAccessDeniedHandler customAccessDeniedHandler() {
        return new CustomAccessDeniedHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

