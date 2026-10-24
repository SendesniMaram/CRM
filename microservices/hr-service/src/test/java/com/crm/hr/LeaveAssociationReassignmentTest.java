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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.cloud.service-registry.auto-registration.enabled=false", "eureka.client.enabled=false"})
@AutoConfigureMockMvc
@Transactional
class LeaveAssociationReassignmentTest extends com.crm.hr.security.TestIdentitySupport {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;
    @Autowired LeaveRepository leaves;
    @Autowired LeaveEmployeeAssociationRepository links;
    @MockBean LeaveDirectoryClient directory;

    @BeforeEach void setup() throws Exception {
        leaves.deleteAll();
        links.deleteAll();
        link("employee-b", 3L, "1");
        when(directory.read(eq("identity-service"), eq("/api/users/3"), anyString()))
                .thenReturn(json.readTree("{\"id\":3,\"username\":\"employee-b\",\"roleType\":\"EMPLOYEE\",\"roles\":[\"EMPLOYEE\"],\"enabled\":true,\"accountNonLocked\":true}"));
        when(directory.read(eq("employee-service"), eq("/api/employees/2"), anyString()))
                .thenReturn(json.readTree("{\"id\":2}"));
    }

    void link(String username, Long userId, String employeeId) {
        var link = new LeaveEmployeeAssociation();
        link.setUsername(username); link.setUserId(userId); link.setEmployeeId(employeeId);
        links.saveAndFlush(link);
    }
    String auth(String role) { return "Bearer " + com.crm.hr.security.TestIdentitySupport.token("test-admin", "", true, List.of(role)); }
    void unchanged() {
        assertEquals("1", links.findById("employee-b").orElseThrow().getEmployeeId());
    }

