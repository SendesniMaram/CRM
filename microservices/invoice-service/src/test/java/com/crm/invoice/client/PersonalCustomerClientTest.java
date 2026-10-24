package com.crm.invoice.client;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class PersonalCustomerClientTest {
 HttpServer server; DiscoveryClient discovery; PersonalCustomerClient client;
 String body="{\"customerId\":\"9007199254740993\"}";int status=200;String path,bearer;AtomicInteger hits=new AtomicInteger();
 @BeforeEach void setup()throws Exception{
  server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/",r->{hits.incrementAndGet();path=r.getRequestURI().toString();bearer=r.getRequestHeaders().getFirst("Authorization");
   byte[] bytes=body.getBytes(StandardCharsets.UTF_8);r.getResponseHeaders().set("Content-Type","application/json");
   if(status==302)r.getResponseHeaders().set("Location","http://127.0.0.1:"+server.getAddress().getPort()+"/redirected");
   r.sendResponseHeaders(status,bytes.length);r.getResponseBody().write(bytes);r.close();
  });server.start();discovery=mock(DiscoveryClient.class);
  when(discovery.getInstances("customer-service")).thenReturn(List.of(new DefaultServiceInstance("test","customer-service","127.0.0.1",server.getAddress().getPort(),false)));
  client=new PersonalCustomerClient(discovery);
 }
 @AfterEach void stop(){server.stop(0);}
 @Test void delegatesOnlyOwnProfileAndPreservesLongId(){assertEquals("9007199254740993",client.own("Bearer synthetic"));assertEquals("/api/customers/me",path);assertEquals("Bearer synthetic",bearer);assertEquals(1,hits.get());}
 @ParameterizedTest @ValueSource(strings={"{\"customerId\":1}","{\"customerId\":\"01\"}","{\"customerId\":\"9223372036854775808\"}","null","[]","invalid"})
 void malformedContractFailsClosed(String value){body=value;assertEquals(502,assertThrows(ResponseStatusException.class,()->client.own("Bearer synthetic")).getStatusCode().value());}
 @ParameterizedTest @ValueSource(ints={401,403,404,500,503,302})
 void errorsDoNotRetryOrRedirect(int value){status=value;int expected=value==401||value==403||value==404?value:value>=500?503:502;
  assertEquals(expected,assertThrows(ResponseStatusException.class,()->client.own("Bearer synthetic")).getStatusCode().value());assertEquals(1,hits.get());}
 @Test void noDiscoveryFailsUnavailable(){when(discovery.getInstances("customer-service")).thenReturn(List.of());assertEquals(503,assertThrows(ResponseStatusException.class,()->client.own("Bearer synthetic")).getStatusCode().value());}
}
