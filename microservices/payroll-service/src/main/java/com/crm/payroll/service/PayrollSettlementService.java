package com.crm.payroll.service;
import com.crm.payroll.entity.*;
import com.crm.payroll.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional
public class PayrollSettlementService {
 private final PayrollRepository payrolls;private final PayrollSettlementRepository entries;private final PersonalPayrollService personal;
 @PersistenceContext private EntityManager entityManager;
 public PayrollSettlementService(PayrollRepository payrolls,PayrollSettlementRepository entries,PersonalPayrollService personal){this.payrolls=payrolls;this.entries=entries;this.personal=personal;}
 public record PaymentRequest(@NotBlank String requestId,@NotNull @Positive @Digits(integer=10,fraction=2) BigDecimal amount,
   @NotNull LocalDate paidAt,@Size(max=50) String method,@Size(max=100) String transactionReference){}
 public record ReversalRequest(@NotBlank String requestId,@NotBlank @Size(max=500) String reason){}
 public record Entry(String reference,String kind,String amount,LocalDate date,String method,String transactionReference,String reversesReference,String reason,String author,LocalDateTime createdAt){}
 public record Summary(String paidAmount,String remainingAmount,String state,List<Entry> entries,String correctionReason,String correctedBy,LocalDateTime correctedAt){}
 @Transactional(readOnly=true) public Summary administration(Authentication auth,long id){PayrollAuthorization.admin(auth);return summary(get(id),false);}
 @Transactional(readOnly=true) public Summary own(Authentication auth,String bearer,long id){personal.read(auth,bearer,id);return summary(get(id),true);}
 public Summary pay(Authentication auth,long id,PaymentRequest request){
  long author=PayrollAuthorization.admin(auth);FinancialValidation.request(request);uuid(request.requestId());
  Payroll row=locked(id);List<PayrollSettlement> history=history(id);
  var previous=entries.findByRequestId(request.requestId());
  if(previous.isPresent()){
   var e=previous.get();
   if(e.getPayrollId()!=id||!"PAYMENT".equals(e.getKind())||e.getAmount().compareTo(request.amount())!=0||!e.getPaidAt().equals(request.paidAt())
     ||!Objects.equals(e.getMethod(),trim(request.method()))||!Objects.equals(e.getTransactionReference(),trim(request.transactionReference())))throw conflict();
   return summary(row,false);
  }
  if(!row.isPublished()||row.isCancelled()||!Set.of("PENDING","PAID").contains(row.getStatus())||PayrollLedger.legacy(row,history))throw conflict();
  BigDecimal remaining=PayrollLedger.remaining(row,history);
  if(remaining==null||request.amount().compareTo(remaining)>0)throw conflict();
  String transaction=trim(request.transactionReference());
  if(transaction!=null&&entries.existsByPayrollIdAndTransactionReference(id,transaction))throw conflict();
  var entry=new PayrollSettlement();entry.setPayrollId(id);entry.setInternalReference(UUID.randomUUID().toString());entry.setRequestId(request.requestId());
  entry.setKind("PAYMENT");entry.setAmount(request.amount());entry.setPaidAt(request.paidAt());entry.setMethod(trim(request.method()));entry.setTransactionReference(transaction);entry.setCreatedBy(author);
  entries.saveAndFlush(entry);history.add(entry);updateStatus(row,history);
  return summary(row,false);
 }
 public Summary reverse(Authentication auth,long id,String reference,ReversalRequest request){
  long author=PayrollAuthorization.admin(auth);FinancialValidation.request(request);uuid(request.requestId());uuid(reference);
  Payroll row=locked(id);List<PayrollSettlement> history=history(id);
  var original=history.stream().filter(e->reference.equals(e.getInternalReference())&&"PAYMENT".equals(e.getKind())).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Settlement unavailable"));
  var previous=entries.findByRequestId(request.requestId());
  if(previous.isPresent()){
   var e=previous.get();if(e.getPayrollId()!=id||!"REVERSAL".equals(e.getKind())||!Objects.equals(e.getReversesId(),original.getId())||!Objects.equals(e.getReason(),request.reason().trim()))throw conflict();
   return summary(row,false);
  }
  if(row.isCancelled()||entries.existsByReversesId(original.getId()))throw conflict();
  var reversal=new PayrollSettlement();reversal.setPayrollId(id);reversal.setInternalReference(UUID.randomUUID().toString());reversal.setRequestId(request.requestId());reversal.setKind("REVERSAL");reversal.setAmount(original.getAmount());
  reversal.setPaidAt(LocalDate.now());reversal.setReversesId(original.getId());reversal.setReason(request.reason().trim());reversal.setCreatedBy(author);
  entries.saveAndFlush(reversal);history.add(reversal);updateStatus(row,history);
  return summary(row,false);
 }
 private void updateStatus(Payroll row,List<PayrollSettlement> history){
  BigDecimal paid=PayrollLedger.paid(history);if(paid.signum()<0||paid.compareTo(row.getNetSalary())>0)throw conflict();
  boolean settled=paid.signum()>0&&paid.compareTo(row.getNetSalary())==0;
  row.setStatus(settled?"PAID":"PENDING");
  row.setPaymentDate(settled?history.stream().filter(e->"PAYMENT".equals(e.getKind())&&!history.stream().anyMatch(r->Objects.equals(r.getReversesId(),e.getId())))
    .map(PayrollSettlement::getPaidAt).max(LocalDate::compareTo).orElse(null):null);
 }
 private Summary summary(Payroll row,boolean own){
  List<PayrollSettlement> history=history(row.getId());BigDecimal paid=PayrollLedger.paid(history),remaining=PayrollLedger.remaining(row,history);
  String state=row.isCancelled()?"CANCELLED":remaining==null?"LEGACY_UNRECONCILED":row.getNetSalary().signum()==0?"NO_AMOUNT_DUE":remaining.signum()==0?"SETTLED":paid.signum()>0?"PARTIAL":"UNPAID";
  List<Entry> result=history.stream().map(e->new Entry(e.getInternalReference(),e.getKind(),e.getAmount().toPlainString(),e.getPaidAt(),e.getMethod(),own?null:e.getTransactionReference(),
    e.getReversesId()==null?null:history.stream().filter(o->Objects.equals(o.getId(),e.getReversesId())).map(PayrollSettlement::getInternalReference).findFirst().orElse(null),
    own?null:e.getReason(),own?null:e.getCreatedBy().toString(),e.getCreatedAt())).toList();
  return new Summary(paid.toPlainString(),remaining==null?null:remaining.toPlainString(),state,result,own?null:row.getCorrectionReason(),own||row.getCorrectedBy()==null?null:row.getCorrectedBy().toString(),own?null:row.getCorrectedAt());
 }
 private Payroll get(long id){return payrolls.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payroll unavailable"));}
 private Payroll locked(long id){Payroll row=payrolls.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Payroll unavailable"));if(entityManager!=null)entityManager.refresh(row,LockModeType.PESSIMISTIC_WRITE);return row;}
 private List<PayrollSettlement> history(long id){return new ArrayList<>(entries.findByPayrollIdOrderByCreatedAtAscIdAsc(id));}
 private static String trim(String s){return s==null||s.isBlank()?null:s.trim();}
 private static void uuid(String value){try{if(!UUID.fromString(value).toString().equals(value))throw new IllegalArgumentException();}catch(RuntimeException e){throw new IllegalArgumentException("Canonical UUID required");}}
 private static ResponseStatusException conflict(){return new ResponseStatusException(HttpStatus.CONFLICT,"Payroll operation conflicts with its current state or settlement history");}
}
