package com.crm.customer.client;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
@Component
public class CustomerIdentityDirectory {
 private final DiscoveryClient discovery;
 public CustomerIdentityDirectory(DiscoveryClient discovery) { this.discovery=discovery; }
 public JsonNode read(long uid, String bearer) {
  var instances=discovery.getInstances("identity-service");
  if(instances.isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Directory unavailable");
  var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory() {
   @Override protected void prepareConnection(java.net.HttpURLConnection c,String method) throws java.io.IOException {
    super.prepareConnection(c,method); c.setInstanceFollowRedirects(false);
   }
  };
  factory.setProxy(java.net.Proxy.NO_PROXY); factory.setConnectTimeout(1500); factory.setReadTimeout(1500);
  try {
   return RestClient.builder().requestFactory(factory).baseUrl(instances.get(0).getUri().toString()).build()
    .get().uri("/api/users/"+uid).header(HttpHeaders.AUTHORIZATION,bearer).retrieve()
    .onStatus(s->s.is3xxRedirection(),(r,s)->{throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Invalid directory response");})
    .body(JsonNode.class);
  } catch(RestClientResponseException e) {
   int s=e.getStatusCode().value();
   throw new ResponseStatusException(s==404?HttpStatus.NOT_FOUND:s==401?HttpStatus.UNAUTHORIZED:s==403?HttpStatus.FORBIDDEN:HttpStatus.SERVICE_UNAVAILABLE,"Directory unavailable");
  } catch(ResourceAccessException e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Directory unavailable"); }
  catch(RestClientException e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Invalid directory response"); }
 }
}
