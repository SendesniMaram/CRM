package com.crm.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.security.core.userdetails.User;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withInitializer(application -> {
                application.getEnvironment().getPropertySources().remove("systemProperties");
                application.getEnvironment().getPropertySources().remove("systemEnvironment");
            })
            .withUserConfiguration(JwtOnlyConfiguration.class);

    @Test
    void missingSecretPreventsStartup() {
        context.run(application -> assertThat(application).hasFailed());
    }

    @Test
    void emptySecretPreventsStartup() {
        context.withPropertyValues("jwt.secret=")
                .run(application -> assertThat(application).hasFailed());
    }

    @Test
    void shortSecretPreventsStartup() {
        context.withPropertyValues("jwt.secret=test-only-short")
                .run(application -> assertThat(application).hasFailed());
    }

    @Test
    void explicitTestSecretAllowsSigningAndVerification() {
        context.withPropertyValues("jwt.secret=test-only-explicit-key-not-for-production-2026")
                .run(application -> {
                    assertThat(application).hasNotFailed();
                    JwtService jwt = application.getBean(JwtService.class);
                    var user = User.withUsername("configuration-test").password("unused").roles("USER").build();
                    assertThat(jwt.isTokenValid(jwt.generateToken(user), user)).isTrue();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(JwtService.class)
    static class JwtOnlyConfiguration {
        @Bean
        static PropertySourcesPlaceholderConfigurer placeholders() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }
}
