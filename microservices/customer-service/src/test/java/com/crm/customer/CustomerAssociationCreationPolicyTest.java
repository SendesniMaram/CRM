package com.crm.customer;
import com.crm.customer.client.CustomerIdentityDirectory;
import com.crm.customer.entity.CustomerAssociation;
import com.crm.customer.repository.*;
import com.crm.customer.service.CustomerPersonalService;
import com.crm.security.AuthenticatedUserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class CustomerAssociationCreationPolicyTest {
 final CustomerAssociationRepository links=mock(CustomerAssociationRepository.class);
 final CustomerRepository customers=mock(CustomerRepository.class);
 final CustomerIdentityDirectory directory=mock(CustomerIdentityDirectory.class);
 final PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
 final CustomerPersonalService service=new CustomerPersonalService(links,customers,directory,transactions);
 final List<SimpleGrantedAuthority> roles=List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
 final UsernamePasswordAuthenticationToken admin=new UsernamePasswordAuthenticationToken(new AuthenticatedUserPrincipal(1,"admin",roles),null,roles);
 @BeforeEach void setup()throws Exception{user("CLIENT",true,true);when(customers.existsById(101L)).thenReturn(true);when(transactions.getTransaction(any())).thenReturn(mock(TransactionStatus.class));}
 void user(String role,boolean enabled,boolean unlocked)throws Exception{when(directory.read(42L,"token")).thenReturn(new ObjectMapper().readTree("{\"id\":42,\"roles\":[\""+role+"\"],\"enabled\":"+enabled+",\"accountNonLocked\":"+unlocked+"}"));}
 @Test void creationUsesInsertNeverMerge(){assertEquals("101",service.create(admin,"42","101","token").customerId());verify(links).insertNew(42L,101L);verify(links,never()).saveAndFlush(any());}
 @Test void concurrentInsertConflictRollsBack(){doThrow(new org.springframework.dao.DataIntegrityViolationException("private SQL")).when(links).insertNew(42L,101L);assertEquals(409,assertThrows(ResponseStatusException.class,()->service.create(admin,"42","101","token")).getStatusCode().value());verify(transactions).rollback(any());verify(transactions,never()).commit(any());}
 @Test void sameAssociationIsIdempotent(){when(links.findById(42L)).thenReturn(Optional.of(new CustomerAssociation(42L,101L)));service.create(admin,"42","101","token");verify(links,never()).insertNew(any(),any());}
 @Test void differentAssociationConflicts(){when(links.findById(42L)).thenReturn(Optional.of(new CustomerAssociation(42L,202L)));assertEquals(409,assertThrows(ResponseStatusException.class,()->service.create(admin,"42","101","token")).getStatusCode().value());}
 @Test void missingCustomerDoesNotWrite(){when(customers.existsById(101L)).thenReturn(false);assertEquals(404,assertThrows(ResponseStatusException.class,()->service.create(admin,"42","101","token")).getStatusCode().value());verifyNoInteractions(links);}
 @Test void inactiveAccountDoesNotWrite()throws Exception{user("CLIENT",false,true);rejected();}
 @Test void lockedAccountDoesNotWrite()throws Exception{user("CLIENT",true,false);rejected();}
 @Test void incorrectRoleDoesNotWrite()throws Exception{user("EMPLOYEE",true,true);rejected();}
 void rejected(){assertEquals(400,assertThrows(ResponseStatusException.class,()->service.create(admin,"42","101","token")).getStatusCode().value());verifyNoInteractions(links);}
 @Test void unavailableIdentityDoesNotWrite(){when(directory.read(anyLong(),anyString())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));assertEquals(503,assertThrows(ResponseStatusException.class,()->service.create(admin,"42","101","token")).getStatusCode().value());verifyNoInteractions(links,customers);}
}
