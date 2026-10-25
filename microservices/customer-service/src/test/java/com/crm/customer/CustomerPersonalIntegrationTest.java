package com.crm.customer;
import com.crm.customer.client.CustomerIdentityDirectory;
import com.crm.customer.entity.*;
import com.crm.customer.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"eureka.client.enabled=false","spring.cloud.service-registry.auto-registration.enabled=false"})
@AutoConfigureMockMvc
class CustomerPersonalIntegrationTest extends com.crm.customer.security.TestIdentitySupport {
 @Autowired MockMvc mvc; @Autowired CustomerRepository customers; @Autowired CustomerAssociationRepository links;
 @Autowired ObjectMapper json; @MockBean CustomerIdentityDirectory directory;
 Long a,b;
 String bearer(long uid,String name,String role){return "Bearer "+signed(name,true,List.of(role),""+uid,Instant.now().plusSeconds(3600));}
 @BeforeEach void data(){
  links.deleteAll();customers.deleteAll();
  Customer x=new Customer();x.setName("Customer A");x.setEmail("a@example.test");a=customers.saveAndFlush(x).getId();
  Customer y=new Customer();y.setName("Customer B");y.setEmail("b@example.test");b=customers.saveAndFlush(y).getId();
  links.saveAndFlush(new CustomerAssociation(42L,a));links.saveAndFlush(new CustomerAssociation(43L,b));
 }
 @Test void ownIsolationAndMinimalProfile()throws Exception{
  mvc.perform(get("/api/customers/me").header("Authorization",bearer(42,"same","CLIENT")).header("X-User-Id","43"))
   .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
   .andExpect(jsonPath("$.customerId").value(a.toString())).andExpect(jsonPath("$.name").value("Customer A"))
   .andExpect(jsonPath("$.address").doesNotExist()).andExpect(jsonPath("$.userId").doesNotExist());
  mvc.perform(get("/api/customers/me").header("Authorization",bearer(43,"same","CLIENT")))
   .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Customer B"));
 }
 @Test void renameKeepsUidAndReusedUsernameHasNoAccess()throws Exception{
  mvc.perform(get("/api/customers/me").header("Authorization",bearer(42,"renamed","CLIENT"))).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Customer A"));
  mvc.perform(get("/api/customers/me").header("Authorization",bearer(44,"same","CLIENT"))).andExpect(status().isNotFound());
 }
 @ParameterizedTest @ValueSource(strings={"ADMIN","SUPER_ADMIN","EMPLOYEE"})
 void personalEndpointDoesNotExtendOtherRoles(String role)throws Exception{
  mvc.perform(get("/api/customers/me").header("Authorization",bearer(42,"user",role))).andExpect(status().isForbidden());
 }
 @Test void targetParametersAndUnauthenticatedDenied()throws Exception{
  mvc.perform(get("/api/customers/me?customerId="+b).header("Authorization",bearer(42,"user","CLIENT"))).andExpect(status().isBadRequest());
  mvc.perform(get("/api/customers/me")).andExpect(status().isUnauthorized());
 }
 @Test void creationIdempotenceConflictAndEligibility()throws Exception{
  when(directory.read(eq(45L),anyString())).thenReturn(json.readTree("{\"id\":45,\"roles\":[\"CLIENT\"],\"enabled\":true,\"accountNonLocked\":true}"));
  Customer c=new Customer();c.setName("C");Long cid=customers.saveAndFlush(c).getId();
  String body="{\"userId\":\"45\",\"customerId\":\""+cid+"\"}";
  for(int i=0;i<2;i++)mvc.perform(post("/api/customers/associations").header("Authorization",bearer(50,"admin","ADMIN")).contentType("application/json").content(body)).andExpect(status().isOk());
  mvc.perform(post("/api/customers/associations").header("Authorization",bearer(50,"admin","SUPER_ADMIN")).contentType("application/json")
   .content("{\"userId\":\"45\",\"customerId\":\""+a+"\"}")).andExpect(status().isConflict());
  org.junit.jupiter.api.Assertions.assertEquals(3,links.count());
 }
 @Test void disabledClientAndWrongRoleNeverWritten()throws Exception{
  for(String data:List.of("{\"id\":45,\"roles\":[\"CLIENT\"],\"enabled\":false,\"accountNonLocked\":true}",
   "{\"id\":45,\"roles\":[\"EMPLOYEE\"],\"enabled\":true,\"accountNonLocked\":true}")){
   when(directory.read(eq(45L),anyString())).thenReturn(json.readTree(data));
   mvc.perform(post("/api/customers/associations").header("Authorization",bearer(50,"admin","ADMIN")).contentType("application/json")
    .content("{\"userId\":\"45\",\"customerId\":\""+a+"\"}")).andExpect(status().isBadRequest());
  }
  org.junit.jupiter.api.Assertions.assertFalse(links.existsById(45L));
 }
 @Test void malformedDirectoryFailsClosed()throws Exception{
  when(directory.read(eq(45L),anyString())).thenReturn(json.readTree("{\"id\":45,\"roleType\":\"CLIENT\"}"));
  mvc.perform(post("/api/customers/associations").header("Authorization",bearer(50,"admin","ADMIN")).contentType("application/json")
   .content("{\"userId\":\"45\",\"customerId\":\""+a+"\"}")).andExpect(status().isBadGateway());
 }
 @Test void clientCannotReadOrCreateAdministrativeAssociation()throws Exception{
  mvc.perform(get("/api/customers/associations/users/43").header("Authorization",bearer(42,"client","CLIENT"))).andExpect(status().isForbidden());
  mvc.perform(post("/api/customers/associations").header("Authorization",bearer(42,"client","CLIENT")).contentType("application/json")
   .content("{\"userId\":\"42\",\"customerId\":\""+a+"\"}")).andExpect(status().isForbidden());
 }
 @Test void danglingAssociationDoesNotReturnOtherCustomer()throws Exception{
  links.deleteById(42L);links.saveAndFlush(new CustomerAssociation(42L,999999L));
  mvc.perform(get("/api/customers/me").header("Authorization",bearer(42,"client","CLIENT"))).andExpect(status().isNotFound());
 }
 @Test void migrationIsAdditiveAndEnforcesUniqueReferences()throws Exception{
  try(var c=java.sql.DriverManager.getConnection("jdbc:h2:mem:customer_migration;MODE=MySQL","sa","")){
   c.createStatement().execute("create table customers(id bigint primary key, name varchar(100))");
   c.createStatement().execute("insert into customers values(1,'Preserved')");
   String script=new String(getClass().getResourceAsStream("/db/migration/V20261023_01__customer_user_associations.sql").readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
   c.createStatement().execute(script);c.createStatement().execute(script);
   c.createStatement().execute("insert into customer_user_associations values(42,1)");
   org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class,()->c.createStatement().execute("insert into customer_user_associations values(43,1)"));
   org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class,()->c.createStatement().execute("insert into customer_user_associations values(44,999)"));
   var r=c.createStatement().executeQuery("select name from customers where id=1");r.next();org.junit.jupiter.api.Assertions.assertEquals("Preserved",r.getString(1));
  }
 }
}
