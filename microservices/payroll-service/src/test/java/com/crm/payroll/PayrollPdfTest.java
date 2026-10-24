package com.crm.payroll;
import com.crm.payroll.controller.PayrollPdfController;
import com.crm.payroll.dto.PayrollResponse;
import com.crm.payroll.service.IPayrollService;
import com.crm.payroll.config.SecurityConfig;
import com.crm.payroll.exception.ResourceNotFoundException;
import com.crm.security.JwtSecurityAutoConfiguration;
import com.crm.security.JwtService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.math.BigDecimal;
import java.nio.file.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(PayrollPdfController.class)
@Import({SecurityConfig.class, JwtSecurityAutoConfiguration.class})
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class PayrollPdfTest extends com.crm.payroll.security.TestIdentitySupport {
 @Autowired MockMvc mvc;
 @Autowired JwtService jwt;
 @MockBean IPayrollService service;
 String auth(String role) { return "Bearer " + com.crm.payroll.security.TestIdentitySupport.token("pdf-synthetic-test", "", true, List.of(role)); }
 @Test void rendersStoredAmountsAndFrenchAccentsAsA4Attachment() throws Exception {
  var row = new PayrollResponse(); row.setId(42L); row.setCurrency("TND"); row.setStatus("PENDING");
  row.setEmployeeId("EMP-TEST"); row.setPayPeriod("2026-10"); row.setBaseSalary(new BigDecimal("1000")); row.setBonuses(new BigDecimal("20")); row.setDeductions(new BigDecimal("10")); row.setTaxAmount(new BigDecimal("100")); row.setNetSalary(new BigDecimal("987.65"));
  when(service.getPayrollById(42L)).thenReturn(row);
  var bytes = mvc.perform(get("/api/payrolls/42/pdf").header("Authorization",auth("ADMIN")))
   .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
   .andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store")))
   .andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.containsString("attachment")))
   .andReturn().getResponse().getContentAsByteArray();
  try(var doc = Loader.loadPDF(bytes)) {
   assertEquals(595.28f,doc.getPage(0).getMediaBox().getWidth(),.1f);
   String text = new PDFTextStripper().getText(doc);
   assertTrue(text.contains("987,65")); assertTrue(text.contains("EMP-TEST"));
   assertTrue(text.contains("Donnée enregistrée"));
   Path dir = Path.of("target/pdf-examples"); Files.createDirectories(dir);
   Files.write(dir.resolve("payroll-synthetic.pdf"),bytes);
   ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0,96),"png",dir.resolve("payroll-synthetic.png").toFile());
  }
 }
 @Test void missingDocumentIs404() throws Exception {
  when(service.getPayrollById(99L)).thenThrow(new ResourceNotFoundException("Test record not found"));
  mvc.perform(get("/api/payrolls/99/pdf").header("Authorization",auth("ADMIN")).accept("application/pdf", "application/json"))
   .andExpect(status().isNotFound());
 }
 @Test void employeeAndClientAreForbiddenBeforeDataIsRead() throws Exception {
  for(String role:List.of("EMPLOYEE","CLIENT")) mvc.perform(get("/api/payrolls/42/pdf").header("Authorization",auth(role))).andExpect(status().isForbidden());
  verifyNoInteractions(service);
 }
 @Test void anonymousIsUnauthorized() throws Exception {
  mvc.perform(get("/api/payrolls/42/pdf")).andExpect(status().isUnauthorized()); verifyNoInteractions(service);
 }
 @Test void superAdminCanDownload() throws Exception {
  when(service.getPayrollById(42L)).thenReturn(new PayrollResponse());
  mvc.perform(get("/api/payrolls/42/pdf").header("Authorization",auth("SUPER_ADMIN"))).andExpect(status().isOk());
 }
}
