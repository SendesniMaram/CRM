package com.crm.employee.security;

import com.crm.employee.config.SecurityConfig;
import com.crm.security.JwtSecurityAutoConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(controllers = InternalServiceSecurityTest.Probe.class)
@Import({SecurityConfig.class, JwtSecurityAutoConfiguration.class, InternalServiceSecurityTest.Probe.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class InternalServiceDisabledTest extends TestIdentitySupport {
    @Autowired MockMvc mvc;
    @Test void missingConfigurationNeverFallsThroughToUserAuthentication() throws Exception {
        mvc.perform(get("/api/internal/employees/103/profile-summary").secure(true)
                .requestAttr("jakarta.servlet.request.cipher_suite", "TLS_AES_128_GCM_SHA256")
                .header("Authorization", "Bearer " + token("synthetic-admin", "", true, List.of("ADMIN"))))
                .andExpect(status().isServiceUnavailable());
        assertThat(validations).hasValue(0);
    }
}
