package com.crm.employee.security;

import com.crm.employee.config.SecurityConfig;
import com.crm.employee.controller.InternalEmployeeProfileController;
import com.crm.employee.repository.EmployeeRepository;
import com.crm.employee.service.EmployeeProfileSummaryService;
import com.crm.security.JwtSecurityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InternalEmployeeProfileController.class)
@Import({SecurityConfig.class, JwtSecurityAutoConfiguration.class, EmployeeProfileSummaryService.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class InternalEmployeeProfileDisabledTest extends TestIdentitySupport {
    @Autowired MockMvc mvc;
    @MockBean EmployeeRepository employees;
    @Test void unconfiguredInternalAuthenticationIs503WithoutAnyBusinessRead() throws Exception {
        mvc.perform(get("/api/internal/employees/103/profile-summary").secure(true)
                .requestAttr("jakarta.servlet.request.cipher_suite", "TLS_AES_128_GCM_SHA256"))
                .andExpect(status().isServiceUnavailable()).andExpect(header().string("Cache-Control", "no-store"));
        verifyNoInteractions(employees);
    }
}
