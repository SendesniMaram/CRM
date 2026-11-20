package com.crm.customer;
import com.crm.customer.client.CustomerIdentityDirectory;
import com.crm.customer.entity.CustomerAssociation;
import com.crm.customer.repository.*;
import com.crm.customer.service.CustomerPersonalService;
import com.crm.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class CustomerPersonalPolicyTest {
 CustomerAssociationRepository links;CustomerRepository customers;CustomerPersonalService service;
 @BeforeEach void setup(){links=mock(CustomerAssociationRepository.class);customers=mock(CustomerRepository.class);
  service=new CustomerPersonalService(links,customers,mock(CustomerIdentityDirectory.class),mock(PlatformTransactionManager.class));}
 @Test void untypedPrincipalCannotUseUsernameFallback(){
  var auth=new UsernamePasswordAuthenticationToken("same-name",null,List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
  assertThrows(AccessDeniedException.class,()->service.own(auth));verifyNoInteractions(links,customers);
 }
 @Test void inconsistentAssociationFailsBeforeCustomerRead(){
  var authorities=List.of(new SimpleGrantedAuthority("ROLE_CLIENT"));
  var auth=new UsernamePasswordAuthenticationToken(new AuthenticatedUserPrincipal(42,"same",authorities),null,authorities);
  when(links.findById(42L)).thenReturn(Optional.of(new CustomerAssociation(43L,101L)));
  assertEquals(502,assertThrows(ResponseStatusException.class,()->service.own(auth)).getStatusCode().value());verifyNoInteractions(customers);
 }
 @ParameterizedTest @ValueSource(strings={"0","01","-1","1e2"," 1","9223372036854775808"})
 void malformedIdentifiersRejected(String value){assertThrows(IllegalArgumentException.class,()->CustomerPersonalService.id(value));}
}
