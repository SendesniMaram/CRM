package com.crm.security;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AccountValidationConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JwtSecurityAutoConfiguration.class))
            .withPropertyValues("app.jwt.secret=" + AccountValidationClientTest.SECRET);

    @Test void missingOriginFailsStartup() {
        runner.run(context -> assertThat(context).hasFailed());
    }
    @Test void validConfigurationCreatesMandatoryClientAndFilter() {
        runner.withPropertyValues("app.security.account-validation.base-url=http://identity-service:8082")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(AccountValidationClient.class)
                            .hasSingleBean(JwtAuthenticationFilter.class);
                    var properties = context.getBean(AccountValidationProperties.class);
                    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofMillis(500));
                    assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(properties.totalTimeout()).isEqualTo(Duration.ofMillis(1500));
                });
    }
    @ParameterizedTest @ValueSource(strings = {"ftp://identity-service", "http://user:password@identity-service", "http://identity-service/api", "http://identity-service?host=evil", "http://identity-service#evil", "http://identity-service:0", "http://identity-service:70000", "/relative"})
    void untrustedOrAmbiguousOriginsFailStartup(String origin) {
        runner.withPropertyValues("app.security.account-validation.base-url=" + origin)
                .run(context -> assertThat(context).hasFailed());
    }
    @ParameterizedTest @ValueSource(strings = {"0ms", "-1ms", "50001ms", "invalid"})
    void invalidTimeoutFailsStartup(String timeout) {
        runner.withPropertyValues("app.security.account-validation.base-url=http://identity-service:8082",
                "app.security.account-validation.connect-timeout=" + timeout)
                .run(context -> assertThat(context).hasFailed());
    }
    @Test void totalBudgetCannotBeSmallerThanSocketTimeouts() {
        assertThatThrownBy(() -> new AccountValidationProperties(URI.create("http://identity-service:8082"),
                Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(100)))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void unknownPropertyFailsInsteadOfSilentlyIgnoringTypo() {
        runner.withPropertyValues("app.security.account-validation.base-url=http://identity-service:8082",
                "app.security.account-validation.read-timout=10ms")
                .run(context -> assertThat(context).hasFailed());
    }
}
