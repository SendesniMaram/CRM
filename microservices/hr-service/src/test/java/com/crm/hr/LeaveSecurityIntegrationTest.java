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
@AutoConfigureMockMvc
@Transactional
class LeaveSecurityIntegrationTest {
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
    String auth(String username, String role) { return "Bearer " + jwt.generateToken(username, "", true, List.of(role)); }
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
                .thenReturn(json.readTree("{\"id\":3,\"username\":\"employee-c\",\"roleType\":\"EMPLOYEE\"}"));
        when(directory.read(eq("employee-service"), eq("/api/employees/103"), anyString()))
                .thenReturn(json.readTree("{\"id\":103}"));
        mvc.perform(post("/api/leaves/associations").header("Authorization", auth("admin", "ADMIN")).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("employee-c")).andExpect(jsonPath("$.employeeId").value("103"));
        assertTrue(links.existsById("employee-c"));
    }
    @Test void associationRejectsClientAccountOrAlreadyLinkedEmployee() throws Exception {
        String admin = auth("admin", "ADMIN");
        when(directory.read(eq("identity-service"), eq("/api/users/3"), anyString()))
                .thenReturn(json.readTree("{\"id\":3,\"username\":\"client-c\",\"roleType\":\"CLIENT\"}"));
        when(directory.read(eq("employee-service"), eq("/api/employees/101"), anyString())).thenReturn(json.readTree("{\"id\":101}"));
        mvc.perform(post("/api/leaves/associations").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content("{\"userId\":3,\"employeeId\":101}")).andExpect(status().isBadRequest());
        when(directory.read(eq("identity-service"), eq("/api/users/3"), anyString()))
                .thenReturn(json.readTree("{\"id\":3,\"username\":\"employee-c\",\"roleType\":\"EMPLOYEE\"}"));
        mvc.perform(post("/api/leaves/associations").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content("{\"userId\":3,\"employeeId\":101}")).andExpect(status().isBadRequest());
        assertFalse(links.existsById("employee-c"));
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