    @ParameterizedTest @ValueSource(strings = {"[\"ADMIN\",\"EMPLOYEE\"]", "[\"EMPLOYEE\",\"ADMIN\"]"})
    void multiRoleAccountCanBeReassignedAndListedRegardlessOfLegacyRole(String roles) throws Exception {
        var account = json.readTree("{\"id\":3,\"username\":\"employee-b\",\"roleType\":\"ADMIN\",\"roles\":"
                + roles + ",\"enabled\":true,\"accountNonLocked\":true}");
        when(directory.read(eq("identity-service"), eq("/api/users/3"), anyString())).thenReturn(account);
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value("2"));
        links.deleteAll();
        mvc.perform(post("/api/leaves/associations").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":3,\"employeeId\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(3));
        when(directory.read(eq("identity-service"), eq("/api/users"), anyString()))
                .thenReturn(json.createArrayNode().add(account));
        when(directory.read(eq("employee-service"), eq("/api/employees"), anyString()))
                .thenReturn(json.readTree("[{\"id\":2,\"employeeCode\":\"SYNTHETIC\"}]"));
        mvc.perform(get("/api/leaves/associations/options").header("Authorization", auth("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.users[0].id").value(3));
        assertEquals(1, links.count());
    }

    @ParameterizedTest @ValueSource(strings = {"enabled", "accountNonLocked"})
    void inactiveAccountCannotBeLinkedReassignedOrOffered(String flag) throws Exception {
        var account = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(
                "{\"id\":3,\"username\":\"employee-b\",\"roles\":[\"EMPLOYEE\"],\"enabled\":true,\"accountNonLocked\":true}");
        account.put(flag, false);
        when(directory.read(eq("identity-service"), eq("/api/users/3"), anyString())).thenReturn(account);
        for (var request : List.of(put("/api/leaves/associations/3").content("{\"employeeId\":2}"),
                post("/api/leaves/associations").content("{\"userId\":3,\"employeeId\":2}"))) {
            mvc.perform(request.header("Authorization", auth("ADMIN")).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
        when(directory.read(eq("identity-service"), eq("/api/users"), anyString()))
                .thenReturn(json.createArrayNode().add(account));
        when(directory.read(eq("employee-service"), eq("/api/employees"), anyString()))
                .thenReturn(json.createArrayNode());
        mvc.perform(get("/api/leaves/associations/options").header("Authorization", auth("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.users").isEmpty());
        unchanged();
    }

    @Test void malformedDirectoryRolesCannotChangeAssociation() throws Exception {
        when(directory.read(eq("identity-service"), eq("/api/users/3"), anyString()))
                .thenReturn(json.readTree("{\"id\":3,\"username\":\"employee-b\",\"roleType\":\"EMPLOYEE\"}"));
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().isBadGateway());
        unchanged();
    }

    @ParameterizedTest @ValueSource(ints = {401, 403, 503})
    void identityRefusalRetainsItsStatusAndDoesNotWrite(int remoteStatus) throws Exception {
        when(directory.read(eq("identity-service"), eq("/api/users/3"), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.valueOf(remoteStatus), "Synthetic refusal"));
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().is(remoteStatus));
        unchanged();
        verify(directory, never()).read(eq("employee-service"), anyString(), anyString());
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "null", "\"invalid\""})
    void malformedDirectoryCollectionIsNotReportedAsEmptyOptions(String body) throws Exception {
        when(directory.read(eq("identity-service"), eq("/api/users"), anyString())).thenReturn(json.readTree(body));
        when(directory.read(eq("employee-service"), eq("/api/employees"), anyString())).thenReturn(json.createArrayNode());
        mvc.perform(get("/api/leaves/associations/options").header("Authorization", auth("ADMIN")))
                .andExpect(status().isBadGateway());
        unchanged();
    }

    @ParameterizedTest @ValueSource(ints = {401, 403, 503})
    void secondDependencyRefusalPreventsAssociationWrite(int remoteStatus) throws Exception {
        when(directory.read(eq("employee-service"), eq("/api/employees/2"), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.valueOf(remoteStatus), "Synthetic refusal"));
        String bearer = auth("ADMIN");
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().is(remoteStatus));
        unchanged();
        verify(directory).read("identity-service", "/api/users/3", bearer);
        verify(directory).read("employee-service", "/api/employees/2", bearer);
        assertEquals(1, links.count());
    }
    void leave(String employeeId, LeaveRequest.LeaveStatus status) {
        var leave = new LeaveRequest();
        leave.setEmployeeId(employeeId); leave.setStatus(status);
        leave.setStartDate(LocalDate.now().plusDays(1)); leave.setEndDate(LocalDate.now().plusDays(2));
        leaves.saveAndFlush(leave);
    }

