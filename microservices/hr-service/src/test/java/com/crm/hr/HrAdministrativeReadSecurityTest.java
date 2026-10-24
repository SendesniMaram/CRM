package com.crm.hr;

import com.crm.hr.controller.HrController;
import com.crm.hr.dto.HrResponse;
import com.crm.hr.service.IHrService;
import com.crm.hr.controller.AttendanceController;
import com.crm.hr.dto.AttendanceResponse;
import com.crm.hr.service.IAttendanceService;
import com.crm.hr.controller.TrainingController;
import com.crm.hr.dto.TrainingResponse;
import com.crm.hr.service.ITrainingService;
import com.crm.hr.config.SecurityConfig;
import com.crm.security.JwtSecurityAutoConfiguration;
import com.crm.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real SecurityFilterChain and JWT filter; only business services are replaced.
@WebMvcTest(controllers = {HrController.class, AttendanceController.class, TrainingController.class})
@Import({SecurityConfig.class, JwtSecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = true, print = MockMvcPrint.NONE)
class HrAdministrativeReadSecurityTest extends com.crm.hr.security.TestIdentitySupport {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean IHrService service0;
    @MockBean IAttendanceService service1;
    @MockBean ITrainingService service2;

    @BeforeEach
    void responses() {
        HrResponse row0 = new HrResponse();
        row0.setId(1L);
        when(service0.getAllRecords()).thenReturn(List.of(row0));
        when(service0.getAllRecordsPaged(anyInt(), anyInt(), any(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of(row0)));
        when(service0.getRecordById(1L)).thenReturn(row0);
        AttendanceResponse row1 = new AttendanceResponse();
        row1.setId(1L);
        when(service1.getAllAttendances()).thenReturn(List.of(row1));
        when(service1.getAllAttendancesPaged(anyInt(), anyInt(), any(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of(row1)));
        when(service1.getAttendanceById(1L)).thenReturn(row1);
        TrainingResponse row2 = new TrainingResponse();
        row2.setId(1L);
        when(service2.getAllTrainings()).thenReturn(List.of(row2));
        when(service2.getAllTrainingsPaged(anyInt(), anyInt(), any(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of(row2)));
        when(service2.getTrainingById(1L)).thenReturn(row2);
    }

    static Stream<Arguments> accessMatrix() {
        return Stream.of("/api/hr-records",
                "/api/hr-records?size=20&keyword=test&sortBy=id",
                "/api/hr-records/1",
                "/api/attendances",
                "/api/attendances?size=20&keyword=test&sortBy=id",
                "/api/attendances/1",
                "/api/trainings",
                "/api/trainings?size=20&keyword=test&sortBy=id",
                "/api/trainings/1")
                .flatMap(path -> Stream.of(
                        Arguments.of(path, "ANONYMOUS", 401),
                        Arguments.of(path, "EMPLOYEE", 403),
                        Arguments.of(path, "CLIENT", 403),
                        Arguments.of(path, "ADMIN", 200),
                        Arguments.of(path, "SUPER_ADMIN", 200)));
    }

    @ParameterizedTest(name = "{1} GET {0} returns {2}")
    @MethodSource("accessMatrix")
    void administrativeReadsRequireAnAdministrator(String path, String role, int expected) throws Exception {
        var request = get(path);
        if (!role.equals("ANONYMOUS")) {
            request.header("Authorization", "Bearer " + com.crm.hr.security.TestIdentitySupport.token(
                    "security-test-user", "", true, List.of(role)));
        }
        var result = mvc.perform(request).andExpect(status().is(expected));
        if (expected != 200) {
            verifyNoInteractions(service0, service1, service2);
        } else if (path.contains("?")) {
            result.andExpect(jsonPath("$.content[0].id").value(1));
        } else if (path.endsWith("/details")) {
            result.andExpect(jsonPath("$.employee.id").value(1));
        } else if (path.endsWith("/1")) {
            result.andExpect(jsonPath("$.id").value(1));
        } else {
            result.andExpect(jsonPath("$[0].id").value(1));
        }
    }
}
