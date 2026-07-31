package com.crm.hr;

import com.crm.hr.dto.LeaveRequestDto;
import com.crm.hr.dto.LeaveResponse;
import com.crm.hr.repository.LeaveRepository;
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

import java.time.LocalDate;
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
class LeaveServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LeaveRepository leaveRepository;

    private LeaveRequestDto validRequest;

    @BeforeEach
    void setUp() {
        leaveRepository.deleteAll();

        validRequest = new LeaveRequestDto();
        validRequest.setEmployeeId("EMP001");
        validRequest.setStartDate(LocalDate.now());
        validRequest.setEndDate(LocalDate.now().plusDays(5));
        validRequest.setReason("Annual vacation");
    }

    @Test
    void createLeave_ShouldReturn201AndPersist() throws Exception {
        String responseJson = mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.startDate").isNotEmpty())
                .andExpect(jsonPath("$.endDate").isNotEmpty())
                .andExpect(jsonPath("$.reason").value("Annual vacation"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LeaveResponse response = objectMapper.readValue(responseJson, LeaveResponse.class);
        assertNotNull(response.getId());
        assertEquals("PENDING", response.getStatus());
    }

    @Test
    void createLeave_WithInvalidDateRange_ShouldReturn400() throws Exception {
        LeaveRequestDto invalidRequest = new LeaveRequestDto();
        invalidRequest.setEmployeeId("EMP001");
        invalidRequest.setStartDate(LocalDate.now().plusDays(5));
        invalidRequest.setEndDate(LocalDate.now()); // End before start

        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("End date must be after or equal to start date")))
                .andExpect(jsonPath("$.path").value("/api/leaves"));
    }

    @Test
    void createLeave_WithValidationErrors_ShouldReturn400() throws Exception {
        LeaveRequestDto invalidRequest = new LeaveRequestDto();
        // Missing employeeId (NotBlank)

        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
                .andExpect(jsonPath("$.path").value("/api/leaves"));
    }

    @Test
    void getAllLeaves_ShouldReturnList() throws Exception {
        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        LeaveRequestDto secondRequest = new LeaveRequestDto();
        secondRequest.setEmployeeId("EMP002");
        secondRequest.setStartDate(LocalDate.now().plusDays(10));
        secondRequest.setEndDate(LocalDate.now().plusDays(12));
        secondRequest.setReason("Sick leave");

        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        String responseJson = mockMvc.perform(get("/api/leaves"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<LeaveResponse> leaves = objectMapper.readValue(
                responseJson, new TypeReference<List<LeaveResponse>>() {});
        assertEquals(2, leaves.size());
    }

    @Test
    void getAllLeaves_WithPagination_ShouldReturnPage() throws Exception {
        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/leaves")
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
    void getAllLeaves_WithKeywordSearch_ShouldReturnFilteredResults() throws Exception {
        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        LeaveRequestDto secondRequest = new LeaveRequestDto();
        secondRequest.setEmployeeId("EMP003");
        secondRequest.setStartDate(LocalDate.now().plusDays(20));
        secondRequest.setEndDate(LocalDate.now().plusDays(22));
        secondRequest.setReason("Training");

        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/leaves")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "EMP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getLeaveById_ShouldReturnLeave() throws Exception {
        String createJson = mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LeaveResponse created = objectMapper.readValue(createJson, LeaveResponse.class);

        mockMvc.perform(get("/api/leaves/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.reason").value("Annual vacation"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getLeaveById_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/leaves/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Leave request not found")))
                .andExpect(jsonPath("$.path").value("/api/leaves/99999"));
    }

    @Test
    void updateLeave_ShouldReturnUpdatedLeave() throws Exception {
        String createJson = mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LeaveResponse created = objectMapper.readValue(createJson, LeaveResponse.class);

        LeaveRequestDto updateRequest = new LeaveRequestDto();
        updateRequest.setEmployeeId("EMP001");
        updateRequest.setStartDate(LocalDate.now().plusDays(2));
        updateRequest.setEndDate(LocalDate.now().plusDays(10));
        updateRequest.setReason("Extended vacation");

        mockMvc.perform(put("/api/leaves/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.reason").value("Extended vacation"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void updateLeave_WithNonExistentId_ShouldReturn404() throws Exception {
        LeaveRequestDto updateRequest = new LeaveRequestDto();
        updateRequest.setEmployeeId("EMP999");
        updateRequest.setStartDate(LocalDate.now());
        updateRequest.setEndDate(LocalDate.now().plusDays(1));

        mockMvc.perform(put("/api/leaves/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Leave request not found")))
                .andExpect(jsonPath("$.path").value("/api/leaves/99999"));
    }

    @Test
    void approveLeave_ShouldReturnApproved() throws Exception {
        String createJson = mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LeaveResponse created = objectMapper.readValue(createJson, LeaveResponse.class);

        mockMvc.perform(patch("/api/leaves/{id}/approve", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectLeave_ShouldReturnRejected() throws Exception {
        String createJson = mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LeaveResponse created = objectMapper.readValue(createJson, LeaveResponse.class);

        mockMvc.perform(patch("/api/leaves/{id}/reject", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void deleteLeave_ShouldReturn204AndRemoveFromDatabase() throws Exception {
        String createJson = mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LeaveResponse created = objectMapper.readValue(createJson, LeaveResponse.class);

        mockMvc.perform(delete("/api/leaves/{id}", created.getId()))
                .andExpect(status().isNoContent());

        assertFalse(leaveRepository.findById(created.getId()).isPresent());
    }

    @Test
    void deleteLeave_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/leaves/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Leave request not found")))
                .andExpect(jsonPath("$.path").value("/api/leaves/99999"));
    }
}
