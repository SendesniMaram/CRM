package com.crm.payroll;

import com.crm.payroll.dto.PayrollRequest;
import com.crm.payroll.dto.PayrollResponse;
import com.crm.payroll.repository.PayrollRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        properties = {
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "eureka.client.enabled=false"
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PayrollServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PayrollRepository payrollRepository;

    private PayrollRequest validRequest;

    @BeforeEach
    void setUp() {
        payrollRepository.deleteAll();
        payrollRepository.flush();

        validRequest = new PayrollRequest();
        validRequest.setEmployeeId("EMP001");
        validRequest.setBaseSalary(new BigDecimal("5000.00"));
        validRequest.setBonuses(new BigDecimal("500.00"));
        validRequest.setDeductions(new BigDecimal("200.00"));
        validRequest.setTaxAmount(new BigDecimal("800.00"));
        validRequest.setPayPeriod("2024-01");
        validRequest.setCurrency("EUR");
        validRequest.setStatus("PAID");
    }

    @Test
    void createPayroll_ShouldReturn201AndCalculateNetSalary() throws Exception {
        String responseJson = mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.baseSalary").value(5000.00))
                .andExpect(jsonPath("$.bonuses").value(500.00))
                .andExpect(jsonPath("$.deductions").value(200.00))
                .andExpect(jsonPath("$.taxAmount").value(800.00))
                .andExpect(jsonPath("$.netSalary").value(4500.00))
                .andExpect(jsonPath("$.payPeriod").value("2024-01"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PayrollResponse response = objectMapper.readValue(responseJson, PayrollResponse.class);
        assertNotNull(response.getId());

        // Verify net salary calculation: 5000 + 500 - 200 - 800 = 4500
        assertEquals(0, new BigDecimal("4500.00").compareTo(response.getNetSalary()));

        payrollRepository.flush();

        // Verify persistence
        assertTrue(payrollRepository.findById(response.getId()).isPresent());
    }

    @Test
    void createPayroll_WithValidationErrors_ShouldReturn400() throws Exception {
        PayrollRequest invalidRequest = new PayrollRequest();
        // Missing employeeId (NotBlank) and baseSalary (NotNull)

        mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
                .andExpect(jsonPath("$.path").value("/api/payrolls"));
    }

    @Test
    void getAllPayrolls_ShouldReturnList() throws Exception {
        // Create two records
        mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        PayrollRequest secondRequest = new PayrollRequest();
        secondRequest.setEmployeeId("EMP002");
        secondRequest.setBaseSalary(new BigDecimal("6000.00"));
        secondRequest.setBonuses(new BigDecimal("300.00"));
        secondRequest.setDeductions(new BigDecimal("150.00"));
        secondRequest.setTaxAmount(new BigDecimal("900.00"));
        secondRequest.setPayPeriod("2024-02");
        secondRequest.setCurrency("EUR");
        secondRequest.setStatus("PENDING");

        mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        payrollRepository.flush();

        String responseJson = mockMvc.perform(get("/api/payrolls"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<PayrollResponse> payrolls = objectMapper.readValue(
                responseJson, new TypeReference<List<PayrollResponse>>() {});
        assertEquals(2, payrolls.size());
    }

    @Test
    void getAllPayrolls_WithPagination_ShouldReturnPage() throws Exception {
        // Create a record first
        mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        payrollRepository.flush();

        mockMvc.perform(get("/api/payrolls")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "id")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void getAllPayrolls_WithKeywordSearch_ShouldReturnFilteredResults() throws Exception {
        // Create record matching keyword
        mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        PayrollRequest secondRequest = new PayrollRequest();
        secondRequest.setEmployeeId("EMP003");
        secondRequest.setBaseSalary(new BigDecimal("4000.00"));
        secondRequest.setPayPeriod("2024-03");
        secondRequest.setCurrency("USD");
        secondRequest.setStatus("PENDING");

        mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        payrollRepository.flush();

        // Search by keyword "EMP001"
        mockMvc.perform(get("/api/payrolls")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "EMP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getPayrollById_ShouldReturnRecord() throws Exception {
        String createJson = mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PayrollResponse created = objectMapper.readValue(createJson, PayrollResponse.class);

        mockMvc.perform(get("/api/payrolls/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.baseSalary").value(5000.00))
                .andExpect(jsonPath("$.netSalary").value(4500.00))
                .andExpect(jsonPath("$.payPeriod").value("2024-01"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void getPayrollById_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/payrolls/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Payroll not found")))
                .andExpect(jsonPath("$.path").value("/api/payrolls/99999"));
    }

    @Test
    void updatePayroll_ShouldReturnUpdatedRecord() throws Exception {
        String createJson = mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PayrollResponse created = objectMapper.readValue(createJson, PayrollResponse.class);

        PayrollRequest updateRequest = new PayrollRequest();
        updateRequest.setEmployeeId("EMP001");
        updateRequest.setBaseSalary(new BigDecimal("5500.00"));
        updateRequest.setBonuses(new BigDecimal("600.00"));
        updateRequest.setDeductions(new BigDecimal("250.00"));
        updateRequest.setTaxAmount(new BigDecimal("900.00"));
        updateRequest.setPayPeriod("2024-02");
        updateRequest.setCurrency("EUR");
        updateRequest.setStatus("PAID");

        mockMvc.perform(put("/api/payrolls/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.baseSalary").value(5500.00))
                .andExpect(jsonPath("$.netSalary").value(4950.00))
                .andExpect(jsonPath("$.payPeriod").value("2024-02"))
                .andExpect(jsonPath("$.status").value("PAID"));

        payrollRepository.flush();

        // Verify persistence
        assertTrue(payrollRepository.findById(created.getId()).isPresent());
    }

    @Test
    void updatePayroll_WithNonExistentId_ShouldReturn404() throws Exception {
        PayrollRequest updateRequest = new PayrollRequest();
        updateRequest.setEmployeeId("EMP999");
        updateRequest.setBaseSalary(new BigDecimal("5000.00"));
        updateRequest.setPayPeriod("2024-01");
        updateRequest.setStatus("PAID");

        mockMvc.perform(put("/api/payrolls/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Payroll not found")))
                .andExpect(jsonPath("$.path").value("/api/payrolls/99999"));
    }

    @Test
    void deletePayroll_ShouldReturn204AndRemoveFromDatabase() throws Exception {
        String createJson = mockMvc.perform(post("/api/payrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PayrollResponse created = objectMapper.readValue(createJson, PayrollResponse.class);

        mockMvc.perform(delete("/api/payrolls/{id}", created.getId()))
                .andExpect(status().isNoContent());

        // Verify deletion from H2
        assertFalse(payrollRepository.findById(created.getId()).isPresent());
    }

    @Test
    void deletePayroll_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/payrolls/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Payroll not found")))
                .andExpect(jsonPath("$.path").value("/api/payrolls/99999"));
    }
}

