package com.crm.invoice;
import com.crm.invoice.client.PersonalCustomerClient;
import com.crm.invoice.entity.Invoice;
import com.crm.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"eureka.client.enabled=false","spring.cloud.service-registry.auto-registration.enabled=false"})
@AutoConfigureMockMvc
class PersonalInvoiceIntegrationTest extends com.crm.invoice.security.TestIdentitySupport {
 @Autowired MockMvc mvc;@Autowired InvoiceRepository invoices;@MockBean PersonalCustomerClient customers;
 String a,b;Long own,foreign;
 String bearer(long uid,String username,String role){return "Bearer "+signed(username,true,List.of(role),""+uid,Instant.now().plusSeconds(3600));}
 @BeforeEach void data(){
  invoices.deleteAll();a=bearer(42,"same","CLIENT");b=bearer(43,"same","CLIENT");
  when(customers.own(a)).thenReturn("101");when(customers.own(b)).thenReturn("202");
  own=save("A","101");foreign=save("B","202");
 }
 Long save(String number,String customer){
  Invoice i=new Invoice();i.setInvoiceNumber(number);i.setCustomerId(customer);i.setSubtotal(BigDecimal.TEN);i.setTotalAmount(BigDecimal.TEN);
  i.setStatus("PENDING");i.setCurrency("EUR");i.setNotes("Private internal notes");return invoices.saveAndFlush(i).getId();
 }
 @Test void listIsFilteredAndPaged()throws Exception{
  mvc.perform(get("/api/invoices/me").header("Authorization",a).header("X-Customer-Id","202")).andExpect(status().isOk())
   .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.totalElements").value(1))
   .andExpect(jsonPath("$.content[0].invoiceNumber").value("A")).andExpect(jsonPath("$.content[0].notes").doesNotExist());
  mvc.perform(get("/api/invoices/me?page=0&size=1").header("Authorization",b)).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].invoiceNumber").value("B"));
 }
 @Test void detailAndPdfEnforceOwnership()throws Exception{
  mvc.perform(get("/api/invoices/me/"+own).header("Authorization",a)).andExpect(status().isOk());
  mvc.perform(get("/api/invoices/me/"+foreign).header("Authorization",a)).andExpect(status().isNotFound());
  mvc.perform(get("/api/invoices/me/"+foreign+"/pdf").header("Authorization",a)).andExpect(status().isNotFound());
  mvc.perform(get("/api/invoices/me/"+own+"/pdf").header("Authorization",a)).andExpect(status().isOk())
   .andExpect(content().contentType("application/pdf")).andExpect(header().string("Cache-Control","no-store"));
 }
 @Test void renamedUidKeepsScopeAndReusedNameDoesNotTransfer()throws Exception{
  String renamed=bearer(42,"renamed","CLIENT");when(customers.own(renamed)).thenReturn("101");
  mvc.perform(get("/api/invoices/me/"+own).header("Authorization",renamed)).andExpect(status().isOk());
  mvc.perform(get("/api/invoices/me/"+own).header("Authorization",b)).andExpect(status().isNotFound());
 }
 @ParameterizedTest @ValueSource(strings={"ADMIN","SUPER_ADMIN","EMPLOYEE"})
 void noNewPermissionsForOtherRoles(String role)throws Exception{
  mvc.perform(get("/api/invoices/me").header("Authorization",bearer(42,"x",role))).andExpect(status().isForbidden());
 }
 @Test void missingAndUnavailableAssociationFailClosed()throws Exception{
  when(customers.own(a)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
  mvc.perform(get("/api/invoices/me").header("Authorization",a)).andExpect(status().isNotFound());
  doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).when(customers).own(a);
  mvc.perform(get("/api/invoices/me/"+own+"/pdf").header("Authorization",a)).andExpect(status().isServiceUnavailable());
 }
 @Test void targetsAndMalformedIdsRefused()throws Exception{
  mvc.perform(get("/api/invoices/me?customerId=202").header("Authorization",a)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/invoices/me/01").header("Authorization",a)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/invoices/me?size=101").header("Authorization",a)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/invoices/me")).andExpect(status().isUnauthorized());
 }
}
