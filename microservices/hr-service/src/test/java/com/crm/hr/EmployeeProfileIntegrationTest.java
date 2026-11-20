package com.crm.hr;

import com.crm.hr.client.EmployeeProfileClient;
import com.crm.hr.dto.EmployeeProfileSummary;
import com.crm.hr.entity.LeaveEmployeeAssociation;
import com.crm.hr.exception.EmployeeProfileException;
import com.crm.hr.repository.LeaveEmployeeAssociationRepository;
import com.crm.hr.security.TestIdentitySupport;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:personal_profile;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.cloud.service-registry.auto-registration.enabled=false", "eureka.client.enabled=false"})
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class EmployeeProfileIntegrationTest extends TestIdentitySupport {
    static final String PATH = "/api/employee-profile/me";
    @Autowired MockMvc mvc;
    @Autowired LeaveEmployeeAssociationRepository links;
    @MockBean EmployeeProfileClient employee;
    @BeforeEach void prepare() {
        links.deleteAll();
        when(employee.read(anyString())).thenAnswer(call -> new EmployeeProfileSummary(call.getArgument(0), "Test", "Professional", "Developer", "7", LocalDate.of(2026, 1, 15)));
    }
    void seed(long uid, String username, String id) { var link = new LeaveEmployeeAssociation(); link.setUserId(uid); link.setUsername(username); link.setEmployeeId(id); links.saveAndFlush(link); }
    String auth(long uid, String name, String role) { return "Bearer " + signed(name, true, List.of(role), Long.toString(uid), Instant.now().plusSeconds(600)); }
    @Test void twoEmployeesResolveOnlyTheirOwnUidAssociationDespiteTargetHeaders() throws Exception {
        seed(42, "original-a", "103"); seed(43, "original-b", "104");
        mvc.perform(get(PATH).header("Authorization", auth(42, "signed-a", "EMPLOYEE")).header("X-User-Id", "43").header("X-Employee-Id", "104"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.employeeId").value("103")).andExpect(jsonPath("$.salary").doesNotExist())
                .andExpect(jsonPath("$.address").doesNotExist()).andExpect(jsonPath("$.dateOfBirth").doesNotExist());
        mvc.perform(get(PATH).header("Authorization", auth(43, "signed-b", "EMPLOYEE"))).andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value("104"));
        verify(employee).read("103"); verify(employee).read("104"); verifyNoMoreInteractions(employee);
    }
    @Test void changedUsernameRetainsUidOwnershipButReusedUsernameDoesNotTransferIt() throws Exception {
        seed(42, "old-username", "103");
        mvc.perform(get(PATH).header("Authorization", auth(42, "new-username", "EMPLOYEE"))).andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value("103"));
        mvc.perform(get(PATH).header("Authorization", auth(43, "old-username", "EMPLOYEE"))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_ASSOCIATED"));
        verify(employee, times(1)).read("103"); verifyNoMoreInteractions(employee);
    }
    @Test void absentAssociationIsExplicitAndDoesNotCallEmployee() throws Exception {
        mvc.perform(get(PATH).header("Authorization", auth(42, "employee", "EMPLOYEE"))).andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store")).andExpect(jsonPath("$.code").value("PROFILE_NOT_ASSOCIATED"));
        verifyNoInteractions(employee);
    }
    @Test void malformedHistoricalAssociationFailsClosed() throws Exception {
        seed(42, "employee", "01");
        mvc.perform(get(PATH).header("Authorization", auth(42, "employee", "EMPLOYEE"))).andExpect(status().isBadGateway()); verifyNoInteractions(employee);
    }
    @ParameterizedTest @ValueSource(strings = {"ADMIN", "SUPER_ADMIN", "CLIENT"})
    void nonEmployeeRolesCannotReadPersonalProfile(String role) throws Exception {
        mvc.perform(get(PATH).header("Authorization", auth(42, "account", role))).andExpect(status().isForbidden()); verifyNoInteractions(employee);
    }
    @Test void missingOrLegacyAuthenticationIsRefused() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header("Authorization", "Bearer " + signed("legacy", true, List.of("EMPLOYEE"), null, Instant.now().plusSeconds(600))))
                .andExpect(status().isUnauthorized()); verifyNoInteractions(employee);
    }
    @ParameterizedTest @ValueSource(strings = {"userId=43", "employeeId=104", "unknown=x"})
    void targetParametersAreNeverAccepted(String query) throws Exception {
        mvc.perform(get(PATH + "?" + query).header("Authorization", auth(42, "employee", "EMPLOYEE")))
                .andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control", "no-store")); verifyNoInteractions(employee);
    }
    @ParameterizedTest @ValueSource(ints = {404, 502, 503})
    void dependencyFailuresAreGenericAndNeverReturnAProfile(int code) throws Exception {
        seed(42, "employee", "103");
        String errorCode = code == 404 ? "PROFILE_NOT_FOUND" : code == 502 ? "PROFILE_INVALID_RESPONSE" : "PROFILE_SERVICE_UNAVAILABLE";
        when(employee.read("103")).thenThrow(new EmployeeProfileException(code, errorCode));
        mvc.perform(get(PATH).header("Authorization", auth(42, "employee", "EMPLOYEE"))).andExpect(status().is(code))
                .andExpect(header().string("Cache-Control", "no-store")).andExpect(jsonPath("$.code").value(errorCode))
                .andExpect(jsonPath("$.employeeId").doesNotExist());
    }
    @Test void profileNamespaceCannotUseTheAuthenticatedCatchAllForWrites() throws Exception {
        mvc.perform(post(PATH).header("Authorization", auth(42, "employee", "EMPLOYEE"))).andExpect(status().isForbidden()); verifyNoInteractions(employee);
    }
}
