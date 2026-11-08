package com.crm.invoice;
import com.crm.invoice.service.PersonalInvoiceService;
import com.crm.invoice.client.PersonalCustomerClient;
import com.crm.invoice.entity.*;
import com.crm.invoice.repository.*;
import com.crm.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersonalPaymentPolicyTest {
 final InvoiceRepository invoices=mock(InvoiceRepository.class);
 final InvoicePaymentRepository payments=mock(InvoicePaymentRepository.class);
 final PersonalCustomerClient customers=mock(PersonalCustomerClient.class);
 final PersonalInvoiceService service=new PersonalInvoiceService(invoices,customers,payments);
 final List<SimpleGrantedAuthority> roles=List.of(new SimpleGrantedAuthority("ROLE_CLIENT"));
 final UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken(new AuthenticatedUserPrincipal(42,"client",roles),null,roles);
 @BeforeEach void setup(){when(customers.own("Bearer token")).thenReturn("101");}
 @Test void paymentHistoryRequiresOwnedInvoice()throws Exception{
  when(invoices.findByIdAndCustomerId(12L,"101")).thenReturn(Optional.of(new Invoice()));
  var p=new InvoicePayment();p.setId(9L);p.setAmount(new BigDecimal("12.30"));p.setNotes("private-note");p.setReference("private-reference");
  when(payments.findByInvoiceIdOrderByPaidAtDescIdDesc(12L)).thenReturn(List.of(p));
  var result=service.payments(auth,"Bearer token","12");assertEquals("12.30",result.get(0).amount());
  String json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result);assertFalse(json.contains("private"));
 }
 @Test void foreignAndMissingInvoiceAreIndistinguishable(){assertEquals(404,assertThrows(ResponseStatusException.class,()->service.payments(auth,"Bearer token","12")).getStatusCode().value());verifyNoInteractions(payments);}
 @Test void invalidIdentifierNeverCallsServices(){assertThrows(IllegalArgumentException.class,()->service.payments(auth,"Bearer token","01"));verifyNoInteractions(customers,invoices,payments);}
 @Test void wrongRoleCannotResolveCustomer(){var other=new UsernamePasswordAuthenticationToken(new AuthenticatedUserPrincipal(42,"admin",List.of()),null,List.of());assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.payments(other,"Bearer token","12"));verifyNoInteractions(customers,invoices,payments);}
 @Test void directoryUnavailableDoesNotReadPayments(){when(customers.own(anyString())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));assertEquals(503,assertThrows(ResponseStatusException.class,()->service.payments(auth,"Bearer token","12")).getStatusCode().value());verifyNoInteractions(invoices,payments);}
 @Test void ownedInvoiceWithoutPaymentsIsEmpty(){when(invoices.findByIdAndCustomerId(12L,"101")).thenReturn(Optional.of(new Invoice()));assertTrue(service.payments(auth,"Bearer token","12").isEmpty());}
}
