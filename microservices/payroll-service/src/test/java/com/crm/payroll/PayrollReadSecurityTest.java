package com.crm.payroll;

import com.crm.payroll.controller.PayrollController;
import com.crm.payroll.dto.PayrollResponse;
import com.crm.payroll.service.IPayrollService;
import com.crm.payroll.config.SecurityConfig;
import com.crm.security.JwtSecurityAutoConfiguration;
import com.crm.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real SecurityFilterChain and JWT filter; only business services are replaced.
@WebMvcTest(controllers = {PayrollController.class})
@Import({SecurityConfig.class, JwtSecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = true, print = MockMvcPrint.NONE)
class PayrollReadSecurityTest extends com.crm.payroll.security.TestIdentitySupport {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean IPayrollService service0;

    @BeforeEach
    void responses() {
        PayrollResponse row0 = new PayrollResponse();
        row0.setId(1L);
        when(service0.getAllPayrolls()).thenReturn(List.of(row0));
        when(service0.getAllPayrollsPaged(anyInt(), anyInt(), any(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of(row0)));
        when(service0.getPayrollById(1L)).thenReturn(row0);
    }

    static Stream<Arguments> accessMatrix() {
        return Stream.of("/api/payrolls",
                "/api/payrolls?size=20&keyword=test&sortBy=id",
                "/api/payrolls/1")
                .flatMap(path -> Stream.of(
                        Arguments.of(path, "ANONYMOUS", 401),
                        Arguments.of(path, "EMPLOYEE", 403),
                        Arguments.of(path, "CLIENT", 403),
                        Arguments.of(path, "ADMIN", 200),
                        Arguments.of(path, "SUPER_ADMIN", 200)));
    }

    @ParameterizedTest(name = "{1} GET {0} returns {2}")
    @MethodSource("accessMatrix")
    void administrativeReadsRequireAnAdministrator(String path, String role, int expected) throws Exception {
        var request = get(path);
        if (!role.equals("ANONYMOUS")) {
            request.header("Authorization", "Bearer " + com.crm.payroll.security.TestIdentitySupport.token(
                    "security-test-user", "", true, List.of(role)));
        }
        var result = mvc.perform(request).andExpect(status().is(expected));
        if (expected != 200) {
            verifyNoInteractions(service0);
        } else if (path.contains("?")) {
            result.andExpect(jsonPath("$.content[0].id").value(1));
        } else if (path.endsWith("/details")) {
            result.andExpect(jsonPath("$.employee.id").value(1));
        } else if (path.endsWith("/1")) {
            result.andExpect(jsonPath("$.id").value(1));
        } else {
            result.andExpect(jsonPath("$[0].id").value(1));
        }
    }
}
