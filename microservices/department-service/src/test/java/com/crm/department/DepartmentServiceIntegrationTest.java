package com.crm.department;

import com.crm.department.dto.DepartmentRequest;
import com.crm.department.dto.DepartmentResponse;
import com.crm.department.repository.DepartmentRepository;
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
class DepartmentServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    private DepartmentRequest validRequest;

    @BeforeEach
    void setUp() {
        departmentRepository.deleteAll();

        validRequest = new DepartmentRequest();
        validRequest.setName("Engineering");
        validRequest.setDescription("Engineering department");
        validRequest.setCode("DEPT-ENG");
    }

    @Test
    void createDepartment_ShouldReturn201AndPersist() throws Exception {
        String responseJson = mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.description").value("Engineering department"))
                .andExpect(jsonPath("$.code").value("DEPT-ENG"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        DepartmentResponse response = objectMapper.readValue(responseJson, DepartmentResponse.class);
        assertNotNull(response.getId());

        // Verify persistence in H2
        assertTrue(departmentRepository.existsByName("Engineering"));
        assertTrue(departmentRepository.existsByCode("DEPT-ENG"));
    }

    @Test
    void createDepartment_WithDuplicateName_ShouldReturn400() throws Exception {
        // First creation succeeds
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        // Second creation with same name should fail
        DepartmentRequest duplicateNameRequest = new DepartmentRequest();
        duplicateNameRequest.setName("Engineering"); // Same name
        duplicateNameRequest.setCode("DEPT-OTHER");

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateNameRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Department name already exists")))
                .andExpect(jsonPath("$.path").value("/api/departments"));
    }

    @Test
    void createDepartment_WithDuplicateCode_ShouldReturn400() throws Exception {
        // First creation succeeds
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        // Second creation with same code should fail
        DepartmentRequest duplicateCodeRequest = new DepartmentRequest();
        duplicateCodeRequest.setName("Human Resources");
        duplicateCodeRequest.setCode("DEPT-ENG"); // Same code

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateCodeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Department code already exists")))
                .andExpect(jsonPath("$.path").value("/api/departments"));
    }

    @Test
    void createDepartment_WithValidationErrors_ShouldReturn400() throws Exception {
        DepartmentRequest invalidRequest = new DepartmentRequest();
        // Missing name (NotBlank)

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
                .andExpect(jsonPath("$.path").value("/api/departments"));
    }

    @Test
    void getAllDepartments_ShouldReturnList() throws Exception {
        // Create two departments
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        DepartmentRequest secondRequest = new DepartmentRequest();
        secondRequest.setName("Human Resources");
        secondRequest.setDescription("HR department");
        secondRequest.setCode("DEPT-HR");

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        String responseJson = mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<DepartmentResponse> departments = objectMapper.readValue(
                responseJson, new TypeReference<List<DepartmentResponse>>() {});
        assertEquals(2, departments.size());
    }

    @Test
    void getAllDepartments_WithPagination_ShouldReturnPage() throws Exception {
        // Create a department first
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/departments")
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
    void getAllDepartments_WithKeywordSearch_ShouldReturnFilteredResults() throws Exception {
        // Create department matching keyword
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        DepartmentRequest secondRequest = new DepartmentRequest();
        secondRequest.setName("Finance");
        secondRequest.setDescription("Finance and accounting");
        secondRequest.setCode("DEPT-FIN");

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        // Search by keyword "Engineering"
        mockMvc.perform(get("/api/departments")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getDepartmentById_ShouldReturnDepartment() throws Exception {
        String createJson = mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        DepartmentResponse created = objectMapper.readValue(createJson, DepartmentResponse.class);

        mockMvc.perform(get("/api/departments/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.description").value("Engineering department"))
                .andExpect(jsonPath("$.code").value("DEPT-ENG"));
    }

    @Test
    void getDepartmentById_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/departments/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Department not found")))
                .andExpect(jsonPath("$.path").value("/api/departments/99999"));
    }

    @Test
    void updateDepartment_ShouldReturnUpdatedDepartment() throws Exception {
        String createJson = mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        DepartmentResponse created = objectMapper.readValue(createJson, DepartmentResponse.class);

        DepartmentRequest updateRequest = new DepartmentRequest();
        updateRequest.setName("Engineering Updated");
        updateRequest.setDescription("Updated engineering department");
        updateRequest.setCode("DEPT-ENG-UPD");

        mockMvc.perform(put("/api/departments/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.name").value("Engineering Updated"))
                .andExpect(jsonPath("$.description").value("Updated engineering department"))
                .andExpect(jsonPath("$.code").value("DEPT-ENG-UPD"));

        // Verify persistence
        assertTrue(departmentRepository.existsByName("Engineering Updated"));
        assertFalse(departmentRepository.existsByName("Engineering"));
    }

    @Test
    void updateDepartment_WithNonExistentId_ShouldReturn404() throws Exception {
        DepartmentRequest updateRequest = new DepartmentRequest();
        updateRequest.setName("NonExistent");
        updateRequest.setCode("DEPT-NE");

        mockMvc.perform(put("/api/departments/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Department not found")))
                .andExpect(jsonPath("$.path").value("/api/departments/99999"));
    }

    @Test
    void deleteDepartment_ShouldReturn204AndRemoveFromDatabase() throws Exception {
        String createJson = mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        DepartmentResponse created = objectMapper.readValue(createJson, DepartmentResponse.class);

        mockMvc.perform(delete("/api/departments/{id}", created.getId()))
                .andExpect(status().isNoContent());

        // Verify deletion from H2
        assertFalse(departmentRepository.findById(created.getId()).isPresent());
    }

    @Test
    void deleteDepartment_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/departments/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Department not found")))
                .andExpect(jsonPath("$.path").value("/api/departments/99999"));
    }
}

