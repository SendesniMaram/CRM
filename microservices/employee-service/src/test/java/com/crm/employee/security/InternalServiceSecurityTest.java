package com.crm.employee.security;

import com.crm.employee.config.SecurityConfig;
import com.crm.security.AuthenticatedUserPrincipal;
import com.crm.security.JwtSecurityAutoConfiguration;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InternalServiceSecurityTest.Probe.class)
@Import({SecurityConfig.class, JwtSecurityAutoConfiguration.class, InternalServiceSecurityTest.Probe.class, InternalServiceSecurityTest.Trust.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class InternalServiceSecurityTest extends TestIdentitySupport {
    static final String PATH = "/api/internal/employees/103/profile-summary";
    static final AtomicInteger calls = new AtomicInteger();
    @Autowired MockMvc mvc;
    @BeforeEach void resetCalls() { calls.set(0); }
    @TestConfiguration static class Trust {
        @Bean @Primary ServiceJwtVerifier testVerifier() {
            return new ServiceJwtVerifier(Map.of("test-key", (RSAPublicKey) ServiceJwtTestTokens.HR.getPublic()), Clock.systemUTC());
        }
    }
    /** Security probe only. No production profile controller or Employee business read exists in F1.1. */
    @RestController static class Probe {
        @GetMapping("/api/internal/employees/{id}/profile-summary") Map<String, Object> internal(Authentication auth) {
            calls.incrementAndGet();
            assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedServicePrincipal.class);
            assertThat(auth.getAuthorities()).extracting("authority").containsExactly(ServiceJwtVerifier.AUTHORITY);
            assertThatThrownBy(() -> AuthenticatedUserPrincipal.require(auth)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            return Map.of("service", auth.getName());
        }
        @GetMapping("/api/employees/service-security-probe") String publicRead() { calls.incrementAndGet(); return "public"; }
    }
    static MockHttpServletRequestBuilder tls(MockHttpServletRequestBuilder request) {
        return request.secure(true).requestAttr("jakarta.servlet.request.cipher_suite", "TLS_AES_128_GCM_SHA256");
    }
    @Test void dedicatedChainAcceptsAValidServiceOnlyOverContainerTls() throws Exception {
        mvc.perform(tls(get(PATH)).header("Authorization", "Bearer " + ServiceJwtTestTokens.valid()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.service").value("hr-service"))
                .andExpect(header().string("Cache-Control", "no-store"));
        assertThat(calls).hasValue(1); assertThat(validations).hasValue(0);
    }
    @Test void plaintextAndForwardedHeadersCannotAuthenticateTheTransport() throws Exception {
        mvc.perform(get(PATH).header("Authorization", "Bearer " + ServiceJwtTestTokens.valid())
                .header("X-Forwarded-Proto", "https").header("X-Service-Name", "hr-service"))
                .andExpect(status().isForbidden());
        mvc.perform(get(PATH).secure(true).header("Authorization", "Bearer " + ServiceJwtTestTokens.valid()))
                .andExpect(status().isForbidden());
        assertThat(calls).hasValue(0); assertThat(validations).hasValue(0);
    }
    @Test void absentOrForgedCredentialIsUnauthorizedEvenOnDirectTlsAccess() throws Exception {
        mvc.perform(tls(get(PATH))).andExpect(status().isUnauthorized());
        mvc.perform(tls(get(PATH)).header("Authorization", "Bearer malformed")).andExpect(status().isUnauthorized());
        mvc.perform(tls(get(PATH)).header("Authorization", "Bearer " + ServiceJwtTestTokens.valid(), "Bearer " + ServiceJwtTestTokens.valid()))
                .andExpect(status().isUnauthorized());
        assertThat(calls).hasValue(0);
    }
    @ParameterizedTest @ValueSource(strings = {"ADMIN", "SUPER_ADMIN", "EMPLOYEE", "CLIENT"})
    void userTokensCannotEnterInternalChain(String role) throws Exception {
        mvc.perform(tls(get(PATH)).header("Authorization", "Bearer " + token("synthetic-user", "", true, List.of(role))))
                .andExpect(status().isUnauthorized());
        assertThat(calls).hasValue(0); assertThat(validations).hasValue(0);
    }
    @Test void serviceTokensCannotAuthenticatePublicEmployeeReads() throws Exception {
        mvc.perform(get("/api/employees/service-security-probe").header("Authorization", "Bearer " + ServiceJwtTestTokens.valid()))
                .andExpect(status().isUnauthorized());
        assertThat(calls).hasValue(0); assertThat(validations).hasValue(0);
    }
    @ParameterizedTest @ValueSource(strings = {"ADMIN", "SUPER_ADMIN"})
    void administratorReadsStillUseIdentityValidation(String role) throws Exception {
        mvc.perform(get("/api/employees/service-security-probe").header("Authorization", "Bearer " + token("synthetic-user", "", true, List.of(role))))
                .andExpect(status().isOk());
        assertThat(validations).hasValue(1);
    }
    @ParameterizedTest @ValueSource(strings = {"EMPLOYEE", "CLIENT"})
    void ordinaryUsersStillCannotReadAdministrativeResources(String role) throws Exception {
        mvc.perform(get("/api/employees/service-security-probe").header("Authorization", "Bearer " + token("synthetic-user", "", true, List.of(role))))
                .andExpect(status().isForbidden());
        assertThat(calls).hasValue(0);
    }
    @Test void namespaceHasNoPermissiveRouteMethodResourceOrQueryFallback() throws Exception {
        String token = "Bearer " + ServiceJwtTestTokens.valid();
        mvc.perform(tls(get("/api/internal/other")).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(tls(get("/api/internal/employees/104/profile-summary")).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(tls(post(PATH)).header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(tls(get(PATH + "?employeeId=104")).header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(tls(get("/api/internal/employees/01/profile-summary")).header("Authorization", token)).andExpect(status().isBadRequest());
        assertThat(calls).hasValue(0); assertThat(validations).hasValue(0);
    }
}
