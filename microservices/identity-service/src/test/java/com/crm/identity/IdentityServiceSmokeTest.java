package com.crm.identity;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "eureka.client.enabled=false",
                "spring.config.name=application-test",
                "spring.profiles.active=test"
        }
)
class IdentityServiceSmokeTest {

    // Avoid contacting Eureka during tests; we only validate Spring wiring.

    @Test

    void contextLoads() {
        // If the Spring context starts successfully, wiring is valid.
    }
}

