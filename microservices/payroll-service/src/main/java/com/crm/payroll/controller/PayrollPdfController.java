package com.crm.payroll.controller;
import com.crm.payroll.service.IPayrollService;
import com.crm.payroll.pdf.PdfReport;
import static com.crm.payroll.pdf.PdfReport.*;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/payrolls")
public class PayrollPdfController {
 private final IPayrollService service;
 public PayrollPdfController(IPayrollService service) { this.service = service; }
 @GetMapping(value="/{id}/pdf", produces="application/pdf")
 public ResponseEntity<byte[]> download(@PathVariable Long id) {
  PdfReport.requireAdmin();
  var r = service.getPayrollById(id);
  return attachment(render("Bulletin de paie", "Référence interne : " + id, List.of(
   new Row("Employé (référence)", value(r.getEmployeeId())),
   new Row("Période", value(r.getPayPeriod())),
   new Row("Date de paiement", date(r.getPaymentDate())),
   new Row("Salaire de base", amount(r.getBaseSalary(), r.getCurrency())),
   new Row("Primes", amount(r.getBonuses(), r.getCurrency())),
   new Row("Retenues", amount(r.getDeductions(), r.getCurrency())),
   new Row("Impôt", amount(r.getTaxAmount(), r.getCurrency())),
   new Row("Net enregistré", amount(r.getNetSalary(), r.getCurrency())),
   new Row("Statut", value(r.getStatus()))
  )), "paie-" + id + ".pdf");
 }
}
