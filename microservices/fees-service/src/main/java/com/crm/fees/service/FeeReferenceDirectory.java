package com.crm.fees.service;
import com.fasterxml.jackson.databind.JsonNode;import org.springframework.cloud.client.discovery.DiscoveryClient;import org.springframework.stereotype.Component;import org.springframework.http.*;import org.springframework.web.client.*;import org.springframework.web.server.ResponseStatusException;
@Component public class FeeReferenceDirectory {
 private final DiscoveryClient discovery;public FeeReferenceDirectory(DiscoveryClient d){discovery=d;}
 public void validate(String employee,String customer,String bearer){require("employee-service","/api/employees/",employee,bearer);if(customer!=null&&!customer.isBlank())require("customer-service","/api/customers/",customer,bearer);}
 public void require(String service,String prefix,String id,String bearer){
  if(bearer==null||!bearer.startsWith("Bearer ")||bearer.substring(7).isBlank())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Bearer required");
  if(id==null||!id.matches("[1-9][0-9]{0,18}"))throw new IllegalArgumentException("Canonical employee/customer identifier required");long expected;try{expected=Long.parseLong(id);}catch(NumberFormatException e){throw new IllegalArgumentException("Identifier outside range");}
  JsonNode row=read(service,prefix+id,bearer);
  if(row==null||!row.isObject()||!row.path("id").isIntegralNumber()||!row.path("id").canConvertToLong()||row.path("id").longValue()!=expected)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Invalid reference response");
 }
 public JsonNode read(String service,String path,String bearer){
  var instances=discovery.getInstances(service);if(instances.isEmpty())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Reference directory unavailable");
  try{var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory(){@Override protected void prepareConnection(java.net.HttpURLConnection c,String method)throws java.io.IOException{super.prepareConnection(c,method);c.setInstanceFollowRedirects(false);}};factory.setConnectTimeout(2000);factory.setReadTimeout(2000);factory.setProxy(java.net.Proxy.NO_PROXY);
   return RestClient.builder().baseUrl(instances.getFirst().getUri().toString()).requestFactory(factory).build().get().uri(path).header(HttpHeaders.AUTHORIZATION,bearer).retrieve().onStatus(s->s.is3xxRedirection(),(a,b)->{throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Directory redirect refused");}).body(JsonNode.class);
  }catch(RestClientResponseException e){int code=e.getStatusCode().value();throw new ResponseStatusException(code==401?HttpStatus.UNAUTHORIZED:code==403?HttpStatus.FORBIDDEN:code==404?HttpStatus.NOT_FOUND:code>=500?HttpStatus.SERVICE_UNAVAILABLE:HttpStatus.BAD_GATEWAY,"Reference validation failed");}catch(ResourceAccessException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Reference directory unavailable");}catch(RestClientException e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Invalid reference response");}
 }
}