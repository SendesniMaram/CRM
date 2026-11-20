package com.crm.hr.client;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;

class EmployeeProfileClientConfigurationTest {
    final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(EmployeeProfileClientConfiguration.class);
    @Test void absentConfigurationDoesNotActivateCalls() {
        runner.run(context -> { assertThat(context).hasNotFailed(); EmployeeProfileClientTest.failure(() -> context.getBean(EmployeeProfileClient.class).read("103"), 503); });
    }
    @Test void activationRequiresDedicatedSigner() {
        runner.withPropertyValues("app.security.employee-profile-client.enabled=true", "app.security.employee-profile-client.base-url=https://employee.test")
                .run(context -> assertThat(context).hasFailed());
    }
    @ParameterizedTest @ValueSource(strings = {"http://employee.test", "https://user:password@employee.test", "https://employee.test/path", "https://employee.test?target=x", "https://employee.test#x", "https://employee.test:0", "https://employee.test:65536"})
    void invalidOrNonHttpsOriginIsRejected(String uri) {
        assertThatIllegalArgumentException().isThrownBy(() -> new EmployeeProfileClientProperties(true, URI.create(uri), null, null, null));
    }
    @Test void zeroInfiniteAndInconsistentBudgetsAreRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new EmployeeProfileClientProperties(true, URI.create("https://employee.test"), Duration.ZERO, null, null));
        assertThatIllegalArgumentException().isThrownBy(() -> new EmployeeProfileClientProperties(true, URI.create("https://employee.test"), null, Duration.ofSeconds(11), null));
        assertThatIllegalArgumentException().isThrownBy(() -> new EmployeeProfileClientProperties(true, URI.create("https://employee.test"), null, null, Duration.ofMillis(100)));
    }
    @Test void permissiveTlsSwitchIsNotSupported() {
        runner.withPropertyValues("app.security.employee-profile-client.trust-all=true").run(context -> assertThat(context).hasFailed());
    }
}
