package com.crm.invoice.controller;
import com.crm.invoice.service.PersonalInvoiceService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.*;
import jakarta.servlet.http.HttpServletRequest;
import static com.crm.invoice.pdf.PdfReport.*;
@RestController @RequestMapping("/api/invoices/me")
public class PersonalInvoiceController {
 private final PersonalInvoiceService service;
 @org.springframework.beans.factory.annotation.Autowired(required=false) private com.crm.invoice.service.InvoicePresentationService documents;
 public PersonalInvoiceController(PersonalInvoiceService s){service=s;}
 @GetMapping public ResponseEntity<PersonalInvoiceService.PageResult> list(Authentication a,@RequestHeader("Authorization") String b,
  @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,HttpServletRequest r){
  if(r.getParameterMap().keySet().stream().anyMatch(k->!java.util.Set.of("page","size").contains(k))) throw new IllegalArgumentException("Target parameters are not allowed");
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(a,b,page,size));
 }
 @GetMapping("/statistics") public ResponseEntity<PersonalInvoiceService.PersonalStatistics> statistics(Authentication a,@RequestHeader("Authorization") String b){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.statistics(a,b));
 }
 @GetMapping("/{id}") public ResponseEntity<PersonalInvoiceService.Summary> detail(Authentication a,@RequestHeader("Authorization") String b,@PathVariable String id,HttpServletRequest r){
  noTarget(r); return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.detail(a,b,id));
 }
 @GetMapping(value="/{id}/pdf",produces="application/pdf")
 public ResponseEntity<byte[]> pdf(Authentication a,@RequestHeader("Authorization") String b,@PathVariable String id,HttpServletRequest r){
  noTarget(r); var v=service.detail(a,b,id);
  if(documents!=null){var frozen=documents.snapshot(Long.parseLong(v.id()));if(frozen!=null)return attachment(frozen,"facture-"+v.id()+".pdf");}
  return attachment(render("Facture", "Reference : "+v.id(),java.util.List.of(
   new Row("Numero",value(v.invoiceNumber())),new Row("Emission",date(v.issueDate())),new Row("Echeance",date(v.dueDate())),
   new Row("Total",value(v.totalAmount())+" "+value(v.currency())),new Row("Statut",value(v.status())))), "facture-"+v.id()+".pdf");
 }
 @GetMapping("/{id}/payments") public ResponseEntity<java.util.List<PersonalInvoiceService.Payment>> payments(Authentication a,@RequestHeader("Authorization") String bearer,@PathVariable String id,HttpServletRequest request){
  noTarget(request);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.payments(a,bearer,id));
 }
 private void noTarget(HttpServletRequest r){if(!r.getParameterMap().isEmpty())throw new IllegalArgumentException("Target parameters are not allowed");}
}
