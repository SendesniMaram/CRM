package com.crm.hr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class SecurityConfig {

    @Bean
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
                        .requestMatchers(HttpMethod.GET, "/api/employee-profile/me").hasRole("EMPLOYEE")
                        .requestMatchers("/api/employee-profile", "/api/employee-profile/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/api/employee-associations/me").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.POST, "/api/employee-associations").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/employee-associations/users/*").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers("/api/employee-associations", "/api/employee-associations/**").denyAll()
                        .requestMatchers("/api/leaves/associations/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/leaves/calendar").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/evaluations/mine", "/api/evaluations/mine/**").hasRole("EMPLOYEE")
                        .requestMatchers("/api/employees/*/documents", "/api/employees/*/career-events", "/api/employees/*/leave-quotas", "/api/documents/**", "/api/career-events/**", "/api/leave-quotas/**")
                                .hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/leaves/mine").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.PUT, "/api/leaves/mine/**").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.DELETE, "/api/leaves/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "EMPLOYEE")
                        .requestMatchers(HttpMethod.GET,
                                "/api/hr-records/**", "/api/attendances/**", "/api/trainings/**",
                                "/api/contracts/**", "/api/evaluations/**", "/api/job-offers/**", "/api/applications/**")
                                .hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/leaves/**")
                                .hasAnyRole("SUPER_ADMIN", "ADMIN", "EMPLOYEE")
                        .requestMatchers(HttpMethod.POST,
                                "/api/hr-records/**", "/api/leaves/**", "/api/attendances/**", "/api/trainings/**",
                                "/api/contracts/**", "/api/evaluations/**", "/api/job-offers/**", "/api/applications/**")
                                .hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/hr-records/**", "/api/leaves/**", "/api/attendances/**", "/api/trainings/**",
                                "/api/contracts/**", "/api/evaluations/**", "/api/job-offers/**", "/api/applications/**")
                                .hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/leaves/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/hr-records/**", "/api/leaves/**", "/api/attendances/**", "/api/trainings/**",
                                "/api/contracts/**", "/api/evaluations/**", "/api/job-offers/**", "/api/applications/**")
                                .hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

