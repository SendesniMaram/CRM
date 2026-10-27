package com.crm.hr;

import com.crm.hr.config.SecurityConfig;
import com.crm.hr.controller.EmployeeContractController;
import com.crm.hr.controller.HrWorkflowController;
import com.crm.hr.service.EmployeeContractService;
import com.crm.hr.service.HrWorkflowService;
import com.crm.security.JwtSecurityAutoConfiguration;
import com.crm.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers={EmployeeContractController.class,HrWorkflowController.class})
@Import({SecurityConfig.class, JwtSecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters=true)
class HrNewEndpointSecurityTest extends com.crm.hr.security.TestIdentitySupport {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean EmployeeContractService contracts;
    @MockBean HrWorkflowService workflows;

    @Test void newHrListsAreRestrictedToAdministrators() throws Exception {
        when(contracts.page(anyInt(),anyInt(),any(),any())).thenReturn(new PageImpl<>(List.of()));
        when(workflows.evaluations(anyInt(),anyInt(),any(),any())).thenReturn(new PageImpl<>(List.of()));
        when(workflows.offers(anyInt(),anyInt(),any())).thenReturn(new PageImpl<>(List.of()));
        when(workflows.applications(anyInt(),anyInt(),any(),any())).thenReturn(new PageImpl<>(List.of()));
        for(String path:List.of("/api/contracts?page=0","/api/evaluations?page=0","/api/job-offers?page=0","/api/applications?page=0")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            for(String role:List.of("EMPLOYEE","CLIENT"))
                mvc.perform(get(path).header("Authorization","Bearer "+token("security-test-user","",true,List.of(role)))).andExpect(status().isForbidden());
            for(String role:List.of("ADMIN","SUPER_ADMIN"))
                mvc.perform(get(path).header("Authorization","Bearer "+token("security-test-user","",true,List.of(role)))).andExpect(status().isOk());
        }
        verify(contracts,times(2)).page(anyInt(),anyInt(),any(),any());
        verify(workflows,times(2)).evaluations(anyInt(),anyInt(),any(),any());
        verify(workflows,times(2)).offers(anyInt(),anyInt(),any());
        verify(workflows,times(2)).applications(anyInt(),anyInt(),any(),any());
    }
}
