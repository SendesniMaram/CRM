package com.crm.customer.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CustomerEmployeeDirectory {
 private final DiscoveryClient discovery;
 public CustomerEmployeeDirectory(DiscoveryClient discovery) { this.discovery=discovery; }
 public void requireExisting(long employeeId, String bearer) {
  var instances=discovery.getInstances("employee-service");
  if(instances.isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Employee directory unavailable");
  var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory(); factory.setProxy(java.net.Proxy.NO_PROXY); factory.setConnectTimeout(1500); factory.setReadTimeout(1500);
  try {
   JsonNode result=RestClient.builder().requestFactory(factory).baseUrl(instances.get(0).getUri().toString()).build().get().uri("/api/employees/{id}",employeeId).header(HttpHeaders.AUTHORIZATION,bearer).retrieve().body(JsonNode.class);
   if(result==null || result.path("id").asLong(-1)!=employeeId) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Invalid employee directory response");
  } catch(RestClientResponseException e) {
   int s=e.getStatusCode().value(); throw new ResponseStatusException(s==404?HttpStatus.BAD_REQUEST:s==401?HttpStatus.UNAUTHORIZED:s==403?HttpStatus.FORBIDDEN:HttpStatus.SERVICE_UNAVAILABLE,"Employee association could not be verified");
  } catch(ResourceAccessException e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Employee directory unavailable"); }
  catch(RestClientException e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Invalid employee directory response"); }
 }
}
