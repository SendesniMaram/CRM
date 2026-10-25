package com.crm.department;
import com.crm.department.config.SecurityConfig;
import com.crm.department.security.TestIdentitySupport;
import com.crm.security.JwtSecurityAutoConfiguration;
import java.util.*;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CentralizedJwtSecurityTest.Probe.class)
@Import({SecurityConfig.class, JwtSecurityAutoConfiguration.class, CentralizedJwtSecurityTest.Probe.class})
@AutoConfigureMockMvc(addFilters = true, print = MockMvcPrint.NONE)
class CentralizedJwtSecurityTest extends TestIdentitySupport {
    static final String PATH = "/api/departments/sprint1b-probe";
    static final AtomicInteger handlers = new AtomicInteger();
    @Autowired MockMvc mvc;
    @BeforeEach void clearHandlers() { handlers.set(0); }
    String auth(String role) { return "Bearer " + token("synthetic-user", "", true, List.of(role)); }
    @RestController static class Probe {
        @GetMapping({PATH, PATH + "/pdf"}) String read() { handlers.incrementAndGet(); return "ok"; }
        @GetMapping("/api/test") String diagnostic() { return "public"; }
        @PostMapping(PATH) String write() { handlers.incrementAndGet(); return "ok"; }
    }
    @Test void activeSignedAdministratorCanReadAndWriteWithOriginalBearer() throws Exception {
        String bearer = auth("ADMIN");
        mvc.perform(get(PATH).header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(post(PATH).header("Authorization", bearer)).andExpect(status().isOk());
        assertThat(handlers).hasValue(2);
        assertThat(validations).hasValue(2);
        assertThat(lastBearer.get()).isEqualTo(bearer);
    }
    @ParameterizedTest @ValueSource(strings = {"disabled", "locked", "roleRemoved"})
    void currentAccountRejectionIsAppliedToSameTokenOnNextRequest(String reason) throws Exception {
        String bearer = auth("ADMIN");
        mvc.perform(get(PATH).header("Authorization", bearer)).andExpect(status().isOk());
        handlers.set(0);
        if (reason.equals("disabled")) enabled = false;
        else if (reason.equals("locked")) locked = true;
        else roleRemoved = true;
        mvc.perform(get(PATH).header("Authorization", bearer)).andExpect(status().isUnauthorized());
        mvc.perform(post(PATH).header("Authorization", bearer)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH + "/pdf").header("Authorization", bearer)).andExpect(status().isUnauthorized());
        assertThat(handlers).hasValue(0);
        assertThat(validations).hasValue(4);
    }
    @ParameterizedTest @ValueSource(strings = {"EMPLOYEE", "CLIENT"})
    void insufficientRoleRemains403(String role) throws Exception {
        mvc.perform(post(PATH).header("Authorization", auth(role))).andExpect(status().isForbidden());
        assertThat(handlers).hasValue(0);
        assertThat(validations).hasValue(1);
    }
    @Test void employeeKeepsLegitimateDepartmentReadPermission() throws Exception {
        mvc.perform(get(PATH).header("Authorization", auth("EMPLOYEE"))).andExpect(status().isOk());
        assertThat(handlers).hasValue(1);
    }
    @ParameterizedTest @ValueSource(ints = {200, 403, 404, 429, 500, 503})
    void impossibleValidationNeverRunsBusinessHandler(int remote) throws Exception {
        identityStatus = remote;
        mvc.perform(get(PATH).header("Authorization", auth("ADMIN"))).andExpect(status().isServiceUnavailable());
        mvc.perform(post(PATH).header("Authorization", auth("ADMIN"))).andExpect(status().isServiceUnavailable());
        mvc.perform(get(PATH + "/pdf").header("Authorization", auth("ADMIN"))).andExpect(status().isServiceUnavailable());
        assertThat(handlers).hasValue(0);
    }
    @ParameterizedTest @ValueSource(strings = {"invalid", "expired", "uid", "legacy"})
    void localInvalidityDoesNotCallIdentityOrHandler(String reason) throws Exception {
        String jwt = reason.equals("invalid") ? "bad-token" : signed("synthetic-user", true, List.of("ADMIN"),
                reason.equals("uid") ? "01" : reason.equals("legacy") ? null : "42",
                reason.equals("expired") ? Instant.now().minusSeconds(5) : Instant.now().plusSeconds(60));
        mvc.perform(get(PATH).header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
        assertThat(validations).hasValue(0); assertThat(handlers).hasValue(0);
    }
    @Test void missingBearerDoesNotRunHandler() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        assertThat(validations).hasValue(0); assertThat(handlers).hasValue(0);
    }

    @Test void publicDiagnosticDoesNotDependOnIdentity() throws Exception {
        identityStatus = 503;
        mvc.perform(get("/api/test")).andExpect(status().isOk());
        assertThat(validations).hasValue(0);
    }
    @Test void diagnosticPrefixDoesNotExemptProtectedData() throws Exception {
        mvc.perform(get("/api/test/private")).andExpect(status().isUnauthorized());
        assertThat(validations).hasValue(0);
    }
    @Test void saturatedClientRefusesAdditionalProtectedRequest() throws Exception {
        delayMillis = 3000;
        var workers = Executors.newFixedThreadPool(4);
        var pending = new ArrayList<Future<Integer>>();
        String bearer = auth("ADMIN");
        try {
            for (int i = 0; i < 4; i++) pending.add(workers.submit(() ->
                    mvc.perform(get(PATH).header("Authorization", bearer)).andReturn().getResponse().getStatus()));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (validations.get() < 4 && System.nanoTime() < deadline) Thread.sleep(5);
            assertThat(validations).hasValue(4);
            mvc.perform(get(PATH).header("Authorization", bearer)).andExpect(status().isServiceUnavailable());
            for (var request : pending) assertThat(request.get(5, TimeUnit.SECONDS)).isEqualTo(503);
            assertThat(handlers).hasValue(0);
        } finally { workers.shutdownNow(); delayMillis = 0; }
    }
}
