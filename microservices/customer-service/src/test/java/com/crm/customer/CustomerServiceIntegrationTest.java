package com.crm.customer;

import com.crm.customer.dto.CustomerRequest;
import com.crm.customer.dto.CustomerResponse;
import com.crm.customer.repository.CustomerRepository;
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
class CustomerServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    private CustomerRequest validRequest;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();

        validRequest = new CustomerRequest();
        validRequest.setName("Acme Corp");
        validRequest.setEmail("contact@acme.com");
        validRequest.setPhone("+123456789");
        validRequest.setCompany("Acme Corporation");
        validRequest.setAddress("123 Main Street, City");
    }

    @Test
    void createCustomer_ShouldReturn201AndPersist() throws Exception {
        String responseJson = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Acme Corp"))
                .andExpect(jsonPath("$.email").value("contact@acme.com"))
                .andExpect(jsonPath("$.phone").value("+123456789"))
                .andExpect(jsonPath("$.company").value("Acme Corporation"))
                .andExpect(jsonPath("$.address").value("123 Main Street, City"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CustomerResponse response = objectMapper.readValue(responseJson, CustomerResponse.class);
        assertNotNull(response.getId());

        // Verify persistence in H2
        assertTrue(customerRepository.existsByName("Acme Corp"));
        assertTrue(customerRepository.existsByEmail("contact@acme.com"));
    }

    @Test
    void createCustomer_WithDuplicateEmail_ShouldReturn400() throws Exception {
        // First creation succeeds
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        // Second creation with same email should fail
        CustomerRequest duplicateEmailRequest = new CustomerRequest();
        duplicateEmailRequest.setName("Other Corp");
        duplicateEmailRequest.setEmail("contact@acme.com"); // Same email
        duplicateEmailRequest.setPhone("+987654321");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmailRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Customer email already exists")))
                .andExpect(jsonPath("$.path").value("/api/customers"));
    }

    @Test
    void createCustomer_WithDuplicatePhone_ShouldReturn400() throws Exception {
        // First creation succeeds
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        // Second creation with same phone should fail
        CustomerRequest duplicatePhoneRequest = new CustomerRequest();
        duplicatePhoneRequest.setName("Other Corp");
        duplicatePhoneRequest.setEmail("other@acme.com");
        duplicatePhoneRequest.setPhone("+123456789"); // Same phone

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicatePhoneRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Customer phone already exists")))
                .andExpect(jsonPath("$.path").value("/api/customers"));
    }

    @Test
    void createCustomer_WithValidationErrors_ShouldReturn400() throws Exception {
        CustomerRequest invalidRequest = new CustomerRequest();
        // Missing name (NotBlank)

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
                .andExpect(jsonPath("$.path").value("/api/customers"));
    }

    @Test
    void getAllCustomers_ShouldReturnList() throws Exception {
        // Create two customers
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        CustomerRequest secondRequest = new CustomerRequest();
        secondRequest.setName("Beta Inc");
        secondRequest.setEmail("info@beta.com");
        secondRequest.setPhone("+987654321");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        String responseJson = mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<CustomerResponse> customers = objectMapper.readValue(
                responseJson, new TypeReference<List<CustomerResponse>>() {});
        assertEquals(2, customers.size());
    }

    @Test
    void getAllCustomers_WithPagination_ShouldReturnPage() throws Exception {
        // Create a customer first
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/customers")
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
    void getAllCustomers_WithKeywordSearch_ShouldReturnFilteredResults() throws Exception {
        // Create customer matching keyword
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        CustomerRequest secondRequest = new CustomerRequest();
        secondRequest.setName("Gamma Ltd");
        secondRequest.setEmail("gamma@example.com");
        secondRequest.setPhone("+555666777");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        // Search by keyword "Acme"
        mockMvc.perform(get("/api/customers")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "Acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getCustomerById_ShouldReturnCustomer() throws Exception {
        String createJson = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CustomerResponse created = objectMapper.readValue(createJson, CustomerResponse.class);

        mockMvc.perform(get("/api/customers/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.name").value("Acme Corp"))
                .andExpect(jsonPath("$.email").value("contact@acme.com"))
                .andExpect(jsonPath("$.phone").value("+123456789"))
                .andExpect(jsonPath("$.company").value("Acme Corporation"))
                .andExpect(jsonPath("$.address").value("123 Main Street, City"));
    }

    @Test
    void getCustomerById_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/customers/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Customer not found")))
                .andExpect(jsonPath("$.path").value("/api/customers/99999"));
    }

    @Test
    void updateCustomer_ShouldReturnUpdatedCustomer() throws Exception {
        String createJson = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CustomerResponse created = objectMapper.readValue(createJson, CustomerResponse.class);

        CustomerRequest updateRequest = new CustomerRequest();
        updateRequest.setName("Acme Corp Updated");
        updateRequest.setEmail("contact.updated@acme.com");
        updateRequest.setPhone("+111222333");
        updateRequest.setCompany("Acme Corporation Updated");
        updateRequest.setAddress("456 Oak Avenue, City");

        mockMvc.perform(put("/api/customers/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.name").value("Acme Corp Updated"))
                .andExpect(jsonPath("$.email").value("contact.updated@acme.com"))
                .andExpect(jsonPath("$.phone").value("+111222333"))
                .andExpect(jsonPath("$.company").value("Acme Corporation Updated"))
                .andExpect(jsonPath("$.address").value("456 Oak Avenue, City"));

        // Verify persistence
        assertTrue(customerRepository.existsByName("Acme Corp Updated"));
        assertTrue(customerRepository.existsByEmail("contact.updated@acme.com"));
    }

    @Test
    void updateCustomer_WithNonExistentId_ShouldReturn404() throws Exception {
        CustomerRequest updateRequest = new CustomerRequest();
        updateRequest.setName("NonExistent");
        updateRequest.setEmail("nonexistent@test.com");
        updateRequest.setPhone("+000000000");

        mockMvc.perform(put("/api/customers/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Customer not found")))
                .andExpect(jsonPath("$.path").value("/api/customers/99999"));
    }

    @Test
    void deleteCustomer_ShouldReturn204AndRemoveFromDatabase() throws Exception {
        String createJson = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CustomerResponse created = objectMapper.readValue(createJson, CustomerResponse.class);

        mockMvc.perform(delete("/api/customers/{id}", created.getId()))
                .andExpect(status().isNoContent());

        // Verify deletion from H2
        assertFalse(customerRepository.findById(created.getId()).isPresent());
    }

    @Test
    void deleteCustomer_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/customers/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Customer not found")))
                .andExpect(jsonPath("$.path").value("/api/customers/99999"));
    }
}

