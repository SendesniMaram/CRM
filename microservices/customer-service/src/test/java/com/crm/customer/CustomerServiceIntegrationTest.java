package com.crm.customer;

import com.crm.customer.dto.CustomerRequest;
import com.crm.customer.dto.CustomerResponse;
import com.crm.customer.repository.CustomerRepository;
import com.crm.customer.repository.CustomerInteractionRepository;
import com.crm.customer.repository.OpportunityStageHistoryRepository;
import com.crm.customer.repository.SalesOpportunityRepository;
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
class CustomerServiceIntegrationTest extends com.crm.customer.security.TestIdentitySupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerInteractionRepository interactionRepository;

    @Autowired
    private SalesOpportunityRepository opportunityRepository;

    @Autowired
    private OpportunityStageHistoryRepository stageHistoryRepository;

    private CustomerRequest validRequest;

    @BeforeEach
    void setUp() {
        stageHistoryRepository.deleteAll();
        opportunityRepository.deleteAll();
        interactionRepository.deleteAll();
        customerRepository.deleteAll();

        validRequest = new CustomerRequest();
        validRequest.setName("Acme Corp");
        validRequest.setEmail("contact@acme.com");
        validRequest.setPhone("+123456789");
        validRequest.setCompany("Acme Corporation");
        validRequest.setAddress("123 Main Street, City");
    }

    @Test
    void addressAccepts500CharactersAndRejects501() throws Exception {
        validRequest.setAddress("a".repeat(500));
        String response = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readValue(response, CustomerResponse.class).getId();
        customerRepository.flush();
        assertEquals("a".repeat(500), customerRepository.findById(id).orElseThrow().getAddress());

        validRequest.setAddress("a".repeat(501));
        mockMvc.perform(put("/api/customers/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
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
    void leadClassificationFilteringAndConversionPreserveTheExistingRecord() throws Exception {
        validRequest.setContactType("LEAD"); validRequest.setCategory("Enterprise"); validRequest.setSegment("North");
        String created = mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.contactType").value("LEAD"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();
        mockMvc.perform(get("/api/customers").param("page", "0").param("size", "10")
                        .param("sortBy", "name").param("contactType", "LEAD").param("segment", "North"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(post("/api/customers/{id}/convert", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.contactType").value("CUSTOMER"));
        assertEquals("Enterprise", customerRepository.findById(id).orElseThrow().getCategory());
        mockMvc.perform(post("/api/customers/{id}/convert", id)).andExpect(status().isBadRequest());
    }

    @Test
    void interactionsArePersistedAndListedNewestFirst() throws Exception {
        String customerJson = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long customerId = objectMapper.readTree(customerJson).get("id").asLong();

        mockMvc.perform(post("/api/customers/{id}/interactions", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"interactionType":"CALL","occurredAt":"2026-10-20T09:30:00","notes":"Premier appel"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.interactionType").value("CALL"));
        mockMvc.perform(post("/api/customers/{id}/interactions", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"interactionType":"EMAIL","occurredAt":"2026-10-21T11:00:00","notes":"Envoi du devis"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/customers/{id}/interactions", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].notes").value("Envoi du devis"))
                .andExpect(jsonPath("$[1].notes").value("Premier appel"));
        assertEquals(2, interactionRepository.findByCustomerIdOrderByOccurredAtDesc(customerId).size());
    }

    @Test
    void interactionEndpointsRejectUnknownCustomersAndInvalidTypes() throws Exception {
        mockMvc.perform(get("/api/customers/{id}/interactions", 99999L))
                .andExpect(status().isNotFound());

        String customerJson = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long customerId = objectMapper.readTree(customerJson).get("id").asLong();

        mockMvc.perform(post("/api/customers/{id}/interactions", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"interactionType":"SMS","occurredAt":"2026-10-20T09:30:00"}
                                """))
                .andExpect(status().isBadRequest());
        assertTrue(interactionRepository.findByCustomerIdOrderByOccurredAtDesc(customerId).isEmpty());
    }

    @Test
    void opportunitiesSupportSearchStageHistoryAndControlledDeletion() throws Exception {
        validRequest.setContactType("LEAD");
        String customerJson = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long customerId = objectMapper.readTree(customerJson).get("id").asLong();

        String opportunityJson = mockMvc.perform(post("/api/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":%d,"title":"CRM Renewal","description":"Annual renewal","estimatedAmount":1250.50,"stage":"NEW","expectedCloseDate":"2027-01-15"}
                                """.formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stage").value("NEW"))
                .andExpect(jsonPath("$.expectedCloseDate").value("2027-01-15"))
                .andReturn().getResponse().getContentAsString();
        long opportunityId = objectMapper.readTree(opportunityJson).get("id").asLong();

        mockMvc.perform(patch("/api/opportunities/{id}/stage", opportunityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stage":"QUALIFICATION"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("QUALIFICATION"));
        mockMvc.perform(get("/api/opportunities")
                        .param("page", "0").param("size", "10")
                        .param("keyword", "renewal").param("stage", "QUALIFICATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(opportunityId));
        mockMvc.perform(get("/api/opportunities/{id}/history", opportunityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].previousStage").value("NEW"))
                .andExpect(jsonPath("$[0].newStage").value("QUALIFICATION"));

        mockMvc.perform(patch("/api/opportunities/{id}/stage", opportunityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stage":"WON"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/opportunities/{id}", opportunityId))
                .andExpect(status().isConflict());
    }

    @Test
    void opportunitiesRejectSuppliersAsSalesContactsAndInvalidStages() throws Exception {
        validRequest.setContactType("SUPPLIER");
        String customerJson = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long customerId = objectMapper.readTree(customerJson).get("id").asLong();

        mockMvc.perform(post("/api/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":%d,"title":"Invalid owner","stage":"NEW"}
                                """.formatted(customerId)))
                .andExpect(status().isBadRequest());
        assertTrue(opportunityRepository.findAll().isEmpty());
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
