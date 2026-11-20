package com.crm.employee.security;

import com.crm.employee.config.SecurityConfig;
import com.crm.employee.controller.InternalEmployeeProfileController;
import com.crm.employee.repository.EmployeeProfileProjection;
import com.crm.employee.repository.EmployeeRepository;
import com.crm.employee.service.EmployeeProfileSummaryService;
import com.crm.security.JwtSecurityAutoConfiguration;
import io.jsonwebtoken.Jwts;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InternalEmployeeProfileController.class)
@Import({SecurityConfig.class, JwtSecurityAutoConfiguration.class, EmployeeProfileSummaryService.class, InternalEmployeeProfileTest.Trust.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class InternalEmployeeProfileTest extends TestIdentitySupport {
    static final String PATH = "/api/internal/employees/103/profile-summary";
    @Autowired MockMvc mvc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @MockBean EmployeeRepository employees;
    @TestConfiguration static class Trust {
        @Bean @Primary ServiceJwtVerifier profileTestVerifier() {
            return new ServiceJwtVerifier(Map.of("test-key", (RSAPublicKey) ServiceJwtTestTokens.HR.getPublic()), Clock.systemUTC());
        }
    }
    static MockHttpServletRequestBuilder tls(MockHttpServletRequestBuilder request) {
        return request.secure(true).requestAttr("jakarta.servlet.request.cipher_suite", "TLS_AES_128_GCM_SHA256");
    }
    @BeforeEach void projection() {
        when(employees.findProfileSummaryById(103L)).thenReturn(Optional.of(
                new EmployeeProfileProjection(103L, "Test", "Professional", "Developer", 7L, LocalDate.of(2026, 1, 15))));
    }
    @Test void hrReceivesExactlyTheMinimalDtoWithCanonicalStringIdentifiers() throws Exception {
        var response = mvc.perform(tls(get(PATH)).header("Authorization", "Bearer " + ServiceJwtTestTokens.valid()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.employeeId").value("103"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("Professional"))
                .andExpect(jsonPath("$.jobTitle").value("Developer"))
                .andExpect(jsonPath("$.departmentId").value("7"))
                .andExpect(jsonPath("$.hireDate").value("2026-01-15")).andReturn().getResponse();
        var body = json.readTree(response.getContentAsString()); var names = new java.util.HashSet<String>(); body.fieldNames().forEachRemaining(names::add);
        assertThat(names).containsExactlyInAnyOrder("employeeId", "firstName", "lastName", "jobTitle", "departmentId", "hireDate");
        verify(employees).findProfileSummaryById(103L); verifyNoMoreInteractions(employees); assertThat(validations).hasValue(0);
    }
    @Test void nullFieldsAreReturnedWithoutInventingData() throws Exception {
        when(employees.findProfileSummaryById(103L)).thenReturn(Optional.of(new EmployeeProfileProjection(103L, null, null, null, null, null)));
        var response = mvc.perform(tls(get(PATH)).header("Authorization", "Bearer " + ServiceJwtTestTokens.valid()))
                .andExpect(status().isOk()).andReturn().getResponse();
        var body = json.readTree(response.getContentAsString());
        for (String field : List.of("firstName", "lastName", "jobTitle", "departmentId", "hireDate")) assertThat(body.get(field).isNull()).isTrue();
    }
    @Test void longIdentifiersRemainExactStrings() throws Exception {
        String id = "9223372036854775807";
        when(employees.findProfileSummaryById(Long.MAX_VALUE)).thenReturn(Optional.of(
                new EmployeeProfileProjection(Long.MAX_VALUE, null, null, null, 9007199254740993L, null)));
        String token = ServiceJwtTestTokens.builder(Instant.now().minusSeconds(1)).claim("employee_id", id)
                .signWith(ServiceJwtTestTokens.HR.getPrivate(), Jwts.SIG.RS256).compact();
        mvc.perform(tls(get("/api/internal/employees/" + id + "/profile-summary")).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value(id))
                .andExpect(jsonPath("$.departmentId").value("9007199254740993"));
    }
    @Test void nonexistentEmployeeReturnsGeneric404WithoutSensitiveDiagnostics() throws Exception {
        when(employees.findProfileSummaryById(103L)).thenReturn(Optional.empty());
        String token = ServiceJwtTestTokens.valid();
        var response = mvc.perform(tls(get(PATH)).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.message").value("Employee profile not found")).andReturn().getResponse();
        assertThat(response.getContentAsString()).doesNotContain(token, "salary", "address", "dateOfBirth");
    }
    @ParameterizedTest @ValueSource(strings = {"ADMIN", "SUPER_ADMIN", "EMPLOYEE", "CLIENT"})
    void userJwtIsNeverAnInternalServiceCredential(String role) throws Exception {
        mvc.perform(tls(get(PATH)).header("Authorization", "Bearer " + token("synthetic-user", "", true, List.of(role))))
                .andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control", "no-store"));
        verifyNoInteractions(employees); assertThat(validations).hasValue(0);
    }
    @Test void missingAuthenticationReturns401() throws Exception {
        mvc.perform(tls(get(PATH))).andExpect(status().isUnauthorized()); verifyNoInteractions(employees);
    }
    @ParameterizedTest @ValueSource(strings = {"expired", "audience", "permission", "resource"})
    void invalidOrUnauthorizedServiceTokenCannotReadAnyEmployee(String reason) throws Exception {
        var builder = ServiceJwtTestTokens.builder(Instant.now().minusSeconds(1));
        int expected = 401;
        switch (reason) {
            case "expired" -> builder.expiration(Date.from(Instant.now().minusSeconds(10)));
            case "audience" -> builder.audience().clear().add("other-service").and();
            case "permission" -> { builder.claim("permission", "employee:read"); expected = 403; }
            case "resource" -> { builder.claim("employee_id", "104"); expected = 403; }
            default -> throw new IllegalArgumentException();
        }
        String token = builder.signWith(ServiceJwtTestTokens.HR.getPrivate(), Jwts.SIG.RS256).compact();
        var response = mvc.perform(tls(get(PATH)).header("Authorization", "Bearer " + token))
                .andExpect(status().is(expected)).andReturn().getResponse();
        assertThat(response.getContentAsString()).doesNotContain(token); verifyNoInteractions(employees);
    }
    @ParameterizedTest @ValueSource(strings = {"0", "01", "-1", "1.0", "9223372036854775808"})
    void malformedPathIs400AfterAuthentication(String id) throws Exception {
        mvc.perform(tls(get("/api/internal/employees/" + id + "/profile-summary")).header("Authorization", "Bearer " + ServiceJwtTestTokens.valid()))
                .andExpect(status().isBadRequest()); verifyNoInteractions(employees);
    }
    @Test void databaseDiagnosticsAreNotReturned() throws Exception {
        when(employees.findProfileSummaryById(103L)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("Synthetic private database diagnostic"));
        var response = mvc.perform(tls(get(PATH)).header("Authorization", "Bearer " + ServiceJwtTestTokens.valid()))
                .andExpect(status().isInternalServerError()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse();
        assertThat(response.getContentAsString()).doesNotContain("Synthetic private database diagnostic");
    }
    @Test void inconsistentProjectionFailsWithoutReturningAnotherEmployee() throws Exception {
        when(employees.findProfileSummaryById(103L)).thenReturn(Optional.of(new EmployeeProfileProjection(104L, "Other", "Employee", null, null, null)));
        var response = mvc.perform(tls(get(PATH)).header("Authorization", "Bearer " + ServiceJwtTestTokens.valid()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value(PATH)).andReturn().getResponse();
        var fields = new java.util.HashSet<String>();
        json.readTree(response.getContentAsString()).fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("timestamp", "status", "error", "message", "path");
        // Bare digits can legitimately occur in the error timestamp.
        assertThat(response.getContentAsString()).doesNotContain("Other", "Employee", "\"104\"");
    }
}
