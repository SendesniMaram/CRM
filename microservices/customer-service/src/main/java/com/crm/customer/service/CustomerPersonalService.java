package com.crm.customer.service;
import com.crm.customer.client.CustomerIdentityDirectory;
import com.crm.customer.entity.CustomerAssociation;
import com.crm.customer.repository.*;
import com.crm.security.AuthenticatedUserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class CustomerPersonalService {
 private final CustomerAssociationRepository associations; private final CustomerRepository customers;
 private final CustomerIdentityDirectory identity; private final TransactionTemplate writes;
 public CustomerPersonalService(CustomerAssociationRepository a,CustomerRepository c,CustomerIdentityDirectory i,PlatformTransactionManager t) {
  associations=a; customers=c; identity=i; writes=new TransactionTemplate(t);
 }
 public record Association(String userId,String customerId) {}
 public record Profile(String customerId,String name,String email,String phone,String company) {}
 public static long id(String v) {
  if(v==null || !v.matches("[1-9][0-9]{0,18}")) throw new IllegalArgumentException("Canonical identifier required");
  try{return Long.parseLong(v);}catch(NumberFormatException e){throw new IllegalArgumentException("Invalid identifier");}
 }
 public static void role(Authentication a,String... roles) {
  AuthenticatedUserPrincipal.require(a);
  if(a.getAuthorities().stream().noneMatch(x->java.util.Arrays.asList(roles).contains(x.getAuthority())))
   throw new AccessDeniedException("Access denied");
 }
 public Association read(Authentication a,String uid) {role(a,"ROLE_ADMIN","ROLE_SUPER_ADMIN"); return map(find(id(uid)));}
 public Association create(Authentication a,String user,String customer,String bearer) {
  role(a,"ROLE_ADMIN","ROLE_SUPER_ADMIN"); long uid=id(user), cid=id(customer);
  JsonNode u=identity.read(uid,bearer);
  if(u==null || !u.isObject() || !u.path("id").isIntegralNumber() || !u.path("id").canConvertToLong()
   || u.path("id").longValue()!=uid || !u.path("roles").isArray() || !u.path("enabled").isBoolean()
   || !u.path("accountNonLocked").isBoolean()) throw status(502);
  boolean client=false;
  for(JsonNode r:u.path("roles")) {
   if(!r.isTextual() || !java.util.Set.of("CLIENT","EMPLOYEE","ADMIN","SUPER_ADMIN").contains(r.textValue())) throw status(502);
   client |= r.textValue().equals("CLIENT");
  }
  if(!client || !u.path("enabled").booleanValue() || !u.path("accountNonLocked").booleanValue()) throw status(400);
  if(!customers.existsById(cid)) throw status(404);
  try {return writes.execute(t->{
   var old=associations.findById(uid);
   if(old.isPresent()) {if(!old.get().getCustomerId().equals(cid)) throw status(409); return map(old.get());}
   if(associations.existsByCustomerId(cid)) throw status(409);
   return map(associations.saveAndFlush(new CustomerAssociation(uid,cid)));
  });}catch(DataIntegrityViolationException e){throw status(409);}
 }
 public Profile own(Authentication a) {
  role(a,"ROLE_CLIENT"); long uid=AuthenticatedUserPrincipal.require(a).userId();
  var link=find(uid);
  if(link.getUserId()==null || link.getUserId()!=uid || link.getCustomerId()==null || link.getCustomerId()<=0) throw status(502);
  var c=customers.findById(link.getCustomerId()).orElseThrow(()->status(404));
  return new Profile(c.getId().toString(),c.getName(),c.getEmail(),c.getPhone(),c.getCompany());
 }
 private CustomerAssociation find(long uid){return associations.findById(uid).orElseThrow(()->status(404));}
 private Association map(CustomerAssociation a){return new Association(a.getUserId().toString(),a.getCustomerId().toString());}
 private static ResponseStatusException status(int s){return new ResponseStatusException(HttpStatus.valueOf(s),"Customer access unavailable");}
}
