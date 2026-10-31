package com.crm.invoice;

import com.crm.invoice.entity.Quote;
import com.crm.invoice.repository.InvoiceRepository;
import com.crm.invoice.repository.QuoteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest(properties = {
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class QuoteServiceIntegrationTest extends com.crm.invoice.security.TestIdentitySupport {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired QuoteRepository quoteRepository;
    @Autowired InvoiceRepository invoiceRepository;

    @BeforeEach
    void clean() {
        quoteRepository.deleteAll();
        invoiceRepository.deleteAll();
    }

    @Test
    void quoteCanBeEditedSearchedAndConvertedExactlyOnceWithItsLines() throws Exception {
        String quoteJson = mockMvc.perform(post("/api/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("DRAFT", "CRM setup", 2, 500, 100)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.subtotal").value(1000))
                .andExpect(jsonPath("$.totalAmount").value(1100))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        long quoteId = objectMapper.readTree(quoteJson).get("id").asLong();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "quote-test-admin", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            byte[] pdf = mockMvc.perform(get("/api/quotes/{id}/pdf", quoteId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andReturn().getResponse().getContentAsByteArray();
            assertTrue(new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1).startsWith("%PDF"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        mockMvc.perform(put("/api/quotes/{id}", quoteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("DRAFT", "CRM setup revised", 3, 400, 80)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(1200))
                .andExpect(jsonPath("$.totalAmount").value(1280))
                .andExpect(jsonPath("$.lines[0].description").value("CRM setup revised"));

        mockMvc.perform(get("/api/quotes")
                        .param("page", "0").param("size", "10")
                        .param("keyword", "DEV-").param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].lines", hasSize(1)));

        mockMvc.perform(patch("/api/quotes/{id}/status", quoteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        String firstConversion = mockMvc.perform(post("/api/quotes/{id}/convert", quoteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoice.subtotal").value(1200))
                .andExpect(jsonPath("$.invoice.lines", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        long invoiceId = objectMapper.readTree(firstConversion).get("invoice").get("id").asLong();

        String secondConversion = mockMvc.perform(post("/api/quotes/{id}/convert", quoteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoice.id").value(invoiceId))
                .andReturn().getResponse().getContentAsString();
        assertEquals(invoiceId, objectMapper.readTree(secondConversion).get("quote")
                .get("convertedInvoiceId").asLong());
        assertEquals(1, invoiceRepository.count());
        Quote persisted = quoteRepository.findById(quoteId).orElseThrow();
        assertNotNull(persisted.getConvertedInvoiceId());
    }

    @Test
    void quoteRequiresValidDatesLinesAndAcceptanceBeforeConversion() throws Exception {
        mockMvc.perform(post("/api/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("DRAFT", "Invalid dates", 1, 100, 0)
                                .replace("2026-11-28", "2026-09-01")))
                .andExpect(status().isBadRequest());

        String quoteJson = mockMvc.perform(post("/api/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("DRAFT", "Draft quote", 1, 100, 0)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long quoteId = objectMapper.readTree(quoteJson).get("id").asLong();

        mockMvc.perform(post("/api/quotes/{id}/convert", quoteId))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/quotes/{id}", quoteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("SENT", "Bypass status", 1, 100, 0)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/quotes").param("page", "-1"))
                .andExpect(status().isBadRequest());
        assertEquals(0, invoiceRepository.count());
    }

    private String requestJson(String status, String description, int quantity, int price, int tax) {
        return """
                {
                  "customerId":"42",
                  "issueDate":"2026-10-28",
                  "expiryDate":"2026-11-28",
                  "lines":[{"description":"%s","quantity":%d,"unitPrice":%d}],
                  "tax":%d,
                  "currency":"EUR",
                  "status":"%s",
                  "notes":"Test quote"
                }
                """.formatted(description, quantity, price, tax, status);
    }
}
