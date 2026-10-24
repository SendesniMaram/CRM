package com.crm.hr.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;

class ServiceTokenConfigurationTest {
    @TempDir Path directory;
    final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(ServiceTokenConfiguration.class);
    @Test void absentConfigurationNeverCreatesASigner() {
        runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(EmployeeServiceTokenSigner.class));
    }
    @Test void enablingWithoutMandatoryKeyConfigurationFailsClosed() {
        runner.withPropertyValues("app.security.employee-service-token.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }
    @ParameterizedTest @ValueSource(strings = {"0s", "31s", "1500ms"})
    void rejectsInvalidLifetime(String value) {
        runner.withPropertyValues("app.security.employee-service-token.enabled=true",
                "app.security.employee-service-token.private-key=" + directory.resolve("missing.pem"),
                "app.security.employee-service-token.key-id=test", "app.security.employee-service-token.lifetime=" + value)
                .run(context -> assertThat(context).hasFailed());
    }
    @Test void missingKeyFileFailsRatherThanGeneratingAKey() {
        runner.withPropertyValues("app.security.employee-service-token.enabled=true",
                "app.security.employee-service-token.private-key=" + directory.resolve("missing.pem"), "app.security.employee-service-token.key-id=test")
                .run(context -> assertThat(context).hasFailed());
    }
    @Test void externalEphemeralPkcs8KeyCanBeLoaded() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        Path key = directory.resolve("ephemeral.pem");
        Files.writeString(key, "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----");
        runner.withPropertyValues("app.security.employee-service-token.enabled=true",
                "app.security.employee-service-token.private-key=" + key, "app.security.employee-service-token.key-id=test")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(EmployeeServiceTokenSigner.class));
    }
    @Test void relativeClasspathOrRepositoryDefaultIsNotAllowed() {
        runner.withPropertyValues("app.security.employee-service-token.enabled=true",
                "app.security.employee-service-token.private-key=classpath:private.pem", "app.security.employee-service-token.key-id=test")
                .run(context -> assertThat(context).hasFailed());
    }
}
