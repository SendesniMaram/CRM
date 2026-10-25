package com.crm.fees;

import com.crm.fees.controller.FeeController;
import com.crm.fees.dto.FeeResponse;
import com.crm.fees.service.IFeeService;
import com.crm.fees.config.SecurityConfig;
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
@WebMvcTest(controllers = {FeeController.class})
@Import({SecurityConfig.class, JwtSecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = true, print = MockMvcPrint.NONE)
class FeeReadSecurityTest extends com.crm.fees.security.TestIdentitySupport {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean IFeeService service0;

    @BeforeEach
    void responses() {
        FeeResponse row0 = new FeeResponse();
        row0.setId(1L);
        when(service0.getAllFees()).thenReturn(List.of(row0));
        when(service0.getAllFeesPaged(anyInt(), anyInt(), any(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of(row0)));
        when(service0.getFeeById(1L)).thenReturn(row0);
    }


    static Stream<Arguments> writeAccessMatrix() {
        return Stream.of("POST", "PUT", "DELETE").flatMap(method ->
                Stream.of("ANONYMOUS", "EMPLOYEE", "CLIENT", "ADMIN", "SUPER_ADMIN")
                        .map(role -> Arguments.of(method, role)));
    }

    @ParameterizedTest(name = "{1} {0} requires administrative authorization")
    @MethodSource("writeAccessMatrix")
    void administrativeWritesRequireAnAdministrator(String method, String role) throws Exception {
        var response = new FeeResponse();
        response.setId(1L);
        lenient().when(service0.createFee(any())).thenReturn(response);
        lenient().when(service0.updateFee(eq(1L), any())).thenReturn(response);
        var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                org.springframework.http.HttpMethod.valueOf(method),
                "/api/fees" + (method.equals("POST") ? "" : "/1"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":\"1\",\"serviceName\":\"Security fee\",\"hourlyRate\":10,\"workedHours\":2}");
        if (!role.equals("ANONYMOUS")) {
            request.header("Authorization", "Bearer " + com.crm.fees.security.TestIdentitySupport.token(
                    "security-test-user", "", true, List.of(role)));
        }
        boolean admin = role.equals("ADMIN") || role.equals("SUPER_ADMIN");
        int expected = role.equals("ANONYMOUS") ? 401 : !admin ? 403
                : method.equals("POST") ? 201 : method.equals("DELETE") ? 204 : 200;
        mvc.perform(request).andExpect(status().is(expected));
        if (!admin) verifyNoInteractions(service0);
        else if (method.equals("POST")) verify(service0).createFee(any());
        else if (method.equals("PUT")) verify(service0).updateFee(eq(1L), any());
        else verify(service0).deleteFee(1L);
    }

    static Stream<Arguments> accessMatrix() {
        return Stream.of("/api/fees",
                "/api/fees?size=20&keyword=test&sortBy=id",
                "/api/fees/1")
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
            request.header("Authorization", "Bearer " + com.crm.fees.security.TestIdentitySupport.token(
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
