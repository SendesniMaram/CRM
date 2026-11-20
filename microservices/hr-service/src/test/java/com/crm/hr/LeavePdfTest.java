package com.crm.hr;

import com.crm.hr.client.LeaveDirectoryClient;
import com.crm.hr.entity.LeaveEmployeeAssociation;
import com.crm.hr.entity.LeaveRequest;
import com.crm.hr.repository.LeaveEmployeeAssociationRepository;
import com.crm.hr.repository.LeaveRepository;
import com.crm.security.JwtService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.nio.file.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.cloud.service-registry.auto-registration.enabled=false", "eureka.client.enabled=false"})
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@Transactional
class LeavePdfTest extends com.crm.hr.security.TestIdentitySupport {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired LeaveRepository leaves;
    @Autowired LeaveEmployeeAssociationRepository links;
    @MockBean LeaveDirectoryClient directory;
    Long id;
    @BeforeEach void setup() {
        leaves.deleteAll(); links.deleteAll();
        var link = new LeaveEmployeeAssociation(); link.setUsername("pdf-owner");
        link.setUserId(41L); link.setEmployeeId("101"); links.saveAndFlush(link);
        var leave = new LeaveRequest(); leave.setEmployeeId("101");
        leave.setStartDate(LocalDate.of(2026, 11, 1)); leave.setEndDate(LocalDate.of(2026, 11, 4));
        leave.setReason("Congé synthétique — aucun dossier réel");
        id = leaves.saveAndFlush(leave).getId();
    }
    String auth(String name, String role) {
        String uid = name.equals("pdf-owner") ? "41" : name.equals("other") ? "42" : "99";
        return "Bearer " + signed(name, true, List.of(role), uid, java.time.Instant.now().plusSeconds(600));
    }
    @Test void ownerDownloadsActualRecord() throws Exception {
        var bytes = mvc.perform(get("/api/leaves/"+id+"/pdf").header("Authorization",auth("pdf-owner","EMPLOYEE")))
            .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.containsString("attachment")))
            .andReturn().getResponse().getContentAsByteArray();
        try(var doc = Loader.loadPDF(bytes)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("101")); assertTrue(text.contains("PENDING"));
            assertTrue(text.contains("Congé synthétique")); assertTrue(text.contains("01/11/2026"));
            assertFalse(text.contains("APPROVED"));
            Path dir=Path.of("target/pdf-examples"); Files.createDirectories(dir);
            Files.write(dir.resolve("leave-synthetic.pdf"),bytes);
            ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0,96),"png",dir.resolve("leave-synthetic.png").toFile());
        }
    }
    @Test void otherEmployeeIsForbiddenEvenWithSpoofedEmployeeParameter() throws Exception {
        var other = new LeaveEmployeeAssociation(); other.setUsername("other"); other.setUserId(42L); other.setEmployeeId("102"); links.saveAndFlush(other);
        mvc.perform(get("/api/leaves/"+id+"/pdf?employeeId=101").header("Authorization",auth("other","EMPLOYEE")))
            .andExpect(status().isForbidden());
    }
    @Test void missingAssociationIsForbidden() throws Exception {
        mvc.perform(get("/api/leaves/"+id+"/pdf").header("Authorization",auth("unlinked","EMPLOYEE"))).andExpect(status().isForbidden());
    }
    @Test void administratorCanReadAndMissingRecordIs404() throws Exception {
        mvc.perform(get("/api/leaves/"+id+"/pdf").header("Authorization",auth("admin","ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/leaves/999999/pdf").header("Authorization",auth("admin","ADMIN")).accept("application/pdf","application/json"))
            .andExpect(status().isNotFound());
    }
    @Test void anonymousAndClientAreRejected() throws Exception {
        mvc.perform(get("/api/leaves/"+id+"/pdf")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/leaves/"+id+"/pdf").header("Authorization",auth("client","CLIENT"))).andExpect(status().isForbidden());
    }
    @Test void approvedAndRejectedDocumentsShowOnlyTheRecordedDecision() throws Exception {
        for(var state:List.of(LeaveRequest.LeaveStatus.APPROVED, LeaveRequest.LeaveStatus.REJECTED)) {
            var leave = leaves.findById(id).orElseThrow();
            leave.setStatus(state); leave.setValidatorUsername("synthetic-validator"); leaves.saveAndFlush(leave);
            var bytes = mvc.perform(get("/api/leaves/"+id+"/pdf").header("Authorization",auth("pdf-owner","EMPLOYEE")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            try(var doc = Loader.loadPDF(bytes)) {
                String text = new PDFTextStripper().getText(doc);
                assertTrue(text.contains(state.name())); assertTrue(text.contains("synthetic-validator"));
            }
        }
    }
}
