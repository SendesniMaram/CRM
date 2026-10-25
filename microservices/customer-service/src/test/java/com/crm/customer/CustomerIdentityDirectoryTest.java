package com.crm.customer;
import com.crm.customer.client.CustomerIdentityDirectory;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class CustomerIdentityDirectoryTest {
 HttpServer server;DiscoveryClient discovery;CustomerIdentityDirectory client;int status=200,hits;String bearer,path;
 @BeforeEach void setup()throws Exception{
  server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/",r->{hits++;path=r.getRequestURI().toString();bearer=r.getRequestHeaders().getFirst("Authorization");
   byte[] bytes="{\"id\":45,\"roles\":[\"CLIENT\"],\"enabled\":true,\"accountNonLocked\":true}".getBytes(StandardCharsets.UTF_8);
   r.getResponseHeaders().set("Content-Type","application/json");
   if(status==302)r.getResponseHeaders().set("Location","/other");
   r.sendResponseHeaders(status,bytes.length);r.getResponseBody().write(bytes);r.close();
  });server.start();discovery=mock(DiscoveryClient.class);
  when(discovery.getInstances("identity-service")).thenReturn(List.of(new DefaultServiceInstance("synthetic","identity-service","127.0.0.1",server.getAddress().getPort(),false)));
  client=new CustomerIdentityDirectory(discovery);
 }
 @AfterEach void close(){server.stop(0);}
 @Test void readsExplicitReferenceUsingAdminBearer(){assertEquals(45,client.read(45,"Bearer synthetic").path("id").asLong());assertEquals("/api/users/45",path);assertEquals("Bearer synthetic",bearer);assertEquals(1,hits);}
 @ParameterizedTest @ValueSource(ints={401,403,404,500,503,302})
 void failureNeverRetriesOrRedirects(int value){status=value;int expected=value==401||value==403||value==404?value:value==302?502:503;
  assertEquals(expected,assertThrows(ResponseStatusException.class,()->client.read(45,"Bearer synthetic")).getStatusCode().value());assertEquals(1,hits);}
 @Test void missingDiscoveryIsUnavailable(){when(discovery.getInstances("identity-service")).thenReturn(List.of());assertEquals(503,assertThrows(ResponseStatusException.class,()->client.read(45,"Bearer synthetic")).getStatusCode().value());}
}
