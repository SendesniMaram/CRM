

package com.crm.hr;

import com.crm.hr.dto.HrRequest;
import com.crm.hr.dto.HrResponse;
import com.crm.hr.repository.HrRepository;
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

import java.time.LocalDateTime;
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
class HrServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HrRepository hrRepository;

    private HrRequest validRequest;

    @BeforeEach
    void setUp() {
        hrRepository.deleteAll();
        hrRepository.flush();

        validRequest = new HrRequest();
        validRequest.setEmployeeId("EMP001");
        validRequest.setContractType("CDI");
        validRequest.setHireDate(LocalDateTime.of(2024, 1, 15, 0, 0));
        validRequest.setJobTitle("Software Engineer");
        validRequest.setSalaryGrade("G7");
        validRequest.setManager("John Doe");
        validRequest.setWorkLocation("Paris");
        validRequest.setEmploymentStatus("ACTIVE");
    }

    @Test
    void createHrRecord_ShouldReturn201AndPersist() throws Exception {
        String responseJson = mockMvc.perform(post("/api/hr-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.contractType").value("CDI"))
                .andExpect(jsonPath("$.hireDate").isNotEmpty())
                .andExpect(jsonPath("$.jobTitle").value("Software Engineer"))
                .andExpect(jsonPath("$.salaryGrade").value("G7"))
                .andExpect(jsonPath("$.manager").value("John Doe"))
                .andExpect(jsonPath("$.workLocation").value("Paris"))
                .andExpect(jsonPath("$.employmentStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        HrResponse response = objectMapper.readValue(responseJson, HrResponse.class);
        assertNotNull(response.getId());

        // Flush to ensure persistence in H2
        hrRepository.flush();

        // Verify persistence
        assertTrue(hrRepository.existsByEmployeeId("EMP001"));
    }

    @Test
    void createHrRecord_WithDuplicateEmployeeId_ShouldReturn400() throws Exception {
        // First creation succeeds
        mockMvc.perform(post("/api/hr-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        // Flush to make the first insert visible
        hrRepository.flush();

        // Second creation with same employeeId should fail
        mockMvc.perform(post("/api/hr-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Employee ID already exists")))
                .andExpect(jsonPath("$.path").value("/api/hr-records"));
    }

    @Test
    void createHrRecord_WithValidationErrors_ShouldReturn400() throws Exception {
        HrRequest invalidRequest = new HrRequest();
        // Missing employeeId (NotBlank)

        mockMvc.perform(post("/api/hr-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
                .andExpect(jsonPath("$.path").value("/api/hr-records"));
    }

    @Test
    void getAllHrRecords_ShouldReturnList() throws Exception {
        // Create two records
        mockMvc.perform(post("/api/hr-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        HrRequest secondRequest = new HrRequest();
        secondRequest.setEmployeeId("EMP002");
        secondRequest.setContractType("CDD");
        secondRequest.setJobTitle("DevOps Engineer");
        secondRequest.setSalaryGrade("G6");
        secondRequest.setEmploymentStatus("ACTIVE");

        mockMvc.perform(post("/api/hr-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        hrRepository.flush();

        String responseJson = mockMvc.perform(get("/api/hr-records"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<HrResponse> records = objectMapper.readValue(
                responseJson, new TypeReference<List<HrResponse>>() {});
        assertEquals(2, records.size());
    }

    @Test
    void getAllHrRecords_WithPagination_ShouldReturnPage() throws Exception {
        // Create a record first
        mockMvc.perform(post("/api/hr-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        hrRepository.flush();

        mockMvc.perform(get("/api/hr-records")
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
    void getAllHrRecords_WithKeywordSearch_ShouldReturnFilteredResults() throws Exception {
        // Create record matching keyword
        mockMvc.perform(post("/api/hr-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        HrRequest secondRequest = new HrRequest();
        secondRequest.setEmployeeId("EMP003");
        secondRequest.setJobTitle("Data Analyst");
        secondRequest.setEmploymentStatus("ACTIVE");

        mockMvc.perform(post("/api/hr-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        hrRepository.flush();

        // Search by keyword "EMP001"
        mockMvc.perform(get("/api/hr-records")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "EMP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getHrRecordById_ShouldReturnRecord() throws Exception {
        String createJson = mockMvc.perform(post("/api/hr-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        HrResponse created = objectMapper.readValue(createJson, HrResponse.class);

        mockMvc.perform(get("/api/hr-records/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.contractType").value("CDI"))
                .andExpect(jsonPath("$.jobTitle").value("Software Engineer"))
                .andExpect(jsonPath("$.salaryGrade").value("G7"))
                .andExpect(jsonPath("$.manager").value("John Doe"))
                .andExpect(jsonPath("$.workLocation").value("Paris"))
                .andExpect(jsonPath("$.employmentStatus").value("ACTIVE"));
    }

    @Test
    void getHrRecordById_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/hr-records/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("HR record not found")))
                .andExpect(jsonPath("$.path").value("/api/hr-records/99999"));
    }

    @Test
    void updateHrRecord_ShouldReturnUpdatedRecord() throws Exception {
        String createJson = mockMvc.perform(post("/api/hr-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        HrResponse created = objectMapper.readValue(createJson, HrResponse.class);

        HrRequest updateRequest = new HrRequest();
        updateRequest.setEmployeeId("EMP001"); // Same ID
        updateRequest.setContractType("CDI Updated");
        updateRequest.setHireDate(LocalDateTime.of(2024, 6, 1, 0, 0));
        updateRequest.setJobTitle("Senior Software Engineer");
        updateRequest.setSalaryGrade("G8");
        updateRequest.setManager("Jane Smith");
        updateRequest.setWorkLocation("Lyon");
        updateRequest.setEmploymentStatus("ACTIVE");

        mockMvc.perform(put("/api/hr-records/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.contractType").value("CDI Updated"))
                .andExpect(jsonPath("$.jobTitle").value("Senior Software Engineer"))
                .andExpect(jsonPath("$.salaryGrade").value("G8"))
                .andExpect(jsonPath("$.manager").value("Jane Smith"))
                .andExpect(jsonPath("$.workLocation").value("Lyon"));

        hrRepository.flush();

        // Verify persistence
        assertTrue(hrRepository.existsByEmployeeId("EMP001"));
    }

    @Test
    void updateHrRecord_WithNonExistentId_ShouldReturn404() throws Exception {
        HrRequest updateRequest = new HrRequest();
        updateRequest.setEmployeeId("EMP999");
        updateRequest.setJobTitle("N/A");
        updateRequest.setEmploymentStatus("INACTIVE");

        mockMvc.perform(put("/api/hr-records/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("HR record not found")))
                .andExpect(jsonPath("$.path").value("/api/hr-records/99999"));
    }

    @Test
    void deleteHrRecord_ShouldReturn204AndRemoveFromDatabase() throws Exception {
        String createJson = mockMvc.perform(post("/api/hr-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        HrResponse created = objectMapper.readValue(createJson, HrResponse.class);

        mockMvc.perform(delete("/api/hr-records/{id}", created.getId()))
                .andExpect(status().isNoContent());

        // Verify deletion from H2
        assertFalse(hrRepository.findById(created.getId()).isPresent());
    }

    @Test
    void deleteHrRecord_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/hr-records/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("HR record not found")))
                .andExpect(jsonPath("$.path").value("/api/hr-records/99999"));
    }
}
