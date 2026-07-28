package com.crm.employee;

import com.crm.employee.client.DepartmentClient;
import com.crm.employee.dto.EmployeeRequest;
import com.crm.employee.dto.EmployeeResponse;
import com.crm.employee.repository.EmployeeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
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
class EmployeeServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @MockBean
    private DepartmentClient departmentClient;

    private EmployeeRequest validRequest;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();

        validRequest = new EmployeeRequest();
        validRequest.setEmployeeCode("EMP001");
        validRequest.setFirstName("John");
        validRequest.setLastName("Doe");
        validRequest.setEmail("john.doe@example.com");
        validRequest.setPhone("+123456789");
        validRequest.setJobTitle("Software Engineer");
        validRequest.setSalary(new BigDecimal("60000.00"));
        validRequest.setGender("Male");
        validRequest.setStatus("ACTIVE");
    }

    @Test
    void createEmployee_ShouldReturn201AndPersist() throws Exception {
        String responseJson = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.employeeCode").value("EMP001"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.phone").value("+123456789"))
                .andExpect(jsonPath("$.jobTitle").value("Software Engineer"))
                .andExpect(jsonPath("$.salary").value(60000.0))
                .andExpect(jsonPath("$.gender").value("Male"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        EmployeeResponse response = objectMapper.readValue(responseJson, EmployeeResponse.class);
        assertNotNull(response.getId());

        // Verify persistence in H2
        assertTrue(employeeRepository.existsByEmployeeCode("EMP001"));
        assertTrue(employeeRepository.existsByEmail("john.doe@example.com"));
    }

    @Test
    void createEmployee_WithDuplicateEmployeeCode_ShouldReturn400() throws Exception {
        // First creation succeeds
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        // Second creation with same employeeCode should fail
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Employee code already exists")))
                .andExpect(jsonPath("$.path").value("/api/employees"));
    }

    @Test
    void createEmployee_WithDuplicateEmail_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        EmployeeRequest duplicateEmailRequest = new EmployeeRequest();
        duplicateEmailRequest.setEmployeeCode("EMP002");
        duplicateEmailRequest.setFirstName("Jane");
        duplicateEmailRequest.setLastName("Doe");
        duplicateEmailRequest.setEmail("john.doe@example.com"); // Same email
        duplicateEmailRequest.setSalary(new BigDecimal("55000.00"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmailRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Email already exists")))
                .andExpect(jsonPath("$.path").value("/api/employees"));
    }

    @Test
    void createEmployee_WithValidationErrors_ShouldReturn400() throws Exception {
        EmployeeRequest invalidRequest = new EmployeeRequest();
        // Missing employeeCode (NotBlank)
        invalidRequest.setSalary(new BigDecimal("-100.00")); // Negative salary

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
                .andExpect(jsonPath("$.path").value("/api/employees"));
    }

    @Test
    void getAllEmployees_ShouldReturnList() throws Exception {
        // Create two employees
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        EmployeeRequest secondRequest = new EmployeeRequest();
        secondRequest.setEmployeeCode("EMP002");
        secondRequest.setFirstName("Jane");
        secondRequest.setLastName("Smith");
        secondRequest.setEmail("jane.smith@example.com");
        secondRequest.setSalary(new BigDecimal("55000.00"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        String responseJson = mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<EmployeeResponse> employees = objectMapper.readValue(
                responseJson, new TypeReference<List<EmployeeResponse>>() {});
        assertEquals(2, employees.size());
    }

    @Test
    void getAllEmployees_WithPagination_ShouldReturnPage() throws Exception {
        // Create an employee first
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/employees")
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
    void getAllEmployees_WithKeywordSearch_ShouldReturnFilteredResults() throws Exception {
        // Create employee with keyword-matching data
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        EmployeeRequest secondRequest = new EmployeeRequest();
        secondRequest.setEmployeeCode("EMP002");
        secondRequest.setFirstName("Alice");
        secondRequest.setLastName("Wonderland");
        secondRequest.setEmail("alice@example.com");
        secondRequest.setSalary(new BigDecimal("70000.00"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        // Search by keyword "John"
        mockMvc.perform(get("/api/employees")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getEmployeeById_ShouldReturnEmployee() throws Exception {
        String createJson = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        EmployeeResponse created = objectMapper.readValue(createJson, EmployeeResponse.class);

        mockMvc.perform(get("/api/employees/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.employeeCode").value("EMP001"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void getEmployeeById_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/employees/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Employee not found")))
                .andExpect(jsonPath("$.path").value("/api/employees/99999"));
    }

    @Test
    void updateEmployee_ShouldReturnUpdatedEmployee() throws Exception {
        String createJson = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        EmployeeResponse created = objectMapper.readValue(createJson, EmployeeResponse.class);

        EmployeeRequest updateRequest = new EmployeeRequest();
        updateRequest.setEmployeeCode("EMP001"); // Same code
        updateRequest.setFirstName("JohnUpdated");
        updateRequest.setLastName("DoeUpdated");
        updateRequest.setEmail("john.updated@example.com");
        updateRequest.setSalary(new BigDecimal("65000.00"));

        mockMvc.perform(put("/api/employees/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.firstName").value("JohnUpdated"))
                .andExpect(jsonPath("$.lastName").value("DoeUpdated"))
                .andExpect(jsonPath("$.email").value("john.updated@example.com"))
                .andExpect(jsonPath("$.salary").value(65000.0));

        // Verify persistence
        assertTrue(employeeRepository.existsByEmail("john.updated@example.com"));
    }

    @Test
    void updateEmployee_WithNonExistentId_ShouldReturn404() throws Exception {
        EmployeeRequest updateRequest = new EmployeeRequest();
        updateRequest.setEmployeeCode("EMP999");
        updateRequest.setSalary(new BigDecimal("50000.00"));

        mockMvc.perform(put("/api/employees/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Employee not found")))
                .andExpect(jsonPath("$.path").value("/api/employees/99999"));
    }

    @Test
    void deleteEmployee_ShouldReturn204AndRemoveFromDatabase() throws Exception {
        String createJson = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        EmployeeResponse created = objectMapper.readValue(createJson, EmployeeResponse.class);

        mockMvc.perform(delete("/api/employees/{id}", created.getId()))
                .andExpect(status().isNoContent());

        // Verify deletion from H2
        assertFalse(employeeRepository.findById(created.getId()).isPresent());
    }

    @Test
    void deleteEmployee_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/employees/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Employee not found")))
                .andExpect(jsonPath("$.path").value("/api/employees/99999"));
    }
}

