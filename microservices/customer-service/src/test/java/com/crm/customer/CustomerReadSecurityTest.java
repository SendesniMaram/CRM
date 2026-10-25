package com.crm.customer;

import com.crm.customer.controller.CustomerController;
import com.crm.customer.dto.CustomerResponse;
import com.crm.customer.service.ICustomerService;
import com.crm.customer.config.SecurityConfig;
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
@WebMvcTest(controllers = {CustomerController.class})
@Import({SecurityConfig.class, JwtSecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = true, print = MockMvcPrint.NONE)
class CustomerReadSecurityTest extends com.crm.customer.security.TestIdentitySupport {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @org.junit.jupiter.api.Test
    void linkedCustomerDeletionReturnsSanitizedConflict() throws Exception {
        doThrow(new org.springframework.dao.DataIntegrityViolationException("private-database-diagnostic"))
                .when(service0).deleteCustomer(1L);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/customers/1")
                .header("Authorization", "Bearer " + token("security-test-user", "", true, List.of("ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Operation conflicts with an existing reference"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-database-diagnostic"))));
    }
    @MockBean ICustomerService service0;

    @BeforeEach
    void responses() {
        CustomerResponse row0 = new CustomerResponse();
        row0.setId(1L);
        when(service0.getAllCustomers()).thenReturn(List.of(row0));
        when(service0.getAllCustomersPaged(anyInt(), anyInt(), any(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of(row0)));
        when(service0.getCustomerById(1L)).thenReturn(row0);
    }


    static Stream<Arguments> writeAccessMatrix() {
        return Stream.of("POST", "PUT", "DELETE").flatMap(method ->
                Stream.of("ANONYMOUS", "EMPLOYEE", "CLIENT", "ADMIN", "SUPER_ADMIN")
                        .map(role -> Arguments.of(method, role)));
    }

    @ParameterizedTest(name = "{1} {0} requires administrative authorization")
    @MethodSource("writeAccessMatrix")
    void administrativeWritesRequireAnAdministrator(String method, String role) throws Exception {
        var response = new CustomerResponse();
        response.setId(1L);
        lenient().when(service0.createCustomer(any())).thenReturn(response);
        lenient().when(service0.updateCustomer(eq(1L), any())).thenReturn(response);
        var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                org.springframework.http.HttpMethod.valueOf(method),
                "/api/customers" + (method.equals("POST") ? "" : "/1"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Security customer\"}");
        if (!role.equals("ANONYMOUS")) {
            request.header("Authorization", "Bearer " + com.crm.customer.security.TestIdentitySupport.token(
                    "security-test-user", "", true, List.of(role)));
        }
        boolean admin = role.equals("ADMIN") || role.equals("SUPER_ADMIN");
        int expected = role.equals("ANONYMOUS") ? 401 : !admin ? 403
                : method.equals("POST") ? 201 : method.equals("DELETE") ? 204 : 200;
        mvc.perform(request).andExpect(status().is(expected));
        if (!admin) verifyNoInteractions(service0);
        else if (method.equals("POST")) verify(service0).createCustomer(any());
        else if (method.equals("PUT")) verify(service0).updateCustomer(eq(1L), any());
        else verify(service0).deleteCustomer(1L);
    }

    static Stream<Arguments> accessMatrix() {
        return Stream.of("/api/customers",
                "/api/customers?size=20&keyword=test&sortBy=id",
                "/api/customers/1")
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
            request.header("Authorization", "Bearer " + com.crm.customer.security.TestIdentitySupport.token(
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
