package com.crm.invoice;
import com.crm.invoice.controller.InvoicePdfController;
import com.crm.invoice.dto.InvoiceResponse;
import com.crm.invoice.service.IInvoiceService;
import com.crm.invoice.config.SecurityConfig;
import com.crm.invoice.exception.ResourceNotFoundException;
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
@WebMvcTest(InvoicePdfController.class)
@Import({SecurityConfig.class, JwtSecurityAutoConfiguration.class})
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class InvoicePdfTest extends com.crm.invoice.security.TestIdentitySupport {
 @Autowired MockMvc mvc;
 @Autowired JwtService jwt;
 @MockBean IInvoiceService service;
 String auth(String role) { return "Bearer " + com.crm.invoice.security.TestIdentitySupport.token("pdf-synthetic-test", "", true, List.of(role)); }
 @Test void rendersStoredAmountsAndFrenchAccentsAsA4Attachment() throws Exception {
  var row = new InvoiceResponse(); row.setId(42L); row.setCurrency("TND"); row.setStatus("PENDING");
  row.setInvoiceNumber("TEST-PDF-42"); row.setCustomerId("CLIENT-TEST"); row.setSubtotal(new BigDecimal("100")); row.setTax(new BigDecimal("19")); row.setTotalAmount(new BigDecimal("123.45")); row.setNotes("Démonstration synthétique - aucune donnée personnelle");
  when(service.getInvoiceById(42L)).thenReturn(row);
  var bytes = mvc.perform(get("/api/invoices/42/pdf").header("Authorization",auth("ADMIN")))
   .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
   .andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store")))
   .andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.containsString("attachment")))
   .andReturn().getResponse().getContentAsByteArray();
  try(var doc = Loader.loadPDF(bytes)) {
   assertEquals(595.28f,doc.getPage(0).getMediaBox().getWidth(),.1f);
   String text = new PDFTextStripper().getText(doc);
   assertTrue(text.contains("123,45")); assertTrue(text.contains("TEST-PDF-42"));
   assertTrue(text.contains("Donnée enregistrée"));
   Path dir = Path.of("target/pdf-examples"); Files.createDirectories(dir);
   Files.write(dir.resolve("invoice-synthetic.pdf"),bytes);
   ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0,96),"png",dir.resolve("invoice-synthetic.png").toFile());
  }
 }
 @Test void missingDocumentIs404() throws Exception {
  when(service.getInvoiceById(99L)).thenThrow(new ResourceNotFoundException("Test record not found"));
  mvc.perform(get("/api/invoices/99/pdf").header("Authorization",auth("ADMIN")).accept("application/pdf", "application/json"))
   .andExpect(status().isNotFound());
 }
 @Test void employeeAndClientAreForbiddenBeforeDataIsRead() throws Exception {
  for(String role:List.of("EMPLOYEE","CLIENT")) mvc.perform(get("/api/invoices/42/pdf").header("Authorization",auth(role))).andExpect(status().isForbidden());
  verifyNoInteractions(service);
 }
 @Test void anonymousIsUnauthorized() throws Exception {
  mvc.perform(get("/api/invoices/42/pdf")).andExpect(status().isUnauthorized()); verifyNoInteractions(service);
 }
 @Test void superAdminCanDownload() throws Exception {
  when(service.getInvoiceById(42L)).thenReturn(new InvoiceResponse());
  mvc.perform(get("/api/invoices/42/pdf").header("Authorization",auth("SUPER_ADMIN"))).andExpect(status().isOk());
 }
}
