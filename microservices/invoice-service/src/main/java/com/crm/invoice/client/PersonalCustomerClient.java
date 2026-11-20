package com.crm.invoice.client;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
/** Delegates the user's own-profile request; never accepts a target customer. */
@Component
public class PersonalCustomerClient {
 private final DiscoveryClient discovery;
 public PersonalCustomerClient(DiscoveryClient d){discovery=d;}
 public String own(String bearer) {
  var instances=discovery.getInstances("customer-service");
  if(instances.isEmpty()) throw error(503);
  var f=new org.springframework.http.client.SimpleClientHttpRequestFactory(){
   @Override protected void prepareConnection(java.net.HttpURLConnection c,String m) throws java.io.IOException{
    super.prepareConnection(c,m); c.setInstanceFollowRedirects(false);
   }
  };
  f.setProxy(java.net.Proxy.NO_PROXY); f.setConnectTimeout(1500); f.setReadTimeout(1500);
  try{
   JsonNode p=RestClient.builder().requestFactory(f).baseUrl(instances.get(0).getUri().toString()).build().get()
    .uri("/api/customers/me").header(HttpHeaders.AUTHORIZATION,bearer).retrieve()
    .onStatus(s->s.is3xxRedirection(),(r,s)->{throw error(502);}).body(JsonNode.class);
   if(p==null || !p.isObject() || !p.path("customerId").isTextual()) throw error(502);
   String id=p.path("customerId").textValue();
   if(!id.matches("[1-9][0-9]{0,18}")) throw error(502);
   try{Long.parseLong(id);}catch(NumberFormatException e){throw error(502);}
   return id;
  }catch(RestClientResponseException e){
   int s=e.getStatusCode().value(); throw error(s==401||s==403||s==404?s:s>=500?503:502);
  }catch(ResourceAccessException e){throw error(503);}
  catch(RestClientException e){throw error(502);}
 }
 private static ResponseStatusException error(int s){return new ResponseStatusException(HttpStatus.valueOf(s),"Personal customer unavailable");}
}
