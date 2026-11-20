package com.crm.payroll;
import com.crm.payroll.entity.*;
import com.crm.payroll.dto.PayrollRequest;
import com.crm.payroll.repository.*;
import com.crm.payroll.service.*;
import com.crm.payroll.service.impl.PayrollServiceImpl;
import com.crm.payroll.mapper.PayrollMapper;
import com.crm.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayrollSettlementPolicyTest {
 PayrollRepository payrolls;PayrollSettlementRepository entries;PersonalPayrollService own;PayrollSettlementService service;PayrollServiceImpl crud;Payroll row;
 final List<PayrollSettlement> history=new ArrayList<>();
 static UsernamePasswordAuthenticationToken auth(String role){var roles=List.of(new SimpleGrantedAuthority("ROLE_"+role));return new UsernamePasswordAuthenticationToken(new AuthenticatedUserPrincipal(42,"operator",roles),null,roles);}
 @BeforeEach void setup(){
  payrolls=mock(PayrollRepository.class);entries=mock(PayrollSettlementRepository.class);own=mock(PersonalPayrollService.class);service=new PayrollSettlementService(payrolls,entries,own);crud=new PayrollServiceImpl(payrolls,new PayrollMapper(),entries);
  row=new Payroll();row.setId(7L);row.setEmployeeId("103");row.setBaseSalary(new BigDecimal("100.00"));row.setNetSalary(new BigDecimal("100.00"));row.setPayPeriod("2026-11");row.setStatus("PENDING");row.setPublished(true);
  when(payrolls.lockById(7L)).thenReturn(Optional.of(row));when(payrolls.findById(7L)).thenReturn(Optional.of(row));
  when(entries.findByPayrollIdOrderByCreatedAtAscIdAsc(7L)).thenAnswer(c->new ArrayList<>(history));
  when(entries.findByRequestId(anyString())).thenAnswer(c->history.stream().filter(e->e.getRequestId().equals(c.getArgument(0))).findFirst());
  when(entries.existsByReversesId(anyLong())).thenAnswer(c->history.stream().anyMatch(e->Objects.equals(e.getReversesId(),c.getArgument(0))));
  when(entries.saveAndFlush(any())).thenAnswer(c->{PayrollSettlement e=c.getArgument(0);e.setId((long)history.size()+1);e.setCreatedAt(LocalDateTime.now());history.add(e);return e;});
 }
 PayrollSettlementService.PaymentRequest payment(String amount){return new PayrollSettlementService.PaymentRequest(UUID.randomUUID().toString(),new BigDecimal(amount),LocalDate.of(2026,11,2),"MANUAL",null);}
 void conflict(Runnable action){assertEquals(409,assertThrows(ResponseStatusException.class,action::run).getStatusCode().value());}
 @Test void partialThenFullUpdatesBalanceAndStatus(){var partial=service.pay(auth("ADMIN"),7,payment("40"));assertEquals("PARTIAL",partial.state());assertEquals(0,new BigDecimal(partial.remainingAmount()).compareTo(new BigDecimal("60")));assertEquals("PENDING",row.getStatus());var full=service.pay(auth("ADMIN"),7,payment("60"));assertEquals("SETTLED",full.state());assertEquals("PAID",row.getStatus());assertEquals(LocalDate.of(2026,11,2),row.getPaymentDate());assertEquals(2,history.size());}
 @Test void overpaymentDoesNotAppend(){conflict(()->service.pay(auth("ADMIN"),7,payment("100.01")));assertTrue(history.isEmpty());}
 @Test void identicalRequestIsIdempotent(){var request=payment("25");service.pay(auth("ADMIN"),7,request);service.pay(auth("ADMIN"),7,request);assertEquals(1,history.size());}
 @Test void reusedRequestWithChangedAmountConflicts(){var request=payment("25");service.pay(auth("ADMIN"),7,request);conflict(()->service.pay(auth("ADMIN"),7,new PayrollSettlementService.PaymentRequest(request.requestId(),BigDecimal.TEN,request.paidAt(),request.method(),null)));assertEquals(1,history.size());}
 @Test void repeatedTransactionReferenceConflicts(){when(entries.existsByPayrollIdAndTransactionReference(7L,"bank-ref")).thenReturn(true);conflict(()->service.pay(auth("ADMIN"),7,new PayrollSettlementService.PaymentRequest(UUID.randomUUID().toString(),BigDecimal.ONE,LocalDate.now(),null,"bank-ref")));assertTrue(history.isEmpty());}
 @Test void reversalPreservesOriginalAndReopensDue(){service.pay(auth("ADMIN"),7,payment("100"));var original=history.getFirst();var result=service.reverse(auth("ADMIN"),7,original.getInternalReference(),new PayrollSettlementService.ReversalRequest(UUID.randomUUID().toString(),"Saisie incorrecte"));assertEquals(2,history.size());assertEquals("PAYMENT",original.getKind());assertEquals(new BigDecimal("100"),original.getAmount());assertEquals(original.getId(),history.get(1).getReversesId());assertEquals("PENDING",row.getStatus());assertNull(row.getPaymentDate());assertEquals("UNPAID",result.state());}
 @Test void reversalRetryIsIdempotentAndSecondDistinctReversalIsRejected(){service.pay(auth("ADMIN"),7,payment("10"));String ref=history.getFirst().getInternalReference();var reversal=new PayrollSettlementService.ReversalRequest(UUID.randomUUID().toString(),"Erreur");service.reverse(auth("ADMIN"),7,ref,reversal);service.reverse(auth("ADMIN"),7,ref,reversal);conflict(()->service.reverse(auth("ADMIN"),7,ref,new PayrollSettlementService.ReversalRequest(UUID.randomUUID().toString(),"Autre")));assertEquals(2,history.size());}
 @Test void historicalPaidWithoutLedgerNeedsReconciliation(){row.setStatus("PAID");assertNull(service.administration(auth("ADMIN"),7).remainingAmount());assertEquals("LEGACY_UNRECONCILED",service.administration(auth("ADMIN"),7).state());conflict(()->service.pay(auth("ADMIN"),7,payment("1")));assertTrue(history.isEmpty());}
 @Test void publicationIsNotPayment(){assertEquals("UNPAID",service.administration(auth("ADMIN"),7).state());assertEquals("PENDING",row.getStatus());}
 @Test void unpublishedAndCancelledCannotBePaid(){row.setPublished(false);conflict(()->service.pay(auth("ADMIN"),7,payment("1")));row.setPublished(true);row.setCancelled(true);conflict(()->service.pay(auth("ADMIN"),7,payment("1")));}
 @Test void zeroNetHasNoFictitiousPayment(){row.setNetSalary(BigDecimal.ZERO);assertEquals("NO_AMOUNT_DUE",service.administration(auth("ADMIN"),7).state());conflict(()->service.pay(auth("ADMIN"),7,payment("1")));assertEquals("PENDING",row.getStatus());}
 @Test void invalidAmountsAndIdsDoNotWrite(){for(String amount:List.of("0","-1","0.001","10000000000"))assertThrows(IllegalArgumentException.class,()->service.pay(auth("ADMIN"),7,payment(amount)));var bad=new PayrollSettlementService.PaymentRequest("not-uuid",BigDecimal.ONE,LocalDate.now(),null,null);assertThrows(IllegalArgumentException.class,()->service.pay(auth("ADMIN"),7,bad));assertTrue(history.isEmpty());}
 @Test void nonAdministratorsCannotWriteOrReadAdministrativeData(){for(String role:List.of("EMPLOYEE","CLIENT")){assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.pay(auth(role),7,payment("1")));assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.administration(auth(role),7));}verifyNoInteractions(payrolls);}
 @Test void personalReadChecksOwnershipBeforeReadingLedger(){when(own.read(any(),anyString(),anyLong())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));assertThrows(ResponseStatusException.class,()->service.own(auth("EMPLOYEE"),"Bearer token",7));verifyNoInteractions(payrolls,entries);}
 @Test void personalHistoryOmitsAdministrativeAuthorReasonAndBankReference(){service.pay(auth("ADMIN"),7,payment("10"));var result=service.own(auth("EMPLOYEE"),"Bearer token",7);assertNull(result.entries().getFirst().author());assertNull(result.entries().getFirst().reason());assertNull(result.entries().getFirst().transactionReference());verify(own).read(any(),eq("Bearer token"),eq(7L));}
 @Test void persistenceFailureIsPropagated(){doThrow(new org.springframework.dao.DataIntegrityViolationException("private SQL")).when(entries).saveAndFlush(any());assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->service.pay(auth("ADMIN"),7,payment("1")));assertEquals("PENDING",row.getStatus());}
 @Test void publishedPayrollCannotBeEditedDeletedOrUnpublished(){var request=request();conflict(()->crud.updatePayroll(7L,request));conflict(()->crud.deletePayroll(7L));conflict(()->crud.setPublished(7L,false));verify(payrolls,never()).delete(any(Payroll.class));}
 @Test void correctionKeepsOriginalAndCreatesLinkedDraft(){when(payrolls.saveAndFlush(any())).thenAnswer(c->{Payroll p=c.getArgument(0);p.setId(8L);return p;});var replacement=crud.correct(auth("ADMIN"),7L,"Erreur de prime");assertEquals(8L,replacement.getId());assertFalse(replacement.isPublished());assertEquals(7L,replacement.getCorrectionOfId());assertTrue(row.isCancelled());assertEquals(8L,row.getReplacementPayrollId());assertEquals(42L,row.getCorrectedBy());assertEquals(new BigDecimal("100.00"),row.getNetSalary());conflict(()->crud.correct(auth("ADMIN"),7L,"Encore"));}
 @Test void paymentBeforeCorrectionBlocksCorrection(){service.pay(auth("ADMIN"),7,payment("1"));conflict(()->crud.correct(auth("ADMIN"),7L,"Erreur"));verify(payrolls,never()).saveAndFlush(any());}
 @Test void correctionBeforePaymentBlocksPayment(){row.setCancelled(true);conflict(()->service.pay(auth("ADMIN"),7,payment("1")));}
 @Test void lockPrecedesSettlementReadAndWrite(){service.pay(auth("ADMIN"),7,payment("1"));var order=inOrder(payrolls,entries);order.verify(payrolls).lockById(7L);order.verify(entries).findByPayrollIdOrderByCreatedAtAscIdAsc(7L);order.verify(entries).findByRequestId(anyString());order.verify(entries).saveAndFlush(any());}
 @Test void manualPaidCrudIsRejected(){var request=request();request.setStatus("PAID");conflict(()->crud.createPayroll(request));}
 PayrollRequest request(){var r=new PayrollRequest();r.setEmployeeId("103");r.setBaseSalary(BigDecimal.TEN);r.setPayPeriod("2026-11");r.setStatus("PENDING");return r;}
}
