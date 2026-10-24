package com.crm.hr;

import com.crm.hr.client.LeaveDirectoryClient;
import com.crm.hr.entity.LeaveEmployeeAssociation;
import com.crm.hr.entity.LeaveRequest;
import com.crm.hr.repository.LeaveEmployeeAssociationRepository;
import com.crm.hr.repository.LeaveRepository;
import com.crm.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.cloud.service-registry.auto-registration.enabled=false", "eureka.client.enabled=false"})
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@Transactional
class LeaveSecurityIntegrationTest extends com.crm.hr.security.TestIdentitySupport {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;
    @Autowired LeaveRepository leaves;
    @Autowired LeaveEmployeeAssociationRepository links;
    @MockBean LeaveDirectoryClient directory;
    Long aId;
    @BeforeEach void setup() {
        leaves.deleteAll(); links.deleteAll();
        link("employee-a", 1L, "101"); link("employee-b", 2L, "102");
        LeaveRequest leave = new LeaveRequest(); leave.setEmployeeId("101");
        leave.setStartDate(LocalDate.now().plusDays(2)); leave.setEndDate(LocalDate.now().plusDays(4));
        aId = leaves.saveAndFlush(leave).getId();
    }
    void link(String username, Long userId, String employeeId) {
        LeaveEmployeeAssociation link = new LeaveEmployeeAssociation();
        link.setUsername(username); link.setUserId(userId); link.setEmployeeId(employeeId); links.saveAndFlush(link);
    }
    String auth(String username, String role) {
        String uid = username.equals("employee-a") ? "1" : username.equals("employee-b") ? "2" : "99";
        return auth(uid, username, role);
    }
    String auth(String uid, String username, String role) {
        return "Bearer " + signed(username, true, List.of(role), uid, java.time.Instant.now().plusSeconds(600));
    }
    String body() { return "{\"startDate\":\"" + LocalDate.now().plusDays(2) + "\",\"endDate\":\"" + LocalDate.now().plusDays(4) + "\",\"reason\":\"Leave\"}"; }
    @Test void employeeCreatesPendingUsingServerAssociationDespiteSpoofedEmployeeId() throws Exception {
        mvc.perform(post("/api/leaves/mine").header("Authorization", auth("employee-a", "EMPLOYEE"))
                .contentType(MediaType.APPLICATION_JSON).content(body().replace("}", ",\"employeeId\":\"102\",\"validatorUsername\":\"forged\"}")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.employeeId").value("101"))
                .andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.validatorUsername").isEmpty());
    }
    @Test void employeeOwnListAndDetail() throws Exception {
        mvc.perform(get("/api/leaves").header("Authorization", auth("employee-a", "EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/leaves/" + aId).header("Authorization", auth("employee-a", "EMPLOYEE"))).andExpect(status().isOk());
    }
    @Test void employeeOtherListAndPagedSearchAreFiltered() throws Exception {
        mvc.perform(get("/api/leaves").header("Authorization", auth("employee-b", "EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/leaves?size=20&keyword=101").header("Authorization", auth("employee-b", "EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void crossAccountReadUpdateAndDeleteForbidden() throws Exception {
        String b = auth("employee-b", "EMPLOYEE");
        mvc.perform(get("/api/leaves/" + aId).header("Authorization", b)).andExpect(status().isForbidden());
        mvc.perform(put("/api/leaves/mine/" + aId).header("Authorization", b).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/leaves/" + aId).header("Authorization", b)).andExpect(status().isForbidden());
        assertTrue(leaves.existsById(aId));
    }
    @Test void ownPendingUpdateAndDeleteAllowed() throws Exception {
        String a = auth("employee-a", "EMPLOYEE");
        mvc.perform(put("/api/leaves/mine/" + aId).header("Authorization", a).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value("101"));
        mvc.perform(delete("/api/leaves/" + aId).header("Authorization", a)).andExpect(status().isNoContent());
        assertFalse(leaves.existsById(aId));
    }
    @Test void employeeCannotDecideOrUseAdministrativeCreateOrUpdate() throws Exception {
        String a = auth("employee-a", "EMPLOYEE");
        for (String action : List.of("approve", "reject"))
            mvc.perform(patch("/api/leaves/" + aId + "/" + action).header("Authorization", a)).andExpect(status().isForbidden());
        mvc.perform(post("/api/leaves").header("Authorization", a).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
        mvc.perform(put("/api/leaves/" + aId).header("Authorization", a).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
    }
    @Test void missingAssociationFailsClosed() throws Exception {
        String auth = auth("unlinked", "EMPLOYEE");
        mvc.perform(get("/api/leaves").header("Authorization", auth)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Employee account is not associated for leave requests"));
        mvc.perform(post("/api/leaves/mine").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
    }
    @Test void adminApprovesRecordsValidatorAndCannotDecideAgain() throws Exception {
        String admin = auth("admin", "ADMIN");
        mvc.perform(get("/api/leaves").header("Authorization", admin)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(patch("/api/leaves/" + aId + "/approve").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED")).andExpect(jsonPath("$.validatorUsername").value("admin"));
        mvc.perform(patch("/api/leaves/" + aId + "/reject").header("Authorization", admin)).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/leaves/" + aId + "/approve").header("Authorization", admin)).andExpect(status().isBadRequest());
        assertEquals("admin", leaves.findById(aId).orElseThrow().getValidatorUsername());
    }
    @Test void superAdminRejectsAndDecidedRequestCannotBeEditedOrDeleted() throws Exception {
        mvc.perform(patch("/api/leaves/" + aId + "/reject").header("Authorization", auth("super", "SUPER_ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED")).andExpect(jsonPath("$.validatorUsername").value("super"));
        String a = auth("employee-a", "EMPLOYEE");
        mvc.perform(put("/api/leaves/mine/" + aId).header("Authorization", a).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/leaves/" + aId).header("Authorization", a)).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/leaves/" + aId + "/approve").header("Authorization", auth("super", "SUPER_ADMIN"))).andExpect(status().isBadRequest());
    }
    @Test void clientHasNoLeaveAccessAndUnauthenticatedIs401() throws Exception {
        String client = auth("client", "CLIENT");
        mvc.perform(get("/api/leaves").header("Authorization", client)).andExpect(status().isForbidden());
        mvc.perform(get("/api/leaves/" + aId).header("Authorization", client)).andExpect(status().isForbidden());
        mvc.perform(post("/api/leaves/mine").header("Authorization", client).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
        mvc.perform(put("/api/leaves/mine/" + aId).header("Authorization", client).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/leaves/" + aId).header("Authorization", client)).andExpect(status().isForbidden());
        for (String action : List.of("approve", "reject"))
            mvc.perform(patch("/api/leaves/" + aId + "/" + action).header("Authorization", client)).andExpect(status().isForbidden());
        mvc.perform(get("/api/leaves/associations").header("Authorization", client)).andExpect(status().isForbidden());
        mvc.perform(get("/api/leaves")).andExpect(status().isUnauthorized());
    }
    @Test void invalidDatesRejected() throws Exception {
        String a = auth("employee-a", "EMPLOYEE");
        for (String request : List.of("{}", "{\"startDate\":\"2000-01-01\",\"endDate\":\"2000-01-02\"}",
                "{\"startDate\":\"" + LocalDate.now().plusDays(4) + "\",\"endDate\":\"" + LocalDate.now().plusDays(2) + "\"}"))
            mvc.perform(post("/api/leaves/mine").header("Authorization", a).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isBadRequest());
    }
    @Test void associationRequiresAdminAndVerifiedEmployeeAccount() throws Exception {
        String payload = "{\"userId\":3,\"employeeId\":103}";
        mvc.perform(post("/api/leaves/associations").header("Authorization", auth("employee-a", "EMPLOYEE")).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
        mvc.perform(get("/api/leaves/associations/options").header("Authorization", auth("employee-a", "EMPLOYEE"))).andExpect(status().isForbidden());
        when(directory.read(eq("identity-service"), eq("/api/users/3"), anyString()))
                .thenReturn(json.readTree("{\"id\":3,\"username\":\"employee-c\",\"roleType\":\"EMPLOYEE\",\"roles\":[\"EMPLOYEE\"],\"enabled\":true,\"accountNonLocked\":true}"));
        when(directory.read(eq("employee-service"), eq("/api/employees/103"), anyString()))
                .thenReturn(json.readTree("{\"id\":103}"));
        mvc.perform(post("/api/leaves/associations").header("Authorization", auth("admin", "ADMIN")).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("employee-c")).andExpect(jsonPath("$.employeeId").value("103"));
        assertTrue(links.existsById("employee-c"));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"ADMIN", "SUPER_ADMIN"})
    void associationOptionsStillReadBothDirectoriesWithAdministrativeAuthorization(String role) throws Exception {
        String bearer = auth("test-admin", role);
        when(directory.read(eq("identity-service"), eq("/api/users"), anyString()))
                .thenReturn(json.readTree("[{\"id\":1,\"username\":\"employee-a\",\"roleType\":\"EMPLOYEE\",\"roles\":[\"EMPLOYEE\"],\"enabled\":true,\"accountNonLocked\":true}]"));
        when(directory.read(eq("employee-service"), eq("/api/employees"), anyString()))
                .thenReturn(json.readTree("[{\"id\":101,\"employeeCode\":\"TEST-101\"}]"));

        mvc.perform(get("/api/leaves/associations/options").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].id").value(1))
                .andExpect(jsonPath("$.employees[0].id").value(101));

        var authorization = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(directory).read(eq("identity-service"), eq("/api/users"), authorization.capture());
        verify(directory).read(eq("employee-service"), eq("/api/employees"), authorization.capture());
        assertTrue(authorization.getAllValues().stream().allMatch(bearer::equals),
                "Both directory calls must preserve the administrator authorization");
    }

    @Test void associationRejectsClientAccountOrAlreadyLinkedEmployee() throws Exception {
        String admin = auth("admin", "ADMIN");
        when(directory.read(eq("identity-service"), eq("/api/users/3"), anyString()))
                .thenReturn(json.readTree("{\"id\":3,\"username\":\"client-c\",\"roleType\":\"CLIENT\",\"roles\":[\"CLIENT\"],\"enabled\":true,\"accountNonLocked\":true}"));
        when(directory.read(eq("employee-service"), eq("/api/employees/101"), anyString())).thenReturn(json.readTree("{\"id\":101}"));
        mvc.perform(post("/api/leaves/associations").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content("{\"userId\":3,\"employeeId\":101}")).andExpect(status().isBadRequest());
        when(directory.read(eq("identity-service"), eq("/api/users/3"), anyString()))
                .thenReturn(json.readTree("{\"id\":3,\"username\":\"employee-c\",\"roleType\":\"EMPLOYEE\",\"roles\":[\"EMPLOYEE\"],\"enabled\":true,\"accountNonLocked\":true}"));
        mvc.perform(post("/api/leaves/associations").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content("{\"userId\":3,\"employeeId\":101}")).andExpect(status().isBadRequest());
        assertFalse(links.existsById("employee-c"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"employee-a", "renamed-employee", "employee-b"})
    void sameUidRetainsOwnLeaveAccessRegardlessOfUsername(String username) throws Exception {
        String bearer = auth("1", username, "EMPLOYEE");
        mvc.perform(get("/api/leaves").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employeeId").value("101"));
        mvc.perform(get("/api/leaves?size=20&keyword=101").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/leaves/" + aId).header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/leaves/" + aId + "/pdf").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
        mvc.perform(post("/api/leaves/mine").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("}", ",\"employeeId\":\"102\"}")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.employeeId").value("101"));
        mvc.perform(put("/api/leaves/mine/" + aId).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("}", ",\"employeeId\":\"102\"}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value("101"));
        mvc.perform(delete("/api/leaves/" + aId).header("Authorization", bearer)).andExpect(status().isNoContent());
        assertEquals("employee-a", links.findByUserId(1L).orElseThrow().getUsername());
        assertEquals("101", links.findByUserId(1L).orElseThrow().getEmployeeId());
    }

    @Test void anotherUidCannotImpersonateOwnerByUsernameHeadersOrRequestFields() throws Exception {
        String bearer = auth("2", "employee-a", "EMPLOYEE");
        mvc.perform(get("/api/leaves").header("Authorization", bearer).header("X-User-Id", "1")
                .header("X-Username", "employee-a").header("X-Roles", "SUPER_ADMIN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/leaves?size=20&keyword=101&employeeId=101").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        for (String suffix : List.of("", "/pdf"))
            mvc.perform(get("/api/leaves/" + aId + suffix).header("Authorization", bearer))
                    .andExpect(status().isForbidden());
        mvc.perform(put("/api/leaves/mine/" + aId).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content(body())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/leaves/" + aId).header("Authorization", bearer)).andExpect(status().isForbidden());
        mvc.perform(post("/api/leaves/mine").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content(body().replace("}", ",\"employeeId\":\"101\"}")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.employeeId").value("102"));
        assertEquals("101", leaves.findById(aId).orElseThrow().getEmployeeId());
    }

    @Test void recreatedUsernameWithNewUidCannotRecoverAnyPersonalLeaveOperation() throws Exception {
        assertPersonalOperationsForbidden(auth("9", "employee-a", "EMPLOYEE"));
        assertEquals(1, leaves.count());
        assertEquals(2, links.count());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"", "01", "-1", "EMP-OLD", "9223372036854775808"})
    void incoherentAssociationBlocksAllPersonalLeaveOperations(String invalidEmployeeId) throws Exception {
        var link = links.findByUserId(1L).orElseThrow();
        link.setEmployeeId(invalidEmployeeId);
        links.saveAndFlush(link);
        assertPersonalOperationsForbidden(auth("1", "employee-a", "EMPLOYEE"));
        assertEquals("101", leaves.findById(aId).orElseThrow().getEmployeeId());
        assertEquals(1, leaves.count());
    }

    private void assertPersonalOperationsForbidden(String bearer) throws Exception {
        for (String path : List.of("/api/leaves", "/api/leaves?size=20&keyword=101",
                "/api/leaves/" + aId, "/api/leaves/" + aId + "/pdf")) {
            String error = mvc.perform(get(path).header("Authorization", bearer))
                    .andExpect(status().isForbidden()).andReturn().getResponse().getContentAsString();
            assertFalse(error.contains("employee-a"));
            assertFalse(error.contains("employee-b"));
        }
        mvc.perform(post("/api/leaves/mine").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content(body())).andExpect(status().isForbidden());
        mvc.perform(put("/api/leaves/mine/" + aId).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content(body())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/leaves/" + aId).header("Authorization", bearer)).andExpect(status().isForbidden());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"ADMIN", "SUPER_ADMIN"})
    void administratorsRetainReadPdfAndDecisionWithoutEmployeeAssociation(String role) throws Exception {
        String bearer = auth("99", "signed-admin", role);
        mvc.perform(get("/api/leaves").header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/leaves/" + aId).header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/leaves/" + aId + "/pdf").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(patch("/api/leaves/" + aId + "/approve").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.validatorUsername").value("signed-admin"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {401, 503})
    void identityFailureStillBlocksPersonalListAndPdf(int remoteStatus) throws Exception {
        identityStatus = remoteStatus;
        String bearer = auth("1", "employee-a", "EMPLOYEE");
        mvc.perform(get("/api/leaves").header("Authorization", bearer)).andExpect(status().is(remoteStatus));
        mvc.perform(get("/api/leaves/" + aId + "/pdf").header("Authorization", bearer)).andExpect(status().is(remoteStatus));
        assertEquals(1, leaves.count());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"403,Leave directory access denied", "404,Directory resource not found", "502,Leave directory communication failed", "503,Leave directory unavailable"})
    void directoryErrorsKeepStatusAndReturnOnlyGenericDetails(int status, String message) throws Exception {
        when(directory.read(eq("identity-service"), eq("/api/users"), anyString()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.valueOf(status), "private internal detail"));
        mvc.perform(get("/api/leaves/associations/options").header("Authorization", auth("admin", "ADMIN")))
                .andExpect(status().is(status)).andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.message").value(message));
    }
}
