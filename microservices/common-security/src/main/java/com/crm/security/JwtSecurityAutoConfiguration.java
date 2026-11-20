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
 * Requires {@code app.jwt.secret} and a trusted origin at
 * {@code app.security.account-validation.base-url}; no offline authentication mode.
 */
@AutoConfiguration
@org.springframework.boot.context.properties.EnableConfigurationProperties(AccountValidationProperties.class)
public class JwtSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtService jwtService(
            @Value("${app.jwt.secret}") String secret) {
        return new JwtService(secret);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(AccountValidationClient validationClient) {
        return new JwtAuthenticationFilter(validationClient);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public AccountValidationClient accountValidationClient(JwtService jwtService, AccountValidationProperties properties) {
        return new AccountValidationClient(jwtService, properties);
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

