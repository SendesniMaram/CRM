package com.crm.hr;

import com.crm.hr.client.LeaveDirectoryClient;
import com.crm.hr.entity.LeaveEmployeeAssociation;
import com.crm.hr.exception.LeaveAssociationConflictException;
import com.crm.hr.repository.LeaveEmployeeAssociationRepository;
import com.crm.hr.repository.LeaveRepository;
import com.crm.hr.security.TestIdentitySupport;
import com.crm.hr.service.EmployeeAssociationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:employee_association_contract;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.cloud.service-registry.auto-registration.enabled=false", "eureka.client.enabled=false"})
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class EmployeeAssociationIntegrationTest extends TestIdentitySupport {
    private static final String PATH = "/api/employee-associations";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @SpyBean LeaveEmployeeAssociationRepository links;
    @Autowired LeaveRepository leaves;
    @Autowired EmployeeAssociationService service;
    @MockBean LeaveDirectoryClient directory;

    @BeforeEach void prepare() {
        leaves.deleteAll();
        links.deleteAll();
        when(directory.read(eq("identity-service"), anyString(), anyString())).thenAnswer(call -> {
            long id = Long.parseLong(call.<String>getArgument(1).substring("/api/users/".length()));
            return json.createObjectNode().put("id", id).put("username", "directory-" + id)
                    .put("enabled", true).put("accountNonLocked", true)
                    .set("roles", json.createArrayNode().add("EMPLOYEE"));
        });
        when(directory.read(eq("employee-service"), anyString(), anyString())).thenAnswer(call ->
                json.createObjectNode().put("id", Long.parseLong(call.<String>getArgument(1).substring("/api/employees/".length()))));
    }

    String auth(long uid, String role) {
        return "Bearer " + signed("signed-username-" + uid, true, List.of(role), Long.toString(uid), Instant.now().plusSeconds(600));
    }
    String body(String uid, String employee) { return "{\"userId\":\"" + uid + "\",\"employeeId\":\"" + employee + "\"}"; }
    void seed(long uid, String employee) {
        var link = new LeaveEmployeeAssociation();
        link.setUserId(uid); link.setEmployeeId(employee); link.setUsername("directory-" + uid);
        links.saveAndFlush(link);
    }

    @ParameterizedTest @ValueSource(strings = {"ADMIN", "SUPER_ADMIN"})
    void administratorCreatesAndReadsExplicitDifferentIdentifiers(String role) throws Exception {
        String bearer = auth(1, role);
        mvc.perform(post(PATH).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body("42", "101")))
                .andExpect(status().isCreated()).andExpect(header().string("Location", PATH + "/users/42"))
                .andExpect(jsonPath("$.userId").value("42")).andExpect(jsonPath("$.employeeId").value("101"))
                .andExpect(jsonPath("$.username").doesNotExist());
        mvc.perform(get(PATH + "/users/42").header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("101"));
        assertThat(links.count()).isEqualTo(1);
        verify(directory).read("identity-service", "/api/users/42", bearer);
        verify(directory).read("employee-service", "/api/employees/101", bearer);
    }

    @Test void repeatedCreationIsIdempotentAndLegacyRouteUsesSameRegistry() throws Exception {
        String bearer = auth(1, "ADMIN");
        mvc.perform(post(PATH).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body("42", "101")))
                .andExpect(status().isCreated());
        mvc.perform(post(PATH).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body("42", "101")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/leaves/associations").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":42,\"employeeId\":101}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.username").value("directory-42"));
        mvc.perform(get("/api/leaves/associations").header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        assertThat(links.count()).isEqualTo(1);
    }

    @Test void legacyCreationIsVisibleThroughNewApi() throws Exception {
        String bearer = auth(1, "ADMIN");
        mvc.perform(post("/api/leaves/associations").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":42,\"employeeId\":101}"))
                .andExpect(status().isOk());
        mvc.perform(get(PATH + "/users/42").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value("101"));
    }

    @Test void multiRoleEmployeeIsEligible() throws Exception {
        var account = json.createObjectNode().put("id", 42).put("username", "directory-42")
                .put("enabled", true).put("accountNonLocked", true);
        account.set("roles", json.createArrayNode().add("ADMIN").add("EMPLOYEE"));
        when(directory.read(eq("identity-service"), anyString(), anyString())).thenReturn(account);
        mvc.perform(post(PATH).header("Authorization", auth(1, "ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content(body("42", "101"))).andExpect(status().isCreated());
    }

    @ParameterizedTest @ValueSource(strings = {"disabled", "locked", "client"})
    void ineligibleTargetNeverCreatesLink(String kind) throws Exception {
        var account = json.createObjectNode().put("id", 42).put("username", "directory-42")
                .put("enabled", !kind.equals("disabled")).put("accountNonLocked", !kind.equals("locked"));
        account.set("roles", json.createArrayNode().add(kind.equals("client") ? "CLIENT" : "EMPLOYEE"));
        when(directory.read(eq("identity-service"), anyString(), anyString())).thenReturn(account);
        mvc.perform(post(PATH).header("Authorization", auth(1, "ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content(body("42", "101"))).andExpect(status().isBadRequest());
        assertThat(links.count()).isZero();
        verify(directory, never()).read(eq("employee-service"), anyString(), anyString());
    }

    @ParameterizedTest @ValueSource(strings = {"identity-service", "employee-service"})
    void missingReferenceIsNotFound(String dependency) throws Exception {
        when(directory.read(eq(dependency), anyString(), anyString())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(post(PATH).header("Authorization", auth(1, "ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content(body("42", "101"))).andExpect(status().isNotFound());
        assertThat(links.count()).isZero();
    }

    @ParameterizedTest @ValueSource(strings = {"identity-service", "employee-service"})
    void unavailableDependencyNeverWrites(String dependency) throws Exception {
        for (int remoteStatus : List.of(401, 403, 503)) {
            doThrow(new ResponseStatusException(HttpStatus.valueOf(remoteStatus)))
                    .when(directory).read(eq(dependency), anyString(), anyString());
            mvc.perform(post(PATH).header("Authorization", auth(1, "ADMIN")).contentType(MediaType.APPLICATION_JSON)
                    .content(body("42", "101"))).andExpect(status().is(remoteStatus));
            assertThat(links.count()).isZero();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"identity-service", "employee-service"})
    void malformedDependencyIsBadGateway(String dependency) throws Exception {
        when(directory.read(eq(dependency), anyString(), anyString())).thenReturn(json.createObjectNode().put("id", "42"));
        mvc.perform(post(PATH).header("Authorization", auth(1, "ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content(body("42", "101"))).andExpect(status().isBadGateway());
        assertThat(links.count()).isZero();
    }

    @ParameterizedTest @ValueSource(strings = {"identity-service", "employee-service"})
    void mismatchedReferenceNeverCreatesLink(String dependency) throws Exception {
        if (dependency.equals("identity-service")) {
            var account = json.createObjectNode().put("id", 999).put("username", "other")
                    .put("enabled", true).put("accountNonLocked", true);
            account.set("roles", json.createArrayNode().add("EMPLOYEE"));
            when(directory.read(eq(dependency), anyString(), anyString())).thenReturn(account);
        } else when(directory.read(eq(dependency), anyString(), anyString())).thenReturn(json.createObjectNode().put("id", 999));
        mvc.perform(post(PATH).header("Authorization", auth(1, "ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content(body("42", "101"))).andExpect(status().isBadRequest());
        assertThat(links.count()).isZero();
    }

    @ParameterizedTest @ValueSource(strings = {"EMPLOYEE", "CLIENT"})
    void ordinaryUsersCannotCreateOrReadForeignAssociation(String role) throws Exception {
        seed(84, "202");
        String bearer = auth(42, role);
        mvc.perform(post(PATH).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body("42", "101")))
                .andExpect(status().isForbidden());
        mvc.perform(get(PATH + "/users/84").header("Authorization", bearer)).andExpect(status().isForbidden());
        assertThat(links.count()).isEqualTo(1);
        verifyNoInteractions(directory);
    }

    @Test void meUsesOnlyValidatedUidEvenWhenUsernameDiffersOrHeadersAreForged() throws Exception {
        seed(42, "101"); seed(84, "202");
        mvc.perform(get(PATH + "/me").header("Authorization", auth(42, "EMPLOYEE"))
                .header("X-User-Id", "84").header("X-Username", "directory-84").header("X-Roles", "SUPER_ADMIN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value("42"))
                .andExpect(jsonPath("$.employeeId").value("101"));
        mvc.perform(get(PATH + "/me").header("Authorization", auth(84, "EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value("202"));
        mvc.perform(get(PATH + "/me?userId=84").header("Authorization", auth(42, "EMPLOYEE")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(directory);
    }

    @Test void recreatedUsernameDoesNotRecoverAnotherUsersAssociation() throws Exception {
        seed(42, "101");
        String bearer = "Bearer " + signed("directory-42", true, List.of("EMPLOYEE"), "84", Instant.now().plusSeconds(60));
        mvc.perform(get(PATH + "/me").header("Authorization", bearer)).andExpect(status().isNotFound());
    }

    @Test void missingAssociationIsNotFound() throws Exception {
        mvc.perform(get(PATH + "/me").header("Authorization", auth(42, "EMPLOYEE"))).andExpect(status().isNotFound());
        mvc.perform(get(PATH + "/users/42").header("Authorization", auth(1, "ADMIN"))).andExpect(status().isNotFound());
    }

    @ParameterizedTest @ValueSource(strings = {"ADMIN", "SUPER_ADMIN", "CLIENT"})
    void meRequiresEmployeeRole(String role) throws Exception {
        seed(42, "101");
        mvc.perform(get(PATH + "/me").header("Authorization", auth(42, role))).andExpect(status().isForbidden());
    }

    @Test void missingOrInvalidBearerAndUnavailableIdentityNeverReachBusinessOperation() throws Exception {
        mvc.perform(get(PATH + "/me")).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH + "/me").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        identityStatus = 503;
        mvc.perform(post(PATH).header("Authorization", auth(1, "ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content(body("42", "101"))).andExpect(status().isServiceUnavailable());
        verifyNoInteractions(directory);
        assertThat(links.count()).isZero();
    }

    @ParameterizedTest @ValueSource(strings = {"0", "01", "-1", "1.0", " 42", "abc", "9223372036854775808"})
    void invalidIdentifiersAreBadRequest(String id) throws Exception {
        String bearer = auth(1, "ADMIN");
        mvc.perform(post(PATH).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body(id, "101")))
                .andExpect(status().isBadRequest());
        mvc.perform(post(PATH).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body("42", id)))
                .andExpect(status().isBadRequest());
        mvc.perform(get(PATH + "/users/{userId}", id).header("Authorization", bearer))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(directory);
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"userId\":42,\"employeeId\":101}", "{\"userId\":true,\"employeeId\":\"101\"}", "{\"userId\":null,\"employeeId\":\"101\"}"})
    void missingOrNonStringIdsAreBadRequest(String request) throws Exception {
        mvc.perform(post(PATH).header("Authorization", auth(1, "ADMIN")).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest());
        assertThat(links.count()).isZero();
    }

    @Test void uniqueUserAndEmployeeAndUsernameAreNeverReassignedByCreation() throws Exception {
        seed(42, "101");
        String bearer = auth(1, "ADMIN");
        for (String request : List.of(body("42", "202"), body("84", "101"))) {
            mvc.perform(post(PATH).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isConflict());
        }
        var reusedName = json.createObjectNode().put("id", 84).put("username", "directory-42")
                .put("enabled", true).put("accountNonLocked", true);
        reusedName.set("roles", json.createArrayNode().add("EMPLOYEE"));
        when(directory.read(eq("identity-service"), anyString(), anyString())).thenReturn(reusedName);
        mvc.perform(post("/api/leaves/associations").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":84,\"employeeId\":101}"))
                .andExpect(status().isBadRequest());
        assertThat(links.count()).isEqualTo(1);
        assertThat(links.findByUserId(42L).orElseThrow().getEmployeeId()).isEqualTo("101");
        assertThat(links.findByUserId(84L)).isEmpty();
    }

    @Test void noNewDeleteOrReassignmentEndpointIsExposed() throws Exception {
        for (String role : List.of("ADMIN", "SUPER_ADMIN", "EMPLOYEE", "CLIENT")) {
            mvc.perform(delete(PATH + "/users/42").header("Authorization", auth(1, role))).andExpect(status().isForbidden());
            mvc.perform(put(PATH + "/users/42").header("Authorization", auth(1, role))
                    .contentType(MediaType.APPLICATION_JSON).content(body("42", "101"))).andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest @ValueSource(strings = {"employee", "user"})
    void concurrentCreationsHaveOnlyOneOwnerAndNoPartialAssociation(String collision) throws Exception {
        var barrier = new CyclicBarrier(2);
        when(directory.read(eq("employee-service"), anyString(), anyString())).thenAnswer(call -> {
            barrier.await(5, TimeUnit.SECONDS);
            return json.createObjectNode().put("id", Long.parseLong(call.<String>getArgument(1).substring("/api/employees/".length())));
        });
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> concurrentCreate(42, 101));
            var b = pool.submit(() -> concurrentCreate(collision.equals("employee") ? 84 : 42,
                    collision.equals("employee") ? 101 : 202));
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
            assertThat(links.count()).isEqualTo(1);
            assertThat(links.findAll().get(0).getEmployeeId()).isIn("101", "202");
        } finally { pool.shutdownNow(); }
    }

    @Test
    void staleConflictChecksCannotOverwriteACommittedAssociation() throws Exception {
        var secondRequestChecked = new CountDownLatch(1);
        var firstRequestCommitted = new CountDownLatch(1);
        doAnswer(call -> {
            String employeeId = call.getArgument(0);
            if (employeeId.equals("202")) {
                secondRequestChecked.countDown();
                assertThat(firstRequestCommitted.await(10, TimeUnit.SECONDS)).isTrue();
            } else if (employeeId.equals("101")) {
                assertThat(secondRequestChecked.await(10, TimeUnit.SECONDS)).isTrue();
            }
            // Both checks observe the initially empty registry; the second returns
            // that stale result only after the first request has committed.
            return false;
        }).when(links).existsByEmployeeId(anyString());
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> {
                try { return concurrentCreate(42, 101); }
                finally { firstRequestCommitted.countDown(); }
            });
            var second = pool.submit(() -> concurrentCreate(42, 202));
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(201);
            assertThat(second.get(15, TimeUnit.SECONDS)).isEqualTo(409);
            assertThat(links.count()).isEqualTo(1);
            assertThat(links.findByUserId(42L).orElseThrow().getEmployeeId()).isEqualTo("101");
        } finally { pool.shutdownNow(); }
    }

    private int concurrentCreate(long userId, long employeeId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("synthetic-admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            service.create(userId, employeeId, "Bearer synthetic-directory-call");
            return 201;
        } catch (LeaveAssociationConflictException ex) { return 409; }
        finally { SecurityContextHolder.clearContext(); }
    }
}
