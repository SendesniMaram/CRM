package com.crm.invoice.controller;
import com.crm.invoice.service.IInvoiceService;
import com.crm.invoice.pdf.PdfReport;
import static com.crm.invoice.pdf.PdfReport.*;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/invoices")
public class InvoicePdfController {
 private final IInvoiceService service;
 public InvoicePdfController(IInvoiceService service) { this.service = service; }
 @GetMapping(value="/{id}/pdf", produces="application/pdf")
 public ResponseEntity<byte[]> download(@PathVariable Long id) {
  PdfReport.requireAdmin();
  var r = service.getInvoiceById(id);
  return attachment(render("Facture", "Référence interne : " + id, List.of(
   new Row("Numéro", value(r.getInvoiceNumber())),
   new Row("Client (référence)", value(r.getCustomerId())),
   new Row("Date d’émission", date(r.getIssueDate())),
   new Row("Échéance", date(r.getDueDate())),
   new Row("Sous-total", amount(r.getSubtotal(), r.getCurrency())),
   new Row("Taxe", amount(r.getTax(), r.getCurrency())),
   new Row("Total enregistré", amount(r.getTotalAmount(), r.getCurrency())),
   new Row("Statut", value(r.getStatus())),
   new Row("Notes", value(r.getNotes()))
  )), "facture-" + id + ".pdf");
 }
}
