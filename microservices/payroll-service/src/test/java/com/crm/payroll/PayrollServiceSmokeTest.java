package com.crm.payroll;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "eureka.client.enabled=false"
        }
)
class PayrollServiceSmokeTest {

    @Test
    void contextLoads() {
    }
}

