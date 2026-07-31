package com.crm.hr;

import com.crm.hr.dto.AttendanceRequest;
import com.crm.hr.dto.AttendanceResponse;
import com.crm.hr.repository.AttendanceRepository;
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
import java.time.LocalTime;
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
class AttendanceServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AttendanceRepository attendanceRepository;

    private AttendanceRequest validRequest;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();

        validRequest = new AttendanceRequest();
        validRequest.setEmployeeId("EMP001");
        validRequest.setDate(LocalDate.now());
        validRequest.setCheckIn(LocalTime.of(9, 0));
        validRequest.setCheckOut(LocalTime.of(18, 0));
        validRequest.setStatus("PRESENT");
        validRequest.setNotes("On time");
    }

    @Test
    void createAttendance_ShouldReturn201AndPersist() throws Exception {
        String responseJson = mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.date").isNotEmpty())
                .andExpect(jsonPath("$.checkIn").value("09:00:00"))
                .andExpect(jsonPath("$.checkOut").value("18:00:00"))
                .andExpect(jsonPath("$.status").value("PRESENT"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AttendanceResponse response = objectMapper.readValue(responseJson, AttendanceResponse.class);
        assertNotNull(response.getId());
    }

    @Test
    void createAttendance_WithDuplicate_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Attendance already exists")))
                .andExpect(jsonPath("$.path").value("/api/attendances"));
    }

    @Test
    void createAttendance_WithValidationErrors_ShouldReturn400() throws Exception {
        AttendanceRequest invalidRequest = new AttendanceRequest();

        mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
                .andExpect(jsonPath("$.path").value("/api/attendances"));
    }

    @Test
    void getAllAttendances_ShouldReturnList() throws Exception {
        mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        AttendanceRequest secondRequest = new AttendanceRequest();
        secondRequest.setEmployeeId("EMP002");
        secondRequest.setDate(LocalDate.now());
        secondRequest.setCheckIn(LocalTime.of(10, 0));
        secondRequest.setCheckOut(LocalTime.of(19, 0));
        secondRequest.setStatus("LATE");
        secondRequest.setNotes("Arrived late");

        mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        String responseJson = mockMvc.perform(get("/api/attendances"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<AttendanceResponse> attendances = objectMapper.readValue(
                responseJson, new TypeReference<List<AttendanceResponse>>() {});
        assertEquals(2, attendances.size());
    }

    @Test
    void getAllAttendances_WithPagination_ShouldReturnPage() throws Exception {
        mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/attendances")
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
    void getAllAttendances_WithKeywordSearch_ShouldReturnFilteredResults() throws Exception {
        mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        AttendanceRequest secondRequest = new AttendanceRequest();
        secondRequest.setEmployeeId("EMP002");
        secondRequest.setDate(LocalDate.now());
        secondRequest.setStatus("ABSENT");

        mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/attendances")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "EMP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAttendanceById_ShouldReturnAttendance() throws Exception {
        String createJson = mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AttendanceResponse created = objectMapper.readValue(createJson, AttendanceResponse.class);

        mockMvc.perform(get("/api/attendances/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.status").value("PRESENT"));
    }

    @Test
    void getAttendanceById_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/attendances/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Attendance not found")))
                .andExpect(jsonPath("$.path").value("/api/attendances/99999"));
    }

    @Test
    void updateAttendance_ShouldReturnUpdatedAttendance() throws Exception {
        String createJson = mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AttendanceResponse created = objectMapper.readValue(createJson, AttendanceResponse.class);

        AttendanceRequest updateRequest = new AttendanceRequest();
        updateRequest.setEmployeeId("EMP001");
        updateRequest.setDate(LocalDate.now());
        updateRequest.setCheckIn(LocalTime.of(8, 30));
        updateRequest.setCheckOut(LocalTime.of(17, 30));
        updateRequest.setStatus("PRESENT");

        mockMvc.perform(put("/api/attendances/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.checkIn").value("08:30:00"))
                .andExpect(jsonPath("$.checkOut").value("17:30:00"))
                .andExpect(jsonPath("$.status").value("PRESENT"));
    }

    @Test
    void updateAttendance_WithNonExistentId_ShouldReturn404() throws Exception {
        AttendanceRequest updateRequest = new AttendanceRequest();
        updateRequest.setEmployeeId("EMP999");
        updateRequest.setDate(LocalDate.now());
        updateRequest.setStatus("ABSENT");

        mockMvc.perform(put("/api/attendances/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Attendance not found")))
                .andExpect(jsonPath("$.path").value("/api/attendances/99999"));
    }

    @Test
    void deleteAttendance_ShouldReturn204AndRemoveFromDatabase() throws Exception {
        String createJson = mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AttendanceResponse created = objectMapper.readValue(createJson, AttendanceResponse.class);

        mockMvc.perform(delete("/api/attendances/{id}", created.getId()))
                .andExpect(status().isNoContent());

        assertFalse(attendanceRepository.findById(created.getId()).isPresent());
    }

    @Test
    void deleteAttendance_WithNonExistentId_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/attendances/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Attendance not found")))
                .andExpect(jsonPath("$.path").value("/api/attendances/99999"));
    }
}
