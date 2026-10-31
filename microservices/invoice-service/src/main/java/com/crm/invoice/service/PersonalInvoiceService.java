package com.crm.invoice.service;
import com.crm.invoice.client.PersonalCustomerClient;
import com.crm.invoice.entity.Invoice;
import com.crm.invoice.repository.InvoiceRepository;
import com.crm.invoice.repository.InvoicePaymentRepository;
import com.crm.security.AuthenticatedUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
@Service
public class PersonalInvoiceService {
 private final InvoiceRepository repository; private final PersonalCustomerClient customers; private final InvoicePaymentRepository payments;
 public PersonalInvoiceService(InvoiceRepository r,PersonalCustomerClient c,InvoicePaymentRepository p){repository=r;customers=c;payments=p;}
 public record Summary(String id,String invoiceNumber,LocalDate issueDate,LocalDate dueDate,String totalAmount,String currency,String status){}
 public record PageResult(java.util.List<Summary> content,long totalElements,int totalPages,int number){}
 public record MoneySummary(long invoiceCount,BigDecimal total,BigDecimal paid,BigDecimal outstanding){}
 public record PersonalStatistics(long invoiceCount,long paidInvoiceCount,long unpaidInvoiceCount,Map<String,MoneySummary> byCurrency){}
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
 public PersonalStatistics statistics(Authentication a,String bearer){
  String customerId=customer(a,bearer);
  var invoices=repository.summarizeByCustomerAndStatusAndCurrency(customerId);
  Map<String,BigDecimal> paidByCurrency=new TreeMap<>();
  // Payment totals are derived for this customer only by the repository query.
  for(var row:payments.summarizePaidByCustomerCurrency(customerId)) paidByCurrency.put(currency(row.getCurrency()),row.getAmount());
  Map<String,BigDecimal> totals=new TreeMap<>(); Map<String,Long> counts=new TreeMap<>();
  long totalCount=0,paidCount=0;
  for(var row:invoices){String key=currency(row.getCurrency());totals.merge(key,row.getTotal(),BigDecimal::add);counts.merge(key,row.getCount(),Long::sum);totalCount+=row.getCount();if("PAID".equalsIgnoreCase(row.getStatus()))paidCount+=row.getCount();}
  Map<String,MoneySummary> result=new TreeMap<>();
  totals.forEach((key,total)->{BigDecimal amount=paidByCurrency.getOrDefault(key,BigDecimal.ZERO);result.put(key,new MoneySummary(counts.getOrDefault(key,0L),total,amount,total.subtract(amount).max(BigDecimal.ZERO)));});
  return new PersonalStatistics(totalCount,paidCount,totalCount-paidCount,result);
 }
 private String currency(String value){return value==null||value.isBlank()?"NON_RENSEIGNÉE":value.trim();}
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