    @ParameterizedTest @ValueSource(strings = {"ADMIN", "SUPER_ADMIN"})
    void administratorsCanReassignWithoutChangingUserOrCreatingAnotherLink(String role) throws Exception {
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth(role))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("employee-b"))
                .andExpect(jsonPath("$.userId").value(3)).andExpect(jsonPath("$.employeeId").value("2"));
        assertEquals(1, links.count());
        assertEquals(0, leaves.count());
        assertFalse(links.existsByEmployeeId("1"));
        verify(directory).read(eq("identity-service"), eq("/api/users/3"), anyString());
        verify(directory).read(eq("employee-service"), eq("/api/employees/2"), anyString());
    }

    @ParameterizedTest @ValueSource(strings = {"EMPLOYEE", "CLIENT"})
    void otherRolesAreForbiddenBeforeDirectoryAccess(String role) throws Exception {
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth(role))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().isForbidden());
        unchanged(); verifyNoInteractions(directory);
    }
    @Test void unauthenticatedIsRejected() throws Exception {
        mvc.perform(put("/api/leaves/associations/3").contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":2}")).andExpect(status().isUnauthorized());
        unchanged(); verifyNoInteractions(directory);
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "{\"employeeId\":0}", "{\"employeeId\":-1}"})
    void invalidPayloadIsBadRequest(String body) throws Exception {
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        unchanged(); verifyNoInteractions(directory);
    }
    @Test void invalidUserIdIsBadRequest() throws Exception {
        mvc.perform(put("/api/leaves/associations/0").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().isBadRequest());
        unchanged(); verifyNoInteractions(directory);
    }
    @Test void missingAssociationIsNotFoundAndDoesNotCreateOne() throws Exception {
        mvc.perform(put("/api/leaves/associations/2").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().isNotFound());
        unchanged(); assertEquals(1, links.count()); verifyNoInteractions(directory);
    }
    @Test void occupiedEmployeeConflictsAndBothLinksArePreserved() throws Exception {
        link("employee-a", 2L, "2");
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().isConflict());
        unchanged(); assertEquals("2", links.findById("employee-a").orElseThrow().getEmployeeId());
    }
    @ParameterizedTest @EnumSource(LeaveRequest.LeaveStatus.class)
    void oldEmployeeLeaveBlocksAllStatuses(LeaveRequest.LeaveStatus status) throws Exception {
        leave("1", status);
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().isConflict());
        unchanged(); assertEquals(1, leaves.count());
    }
    @ParameterizedTest @EnumSource(LeaveRequest.LeaveStatus.class)
    void newEmployeeLeaveBlocksAllStatuses(LeaveRequest.LeaveStatus status) throws Exception {
        leave("2", status);
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().isConflict());
        unchanged(); assertEquals(1, leaves.count());
    }
    @ParameterizedTest @ValueSource(strings = {
            "{\"id\":3,\"username\":\"employee-b\",\"roleType\":\"CLIENT\",\"roles\":[\"CLIENT\"],\"enabled\":true,\"accountNonLocked\":true}",
            "{\"id\":99,\"username\":\"employee-b\",\"roleType\":\"EMPLOYEE\",\"roles\":[\"EMPLOYEE\"],\"enabled\":true,\"accountNonLocked\":true}",
            "{\"id\":3,\"username\":\"\",\"roleType\":\"EMPLOYEE\",\"roles\":[\"EMPLOYEE\"],\"enabled\":true,\"accountNonLocked\":true}"})
    void invalidIdentityReferenceIsRejected(String response) throws Exception {
        when(directory.read(eq("identity-service"), eq("/api/users/3"), anyString())).thenReturn(json.readTree(response));
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().isBadRequest());
        unchanged();
    }
    @Test void mismatchedEmployeeReferenceIsRejected() throws Exception {
        when(directory.read(eq("employee-service"), eq("/api/employees/2"), anyString())).thenReturn(json.readTree("{\"id\":99}"));
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().isBadRequest());
        unchanged();
    }
    @Test void changedUsernameConflictsRatherThanChangingAccount() throws Exception {
        when(directory.read(eq("identity-service"), eq("/api/users/3"), anyString()))
                .thenReturn(json.readTree("{\"id\":3,\"username\":\"different\",\"roleType\":\"EMPLOYEE\",\"roles\":[\"EMPLOYEE\"],\"enabled\":true,\"accountNonLocked\":true}"));
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().isConflict());
        unchanged(); assertFalse(links.existsById("different"));
    }
    @ParameterizedTest @ValueSource(strings = {"identity-service", "employee-service"})
    void missingRemoteResourceIsNotFound(String service) throws Exception {
        when(directory.read(eq(service), anyString(), anyString())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().isNotFound());
        unchanged();
    }
    @Test void remoteAccessDeniedFailsClosed() throws Exception {
        when(directory.read(eq("identity-service"), anyString(), anyString())).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":2}"))
                .andExpect(status().isForbidden());
        unchanged();
    }
    @Test void repeatingSameAssignmentIsIdempotentWithoutLeaves() throws Exception {
        when(directory.read(eq("employee-service"), eq("/api/employees/1"), anyString())).thenReturn(json.readTree("{\"id\":1}"));
        mvc.perform(put("/api/leaves/associations/3").header("Authorization", auth("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"employeeId\":1}"))
                .andExpect(status().isOk());
        unchanged(); assertEquals(1, links.count());
    }
}
