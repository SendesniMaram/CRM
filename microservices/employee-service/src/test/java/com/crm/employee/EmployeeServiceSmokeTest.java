package com.crm.employee;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "eureka.client.enabled=false"
        }
)
class EmployeeServiceSmokeTest {

    @Test
    void contextLoads() {
        // If the Spring context starts successfully, wiring is valid.
    }
}
