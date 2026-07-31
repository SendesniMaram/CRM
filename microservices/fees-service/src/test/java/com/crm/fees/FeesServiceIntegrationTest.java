package com.crm.fees;

import com.crm.fees.dto.FeeRequest;
import com.crm.fees.dto.FeeResponse;
import com.crm.fees.repository.FeeRepository;
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
class FeesServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeeRepository feeRepository;

    private FeeRequest validRequest;

    @BeforeEach
    void setUp() {
        feeRepository.deleteAll();
        feeRepository.flush();

        validRequest = new FeeRequest();
        validRequest.setEmployeeId("EMP001");
        validRequest.setCustomerId("CUST001");
        validRequest.setServiceName("Consulting");
        validRequest.setDescription("IT consulting services");
        validRequest.setHourlyRate(new BigDecimal("150.00"));
        validRequest.setWorkedHours(new BigDecimal("10"));
        validRequest.setInvoiceStatus("PENDING");
        validRequest.setPaymentStatus("UNPAID");
    }

    @Test
    void createFee_ShouldReturn201AndPersist() throws Exception {
        String responseJson = mockMvc.perform(post("/api/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.customerId").value("CUST001"))
                .andExpect(jsonPath("$.serviceName").value("Consulting"))
                .andExpect(jsonPath("$.hourlyRate").value(150.00))
                .andExpect(jsonPath("$.amount").value(1500.00))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FeeResponse response = objectMapper.readValue(responseJson, FeeResponse.class);
        assertNotNull(response.getId());

        feeRepository.flush();
        assertTrue(feeRepository.existsByEmployeeId("EMP001"));
    }

    @Test
    void createFee_WithValidationErrors_ShouldReturn400() throws Exception {
        FeeRequest invalidRequest = new FeeRequest();

        mockMvc.perform(post("/api/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
                .andExpect(jsonPath("$.path").value("/api/fees"));
    }

    @Test
    void getAllFees_ShouldReturnList() throws Exception {
        mockMvc.perform(post("/api/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        FeeRequest secondRequest = new FeeRequest();
        secondRequest.setEmployeeId("EMP002");
        secondRequest.setCustomerId("CUST002");
        secondRequest.setServiceName("Development");
        secondRequest.setHourlyRate(new BigDecimal("200.00"));
        secondRequest.setWorkedHours(new BigDecimal("20"));
        secondRequest.setInvoiceStatus("PENDING");
        secondRequest.setPaymentStatus("UNPAID");

        mockMvc.perform(post("/api/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        feeRepository.flush();

        String responseJson = mockMvc.perform(get("/api/fees"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<FeeResponse> fees = objectMapper.readValue(
                responseJson, new TypeReference<List<FeeResponse>>() {});
        assertEquals(2, fees.size());
    }

    @Test
    void getAllFees_WithPagination_ShouldReturnPage() throws Exception {
        mockMvc.perform(post("/api/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        feeRepository.flush();

        mockMvc.perform(get("/api/fees")
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
    void getAllFees_WithKeywordSearch_ShouldReturnFilteredResults() throws Exception {
        mockMvc.perform(post("/api/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        FeeRequest secondRequest = new FeeRequest();
        secondRequest.setEmployeeId("EMP003");
        secondRequest.setCustomerId("CUST003");
        secondRequest.setServiceName("Design");
        secondRequest.setHourlyRate(new BigDecimal("100.00"));
        secondRequest.setWorkedHours(new BigDecimal("5"));
        secondRequest.setInvoiceStatus("PENDING");
        secondRequest.setPaymentStatus("UNPAID");

        mockMvc.perform(post("/api/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        feeRepository.flush();

        mockMvc.perform(get("/api/fees")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "EMP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getFeeById_ShouldReturnFee() throws Exception {
        String createJson = mockMvc.perform(post("/api/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FeeResponse created = objectMapper.readValue(createJson, FeeResponse.class);

        mockMvc.perform(get("/api/fees/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.customerId").value("CUST001"))
                .andExpect(jsonPath("$.serviceName").value("Consulting"))
                .andExpect(jsonPath("$.amount").value(1500.00));
    }

    @Test
    void getFeeById_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/fees/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Fee not found")))
                .andExpect(jsonPath("$.path").value("/api/fees/99999"));
    }

    @Test
    void updateFee_ShouldReturnUpdatedFee() throws Exception {
        String createJson = mockMvc.perform(post("/api/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FeeResponse created = objectMapper.readValue(createJson, FeeResponse.class);

        FeeRequest updateRequest = new FeeRequest();
        updateRequest.setEmployeeId("EMP001");
        updateRequest.setCustomerId("CUST001");
        updateRequest.setServiceName("Consulting Updated");
        updateRequest.setHourlyRate(new BigDecimal("200.00"));
        updateRequest.setWorkedHours(new BigDecimal("15"));
        updateRequest.setInvoiceStatus("PAID");
        updateRequest.setPaymentStatus("PAID");

        mockMvc.perform(put("/api/fees/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.serviceName").value("Consulting Updated"))
                .andExpect(jsonPath("$.amount").value(3000.00));
    }

    @Test
    void updateFee_WithNonExistentId_ShouldReturn404() throws Exception {
        FeeRequest updateRequest = new FeeRequest();
        updateRequest.setEmployeeId("EMP999");
        updateRequest.setCustomerId("CUST999");
        updateRequest.setServiceName("N/A");
        updateRequest.setHourlyRate(new BigDecimal("100.00"));
        updateRequest.setWorkedHours(new BigDecimal("5"));
        updateRequest.setInvoiceStatus("PENDING");
        updateRequest.setPaymentStatus("UNPAID");

        mockMvc.perform(put("/api/fees/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Fee not found")))
                .andExpect(jsonPath("$.path").value("/api/fees/99999"));
    }

    @Test
    void deleteFee_ShouldReturn204AndRemoveFromDatabase() throws Exception {
        String createJson = mockMvc.perform(post("/api/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FeeResponse created = objectMapper.readValue(createJson, FeeResponse.class);

        mockMvc.perform(delete("/api/fees/{id}", created.getId()))
                .andExpect(status().isNoContent());

        assertFalse(feeRepository.findById(created.getId()).isPresent());
    }

    @Test
    void deleteFee_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/fees/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Fee not found")))
                .andExpect(jsonPath("$.path").value("/api/fees/99999"));
    }

    @Test
    void createFee_AmountShouldBeAutoCalculated() throws Exception {
        FeeRequest request = new FeeRequest();
        request.setEmployeeId("EMP010");
        request.setCustomerId("CUST010");
        request.setServiceName("Test Service");
        request.setHourlyRate(new BigDecimal("100.00"));
        request.setWorkedHours(new BigDecimal("8"));
        request.setInvoiceStatus("PENDING");
        request.setPaymentStatus("UNPAID");

        mockMvc.perform(post("/api/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(800.00));
    }
}
