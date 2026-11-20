package com.crm.invoice;

import com.crm.invoice.client.InvoiceSourceClient;
import com.crm.invoice.entity.*;
import com.crm.invoice.mapper.InvoiceMapper;
import com.crm.invoice.repository.*;
import com.crm.invoice.service.*;
import com.crm.invoice.service.impl.InvoiceServiceImpl;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real local Hibernate/H2 transactions. No migrations, MySQL or CRM network. */
class InvoiceReconciliationFixtureTest {
 SessionFactory factory; EntityManager em; TransactionTemplate tx;
 InvoiceRepository invoices; InvoiceReconciliationRepository jobs; InvoiceFeeClaimRepository claims;
 InvoiceSourceClient source; InvoiceReconciliationService service;
 @BeforeEach void setup() {
  InvoicePresentationPolicyTest.role("ADMIN");
  var cfg=new Configuration();
  for(var type:List.of(Invoice.class,InvoiceLine.class,InvoicePayment.class,InvoiceReminder.class,
    InvoiceNumberSequence.class,InvoicePresentation.class,InvoiceDocument.class,ReminderPolicy.class,
    ReminderTask.class,InvoiceReconciliation.class,InvoiceFeeClaim.class))cfg.addAnnotatedClass(type);
  cfg.setProperty("hibernate.connection.driver_class","org.h2.Driver");
  cfg.setProperty("hibernate.connection.url","jdbc:h2:mem:invoice_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");
  cfg.setProperty("hibernate.hbm2ddl.auto","create-drop");cfg.setProperty("hibernate.show_sql","false");
  factory=cfg.buildSessionFactory();em=SharedEntityManagerCreator.createSharedEntityManager(factory);
  var manager=new JpaTransactionManager(factory);tx=new TransactionTemplate(manager);
  var repositories=new JpaRepositoryFactory(em);
  invoices=repositories.getRepository(InvoiceRepository.class);
  jobs=repositories.getRepository(InvoiceReconciliationRepository.class);
  claims=repositories.getRepository(InvoiceFeeClaimRepository.class);
  var seq=repositories.getRepository(InvoiceNumberSequenceRepository.class);
  var payments=repositories.getRepository(InvoicePaymentRepository.class);
  var reminders=repositories.getRepository(InvoiceReminderRepository.class);
  tx.executeWithoutResult(s->{var n=new InvoiceNumberSequence();n.setId(1);n.setNextValue(1L);em.persist(n);});
  source=mock(InvoiceSourceClient.class);
  when(source.get(11L,"Bearer synthetic")).thenReturn(fee("11","101","TND","30.85"));
  when(source.get(12L,"Bearer synthetic")).thenReturn(fee("12","101","TND","30.85"));
  service=new InvoiceReconciliationService(jobs,claims,invoices,new InvoiceServiceImpl(invoices,new InvoiceMapper(),seq,payments,reminders),source,manager);
 }
 InvoiceSourceClient.Fee fee(String id,String customer,String currency,String amount){return new InvoiceSourceClient.Fee(id,customer,currency,"Consulting","21","2.50","12.34",amount);}
 InvoiceReconciliationService.Input input(String id,String...fees){return new InvoiceReconciliationService.Input(id,"101","TND",List.of(fees),LocalDate.of(2026,11,7),LocalDate.of(2026,12,7),new BigDecimal("1.00"));}
 @AfterEach void close(){org.springframework.security.core.context.SecurityContextHolder.clearContext();if(factory!=null)factory.close();}
 @Test void createsOneDraftWithRealChildInsertsAndImmutableSourceLinks(){
  var request=input(UUID.randomUUID().toString(),"11","12");var result=service.execute(request,"Bearer synthetic");
  assertEquals("RECONCILED",result.state());assertNotNull(result.invoiceId());
  tx.executeWithoutResult(s->{var row=invoices.findById(Long.valueOf(result.invoiceId())).orElseThrow();assertEquals("DRAFT",row.getStatus());assertFalse(row.isIssued());assertEquals(new BigDecimal("61.70"),row.getSubtotal());assertEquals(new BigDecimal("62.70"),row.getTotalAmount());assertEquals(2,row.getLines().size());assertTrue(row.getLines().stream().allMatch(l->l.getInvoiceId().equals(row.getId())&&l.getId()!=null));assertTrue(claims.findAll().stream().allMatch(c->c.getLineId()!=null&&c.getInvoiceId().equals(row.getId())));});
  assertEquals(result.invoiceId(),service.execute(request,"Bearer synthetic").invoiceId());assertEquals(1,invoices.count());verify(source,times(1)).get(11L,"Bearer synthetic");
 }
 @Test void anotherRequestCannotClaimAConfirmedFee(){service.execute(input(UUID.randomUUID().toString(),"11"),"Bearer synthetic");assertThrows(ResponseStatusException.class,()->service.execute(input(UUID.randomUUID().toString(),"11"),"Bearer synthetic"));assertEquals(1,invoices.count());assertEquals(1,jobs.count());}
 @Test void sourceFailureIsDurableAndRetryKeepsTheSameReservation(){
  var request=input(UUID.randomUUID().toString(),"11");when(source.get(11L,"Bearer synthetic")).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
  var failed=service.execute(request,"Bearer synthetic");assertEquals("NEEDS_ATTENTION",failed.state());assertEquals(503,failed.errorStatus());assertEquals(0,invoices.count());assertEquals(request.requestId(),claims.findById(11L).orElseThrow().getJobId());
  doReturn(fee("11","101","TND","30.85")).when(source).get(11L,"Bearer synthetic");assertEquals("RECONCILED",service.retry(request.requestId(),"Bearer synthetic").state());assertEquals(1,invoices.count());
 }
 @Test void releaseBeforeConfirmationPreservesJobAndAllowsANewSelection(){
  when(source.get(11L,"Bearer synthetic")).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));var first=input(UUID.randomUUID().toString(),"11");service.execute(first,"Bearer synthetic");assertEquals("CANCELLED",service.abandon(first.requestId(),"Source unavailable").state());assertNull(claims.findById(11L).orElseThrow().getJobId());assertThrows(ResponseStatusException.class,()->service.retry(first.requestId(),"Bearer synthetic"));
  doReturn(fee("11","101","TND","30.85")).when(source).get(11L,"Bearer synthetic");var second=service.execute(input(UUID.randomUUID().toString(),"11"),"Bearer synthetic");assertEquals("RECONCILED",second.state());assertEquals(2,jobs.count());assertThrows(ResponseStatusException.class,()->service.abandon(second.id(),"Cannot release a created invoice"));
 }
 @Test void customerOrCurrencyMismatchNeverCreatesAnInvoice(){when(source.get(11L,"Bearer synthetic")).thenReturn(fee("11","102","TND","30.85"));var result=service.execute(input(UUID.randomUUID().toString(),"11"),"Bearer synthetic");assertEquals("NEEDS_ATTENTION",result.state());assertEquals(409,result.errorStatus());assertEquals(0,invoices.count());assertNull(claims.findById(11L).orElseThrow().getInvoiceId());}
 @Test void changedPayloadAndDuplicateSourcesAreRefused(){var id=UUID.randomUUID().toString();service.execute(input(id,"11"),"Bearer synthetic");assertThrows(ResponseStatusException.class,()->service.execute(input(id,"12"),"Bearer synthetic"));assertThrows(IllegalArgumentException.class,()->service.execute(input(UUID.randomUUID().toString(),"12","12"),"Bearer synthetic"));assertEquals(1,invoices.count());}
 @Test void failedFinalTransactionRollsBackInvoiceAndSequenceThenCanRetry(){
  var request=input(UUID.randomUUID().toString(),"11");var spied=mock(InvoiceRepository.class);when(spied.findById(anyLong())).thenAnswer(c->invoices.findById(c.getArgument(0)));doThrow(new org.springframework.dao.DataIntegrityViolationException("synthetic constraint")).when(spied).saveAndFlush(any(Invoice.class));
  var repositories=new JpaRepositoryFactory(em);var backend=new InvoiceServiceImpl(invoices,new InvoiceMapper(),repositories.getRepository(InvoiceNumberSequenceRepository.class),repositories.getRepository(InvoicePaymentRepository.class),repositories.getRepository(InvoiceReminderRepository.class));
  var failing=new InvoiceReconciliationService(jobs,claims,spied,backend,source,new JpaTransactionManager(factory));var failed=failing.execute(request,"Bearer synthetic");assertEquals("NEEDS_ATTENTION",failed.state());assertEquals(409,failed.errorStatus());assertEquals(0,invoices.count());assertNull(claims.findById(11L).orElseThrow().getInvoiceId());assertEquals(1L,repositories.getRepository(InvoiceNumberSequenceRepository.class).findById(1).orElseThrow().getNextValue());assertEquals("RECONCILED",service.retry(request.requestId(),"Bearer synthetic").state());
 }
 @Test void personalRolesCannotReserveAnything(){for(String role:List.of("CLIENT","EMPLOYEE")){InvoicePresentationPolicyTest.role(role);assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.execute(input(UUID.randomUUID().toString(),"11"),"Bearer synthetic"));}assertEquals(0,jobs.count());verifyNoInteractions(source);}
 @Test void archiveKeepsActualLinesPaymentsClaimsAndFrozenPdf(){
  var result=service.execute(input(UUID.randomUUID().toString(),"11"),"Bearer synthetic");long id=Long.parseLong(result.invoiceId());var repos=new JpaRepositoryFactory(em);var payments=repos.getRepository(InvoicePaymentRepository.class);
  var documents=new InvoicePresentationService(repos.getRepository(InvoicePresentationRepository.class),invoices,repos.getRepository(InvoiceDocumentRepository.class),payments);
  var template=tx.execute(s->documents.save(null,new InvoicePresentationService.Input("Brand","Header","Company","Footer","Mention","DETAIL")));
  byte[] frozen=tx.execute(s->documents.issue(id,Long.parseLong(template.id())));
  tx.executeWithoutResult(s->{var invoice=invoices.findById(id).orElseThrow();var payment=new InvoicePayment();payment.setInvoiceId(id);payment.setAmount(invoice.getTotalAmount());payment.setPaidAt(LocalDate.of(2026,11,7));payments.saveAndFlush(payment);invoice.setStatus("PAID");});
  var followup=new InvoiceFollowupService(invoices,payments,repos.getRepository(ReminderPolicyRepository.class),repos.getRepository(ReminderTaskRepository.class),repos.getRepository(InvoiceReminderRepository.class),java.time.Clock.systemUTC());
  tx.executeWithoutResult(s->followup.archive(id));
  tx.executeWithoutResult(s->{var invoice=invoices.findById(id).orElseThrow();assertTrue(invoice.isArchived());assertEquals(1,invoice.getLines().size());assertEquals(1,payments.count());assertEquals(id,claims.findById(11L).orElseThrow().getInvoiceId());assertArrayEquals(frozen,documents.snapshot(id));});
 }
}
