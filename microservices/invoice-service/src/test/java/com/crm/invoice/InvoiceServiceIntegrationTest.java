package com.crm.invoice;

import com.crm.invoice.dto.InvoiceRequest;
import com.crm.invoice.dto.InvoiceResponse;
import com.crm.invoice.repository.InvoiceRepository;
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
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
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
class InvoiceServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private InvoiceRequest validRequest;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        invoiceRepository.flush();

        validRequest = new InvoiceRequest();
        validRequest.setInvoiceNumber("INV-001");
        validRequest.setCustomerId("CUST001");
        validRequest.setIssueDate(LocalDate.of(2024, 1, 15));
        validRequest.setDueDate(LocalDate.of(2024, 2, 15));
        validRequest.setSubtotal(new BigDecimal("1000.00"));
        validRequest.setTax(new BigDecimal("200.00"));
        validRequest.setCurrency("EUR");
        validRequest.setStatus("PENDING");
        validRequest.setNotes("Invoice for January services");
    }

    @Test
    void createInvoice_ShouldReturn201AndPersist() throws Exception {
        String responseJson = mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.customerId").value("CUST001"))
                .andExpect(jsonPath("$.issueDate").isNotEmpty())
                .andExpect(jsonPath("$.dueDate").isNotEmpty())
                .andExpect(jsonPath("$.subtotal").value(1000.00))
                .andExpect(jsonPath("$.tax").value(200.00))
                .andExpect(jsonPath("$.totalAmount").value(1200.00))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.notes").value("Invoice for January services"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        InvoiceResponse response = objectMapper.readValue(responseJson, InvoiceResponse.class);
        assertNotNull(response.getId());

        invoiceRepository.flush();
        assertTrue(invoiceRepository.existsByInvoiceNumber("INV-001"));
    }

    @Test
    void createInvoice_WithDuplicateInvoiceNumber_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        invoiceRepository.flush();

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Invoice number already exists")))
                .andExpect(jsonPath("$.path").value("/api/invoices"));
    }

    @Test
    void createInvoice_WithValidationErrors_ShouldReturn400() throws Exception {
        InvoiceRequest invalidRequest = new InvoiceRequest();

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
                .andExpect(jsonPath("$.path").value("/api/invoices"));
    }

    @Test
    void getAllInvoices_ShouldReturnList() throws Exception {
        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        InvoiceRequest secondRequest = new InvoiceRequest();
        secondRequest.setInvoiceNumber("INV-002");
        secondRequest.setCustomerId("CUST002");
        secondRequest.setSubtotal(new BigDecimal("500.00"));
        secondRequest.setTax(new BigDecimal("50.00"));
        secondRequest.setCurrency("USD");
        secondRequest.setStatus("PAID");

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        invoiceRepository.flush();

        String responseJson = mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<InvoiceResponse> invoices = objectMapper.readValue(
                responseJson, new TypeReference<List<InvoiceResponse>>() {});
        assertEquals(2, invoices.size());
    }

    @Test
    void getAllInvoices_WithPagination_ShouldReturnPage() throws Exception {
        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        invoiceRepository.flush();

        mockMvc.perform(get("/api/invoices")
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
    void getAllInvoices_WithKeywordSearch_ShouldReturnFilteredResults() throws Exception {
        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        InvoiceRequest secondRequest = new InvoiceRequest();
        secondRequest.setInvoiceNumber("INV-003");
        secondRequest.setCustomerId("CUST003");
        secondRequest.setSubtotal(new BigDecimal("300.00"));
        secondRequest.setTax(new BigDecimal("30.00"));
        secondRequest.setCurrency("EUR");
        secondRequest.setStatus("PENDING");

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        invoiceRepository.flush();

        mockMvc.perform(get("/api/invoices")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "INV-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getInvoiceById_ShouldReturnInvoice() throws Exception {
        String createJson = mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        InvoiceResponse created = objectMapper.readValue(createJson, InvoiceResponse.class);

        mockMvc.perform(get("/api/invoices/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.customerId").value("CUST001"))
                .andExpect(jsonPath("$.subtotal").value(1000.00))
                .andExpect(jsonPath("$.tax").value(200.00))
                .andExpect(jsonPath("$.totalAmount").value(1200.00))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.notes").value("Invoice for January services"));
    }

    @Test
    void getInvoiceById_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/invoices/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Invoice not found")))
                .andExpect(jsonPath("$.path").value("/api/invoices/99999"));
    }

    @Test
    void updateInvoice_ShouldReturnUpdatedInvoice() throws Exception {
        String createJson = mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        InvoiceResponse created = objectMapper.readValue(createJson, InvoiceResponse.class);

        InvoiceRequest updateRequest = new InvoiceRequest();
        updateRequest.setInvoiceNumber("INV-001");
        updateRequest.setCustomerId("CUST001");
        updateRequest.setIssueDate(LocalDate.of(2024, 6, 1));
        updateRequest.setDueDate(LocalDate.of(2024, 7, 1));
        updateRequest.setSubtotal(new BigDecimal("2000.00"));
        updateRequest.setTax(new BigDecimal("400.00"));
        updateRequest.setCurrency("EUR");
        updateRequest.setStatus("PAID");
        updateRequest.setNotes("Updated invoice");

        mockMvc.perform(put("/api/invoices/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.subtotal").value(2000.00))
                .andExpect(jsonPath("$.tax").value(400.00))
                .andExpect(jsonPath("$.totalAmount").value(2400.00))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.notes").value("Updated invoice"));

        invoiceRepository.flush();
        assertTrue(invoiceRepository.existsByInvoiceNumber("INV-001"));
    }

    @Test
    void updateInvoice_WithNonExistentId_ShouldReturn404() throws Exception {
        InvoiceRequest updateRequest = new InvoiceRequest();
        updateRequest.setInvoiceNumber("INV-999");
        updateRequest.setCustomerId("CUST999");
        updateRequest.setSubtotal(new BigDecimal("100.00"));
        updateRequest.setTax(new BigDecimal("10.00"));
        updateRequest.setCurrency("EUR");
        updateRequest.setStatus("PENDING");

        mockMvc.perform(put("/api/invoices/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Invoice not found")))
                .andExpect(jsonPath("$.path").value("/api/invoices/99999"));
    }

    @Test
    void deleteInvoice_ShouldReturn204AndRemoveFromDatabase() throws Exception {
        String createJson = mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        InvoiceResponse created = objectMapper.readValue(createJson, InvoiceResponse.class);

        mockMvc.perform(delete("/api/invoices/{id}", created.getId()))
                .andExpect(status().isNoContent());

        assertFalse(invoiceRepository.findById(created.getId()).isPresent());
    }

    @Test
    void deleteInvoice_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/invoices/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Invoice not found")))
                .andExpect(jsonPath("$.path").value("/api/invoices/99999"));
    }
}
