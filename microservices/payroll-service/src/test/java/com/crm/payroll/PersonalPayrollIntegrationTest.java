package com.crm.payroll;

import com.crm.payroll.client.PayrollEmployeeDirectoryClient;
import com.crm.payroll.entity.Payroll;
import com.crm.payroll.repository.PayrollRepository;
import com.crm.payroll.security.TestIdentitySupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:personal_payroll;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.cloud.service-registry.auto-registration.enabled=false", "eureka.client.enabled=false"})
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class PersonalPayrollIntegrationTest extends TestIdentitySupport {
    private static final String PATH = "/api/payrolls/me";
    private static final String EMPLOYEE = "Bearer " + token("employee-42", "", true, List.of("EMPLOYEE"));

    @Autowired MockMvc mvc;
    @Autowired PayrollRepository payrolls;
    @MockBean PayrollEmployeeDirectoryClient directory;

    private Long ownOctober;
    private Long ownSeptember;
    private Long otherOctober;

    @BeforeEach
    void prepare() {
        payrolls.deleteAll();
        when(directory.ownEmployeeId(anyString(), eq(42L))).thenReturn("101");
        ownOctober = payrolls.saveAndFlush(payroll("101", "2026-10", "TND")).getId();
        ownSeptember = payrolls.saveAndFlush(payroll("101", "2026-09", "TND")).getId();
        otherOctober = payrolls.saveAndFlush(payroll("102", "2026-10", "EUR")).getId();
    }

    @Test
    void listIsBoundToTheAssociatedEmployeeAndFiltersByPeriod() throws Exception {
        mvc.perform(get(PATH).header("Authorization", EMPLOYEE).param("period", "2026-10"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ownOctober))
                .andExpect(jsonPath("$.content[0].employeeId").value("101"));
        verify(directory).ownEmployeeId(EMPLOYEE, 42L);
    }

    @Test
    void yearFilterReturnsOnlyOwnedRecords() throws Exception {
        mvc.perform(get(PATH).header("Authorization", EMPLOYEE).param("period", "2026"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].employeeId").value("101"))
                .andExpect(jsonPath("$.content[1].employeeId").value("101"));
    }

    @Test
    void detailsAndPdfCannotCrossEmployeeOwnership() throws Exception {
        mvc.perform(get(PATH + "/" + ownSeptember).header("Authorization", EMPLOYEE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ownSeptember));
        mvc.perform(get(PATH + "/" + otherOctober).header("Authorization", EMPLOYEE))
                .andExpect(status().isNotFound());
        mvc.perform(get(PATH + "/" + ownOctober + "/pdf").header("Authorization", EMPLOYEE))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("bulletin-2026-10.pdf")))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getContentAsByteArray().length > 0));
        mvc.perform(get(PATH + "/" + otherOctober + "/pdf").header("Authorization", EMPLOYEE))
                .andExpect(status().isNotFound());
    }

    @Test
    void employeeCannotSelectAnotherEmployeeOrUseUnknownFilters() throws Exception {
        mvc.perform(get(PATH).header("Authorization", EMPLOYEE).param("employeeId", "102"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(PATH).header("Authorization", EMPLOYEE).param("period", "2026-99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyEmployeesCanUsePersonalPayrollEndpoints() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header("Authorization", "Bearer " + token("client", "", true, List.of("CLIENT"))))
                .andExpect(status().isForbidden());
        mvc.perform(get(PATH).header("Authorization", "Bearer " + token("admin", "", true, List.of("ADMIN"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/payrolls").header("Authorization", EMPLOYEE)).andExpect(status().isForbidden());
    }

    private Payroll payroll(String employeeId, String period, String currency) {
        Payroll row = new Payroll();
        row.setEmployeeId(employeeId); row.setPayPeriod(period); row.setCurrency(currency);
        row.setBaseSalary(new BigDecimal("2000.00")); row.setBonuses(new BigDecimal("100.00"));
        row.setDeductions(new BigDecimal("50.00")); row.setTaxAmount(new BigDecimal("250.00"));
        row.setNetSalary(new BigDecimal("1800.00")); row.setPaymentDate(LocalDate.of(2026, 10, 31));
        row.setStatus("PAID");
        return row;
    }
}
