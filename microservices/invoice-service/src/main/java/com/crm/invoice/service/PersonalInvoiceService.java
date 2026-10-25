package com.crm.invoice.service;
import com.crm.invoice.client.PersonalCustomerClient;
import com.crm.invoice.entity.Invoice;
import com.crm.invoice.repository.InvoiceRepository;
import com.crm.security.AuthenticatedUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
@Service
public class PersonalInvoiceService {
 private final InvoiceRepository repository; private final PersonalCustomerClient customers;
 public PersonalInvoiceService(InvoiceRepository r,PersonalCustomerClient c){repository=r;customers=c;}
 public record Summary(String id,String invoiceNumber,LocalDate issueDate,LocalDate dueDate,String totalAmount,String currency,String status){}
 public record PageResult(java.util.List<Summary> content,long totalElements,int totalPages,int number){}
 private String customer(Authentication a,String bearer) {
  AuthenticatedUserPrincipal.require(a);
  if(a.getAuthorities().stream().noneMatch(x->x.getAuthority().equals("ROLE_CLIENT"))) throw new AccessDeniedException("Access denied");
  return customers.own(bearer);
 }
 public PageResult list(Authentication a,String bearer,int page,int size){
  if(page<0 || size<1 || size>100) throw new IllegalArgumentException("Invalid pagination");
  var p=repository.findByCustomerId(customer(a,bearer),PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"id")));
  return new PageResult(p.map(this::summary).getContent(),p.getTotalElements(),p.getTotalPages(),p.getNumber());
 }
 public Summary detail(Authentication a,String bearer,String id){
  long key=parseId(id);
  var row=repository.findByIdAndCustomerId(key,customer(a,bearer)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Invoice unavailable"));
  return summary(row);
 }
 private long parseId(String s){
  if(s==null || !s.matches("[1-9][0-9]{0,18}")) throw new IllegalArgumentException("Invalid identifier");
  try{return Long.parseLong(s);}catch(NumberFormatException e){throw new IllegalArgumentException("Invalid identifier");}
 }
 private Summary summary(Invoice r){return new Summary(r.getId().toString(),r.getInvoiceNumber(),r.getIssueDate(),r.getDueDate(),r.getTotalAmount().toPlainString(),r.getCurrency(),r.getStatus());}
}
