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
class LeaveAssociationReassignmentTest {
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
                .thenReturn(json.readTree("{\"id\":3,\"username\":\"employee-b\",\"roleType\":\"EMPLOYEE\"}"));
        when(directory.read(eq("employee-service"), eq("/api/employees/2"), anyString()))
                .thenReturn(json.readTree("{\"id\":2}"));
    }

    void link(String username, Long userId, String employeeId) {
        var link = new LeaveEmployeeAssociation();
        link.setUsername(username); link.setUserId(userId); link.setEmployeeId(employeeId);
        links.saveAndFlush(link);
    }
    String auth(String role) { return "Bearer " + jwt.generateToken("test-admin", "", true, List.of(role)); }
    void unchanged() {
        assertEquals("1", links.findById("employee-b").orElseThrow().getEmployeeId());
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
            "{\"id\":3,\"username\":\"employee-b\",\"roleType\":\"CLIENT\"}",
            "{\"id\":99,\"username\":\"employee-b\",\"roleType\":\"EMPLOYEE\"}",
            "{\"id\":3,\"username\":\"\",\"roleType\":\"EMPLOYEE\"}"})
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
                .thenReturn(json.readTree("{\"id\":3,\"username\":\"different\",\"roleType\":\"EMPLOYEE\"}"));
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
