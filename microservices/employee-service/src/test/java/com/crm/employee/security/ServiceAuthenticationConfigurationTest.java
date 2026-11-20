package com.crm.employee.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;

class ServiceAuthenticationConfigurationTest {
    @TempDir Path directory;
    final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(ServiceAuthenticationConfiguration.class);
    @Test void absentConfigurationCreatesAClosedVerifier() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ServiceJwtVerifier.class);
            assertThat(context.getBean(ServiceJwtVerifier.class).configured()).isFalse();
        });
    }
    @Test void explicitEnableRequiresTrustedKeys() {
        runner.withPropertyValues("app.security.hr-service-auth.enabled=true").run(context -> assertThat(context).hasFailed());
    }
    @Test void unknownPermissiveTransportSwitchIsNotAccepted() {
        runner.withPropertyValues("app.security.hr-service-auth.allow-http=true")
                .run(context -> assertThat(context).hasFailed());
    }
    @Test void missingPublicKeyFileFailsStartup() {
        runner.withPropertyValues("app.security.hr-service-auth.enabled=true",
                "app.security.hr-service-auth.trusted-keys.test-key=" + directory.resolve("missing.pem"))
                .run(context -> assertThat(context).hasFailed());
    }
    @Test void relativeKeyPathIsRefused() {
        runner.withPropertyValues("app.security.hr-service-auth.enabled=true", "app.security.hr-service-auth.trusted-keys.test-key=key.pem")
                .run(context -> assertThat(context).hasFailed());
    }
    @Test void malformedKeyFailsStartup() throws Exception {
        Path path = directory.resolve("invalid.pem"); Files.writeString(path, "not a key");
        runner.withPropertyValues("app.security.hr-service-auth.enabled=true", "app.security.hr-service-auth.trusted-keys.test-key=" + path)
                .run(context -> assertThat(context).hasFailed());
    }
    @Test void onlyAnExternalEphemeralPublicKeyIsLoaded() throws Exception {
        Path path = directory.resolve("public.pem");
        Files.writeString(path, "-----BEGIN PUBLIC KEY-----\n" + Base64.getEncoder().encodeToString(ServiceJwtTestTokens.HR.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----");
        runner.withPropertyValues("app.security.hr-service-auth.enabled=true", "app.security.hr-service-auth.trusted-keys.test-key=" + path)
                .run(context -> { assertThat(context).hasNotFailed(); assertThat(context.getBean(ServiceJwtVerifier.class).configured()).isTrue(); });
    }
}
