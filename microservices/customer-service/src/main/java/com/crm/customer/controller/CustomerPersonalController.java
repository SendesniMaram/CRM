package com.crm.customer.controller;
import com.crm.customer.service.CustomerPersonalService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.*;
import jakarta.servlet.http.HttpServletRequest;
@RestController
public class CustomerPersonalController {
 private final CustomerPersonalService service;
 public CustomerPersonalController(CustomerPersonalService service){this.service=service;}
 public record Creation(String userId,String customerId){}
 @GetMapping("/api/customers/me")
 public ResponseEntity<CustomerPersonalService.Profile> me(Authentication a,HttpServletRequest request) {
  if(!request.getParameterMap().isEmpty()) throw new IllegalArgumentException("Target parameters are not allowed");
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.own(a));
 }
 @GetMapping("/api/customers/associations/users/{uid}")
 public ResponseEntity<CustomerPersonalService.Association> read(Authentication a,@PathVariable String uid) {
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.read(a,uid));
 }
 @PostMapping("/api/customers/associations")
 public ResponseEntity<CustomerPersonalService.Association> create(Authentication a,@RequestBody Creation r,@RequestHeader("Authorization") String bearer) {
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.create(a,r.userId(),r.customerId(),bearer));
 }
}
