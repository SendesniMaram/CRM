package com.crm.gateway;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

class ComposeSecurityConfigurationTest {
    private Properties yaml(String path) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(new FileSystemResource(path));
        return factory.getObject();
    }
    @ParameterizedTest
    @ValueSource(strings = {"employee", "department", "customer", "hr", "payroll", "fees", "invoice"})
    void everyConsumerUsesFixedInternalOriginAndExistingNetwork(String service) {
        var compose = yaml("../../docker-compose.yml");
        String prefix = "services." + service + "-service.";
        assertThat(compose.getProperty(prefix + "environment.IDENTITY_VALIDATION_BASE_URL"))
                .isEqualTo("http://identity-service:8082");
        assertThat(compose.getProperty(prefix + "networks[0]")).isEqualTo("crm-network");
        assertThat(compose.getProperty(prefix + "depends_on.identity-service.condition")).isEqualTo("service_started");
        var application = yaml("../" + service + "-service/src/main/resources/application.yml");
        assertThat(application.getProperty("app.security.account-validation.base-url"))
                .isEqualTo("${IDENTITY_VALIDATION_BASE_URL}");
        assertThat(application.getProperty("app.security.account-validation.connect-timeout")).isEqualTo("500ms");
        assertThat(application.getProperty("app.security.account-validation.read-timeout")).isEqualTo("1s");
        assertThat(application.getProperty("app.security.account-validation.total-timeout")).isEqualTo("1500ms");
    }
    @Test void identityNamePortAndReadinessAssumptionsAreExplicit() {
        var compose = yaml("../../docker-compose.yml");
        assertThat(compose.getProperty("services.identity-service.networks[0]")).isEqualTo("crm-network");
        assertThat(yaml("../identity-service/src/main/resources/application.yml").getProperty("server.port")).isEqualTo("8082");
        assertThat(compose.getProperty("services.mysql.healthcheck.test[0]")).isEqualTo("CMD");
        assertThat(compose.getProperty("services.identity-service.depends_on.mysql.condition")).isEqualTo("service_healthy");
        assertThat(compose.keySet()).noneMatch(key -> key.toString().startsWith("services.identity-service.healthcheck."));
    }
}
