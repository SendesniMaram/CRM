package com.crm.department;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "eureka.client.enabled=false"
        }
)
class DepartmentServiceSmokeTest extends com.crm.department.security.TestIdentitySupport {

    @Test
    void contextLoads() {
    }
}
