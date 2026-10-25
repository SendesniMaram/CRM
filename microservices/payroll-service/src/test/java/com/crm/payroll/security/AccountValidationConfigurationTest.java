package com.crm.payroll.security;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import com.crm.security.JwtSecurityAutoConfiguration;

class AccountValidationConfigurationTest {
    @Test void serviceRequiresExplicitEnvironmentOriginAndKeepsTimeouts() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("src/main/resources/application.yml"));
        var properties = yaml.getObject();
        assertThat(properties.getProperty("app.security.account-validation.base-url")).isEqualTo("${IDENTITY_VALIDATION_BASE_URL}");
        assertThat(properties.getProperty("app.security.account-validation.connect-timeout")).isEqualTo("500ms");
        assertThat(properties.getProperty("app.security.account-validation.read-timeout")).isEqualTo("1s");
        assertThat(properties.getProperty("app.security.account-validation.total-timeout")).isEqualTo("1500ms");
    }
    @Test void missingOriginFailsSecurityContextStartup() {
        runner().run(context -> assertThat(context).hasFailed());
    }
    @Test void invalidOriginFailsSecurityContextStartup() {
        runner().withPropertyValues("app.security.account-validation.base-url=http://user:password@identity")
                .run(context -> assertThat(context).hasFailed());
    }
    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JwtSecurityAutoConfiguration.class))
                .withPropertyValues("app.jwt.secret=synthetic-config-test-key-more-than-32-bytes");
    }
}
