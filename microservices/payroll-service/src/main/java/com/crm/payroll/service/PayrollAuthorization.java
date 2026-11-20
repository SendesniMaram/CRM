package com.crm.payroll.service;
import com.crm.security.AuthenticatedUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
public final class PayrollAuthorization {
 private PayrollAuthorization(){}
 public static long admin(Authentication authentication){
  var principal=AuthenticatedUserPrincipal.require(authentication);
  if(authentication.getAuthorities().stream().noneMatch(a->java.util.Set.of("ROLE_ADMIN","ROLE_SUPER_ADMIN").contains(a.getAuthority())))throw new AccessDeniedException("Administrative payroll access required");
  return principal.userId();
 }
}
