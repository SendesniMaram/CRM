package com.crm.hr.controller;
import com.crm.hr.service.ILeaveService;
import com.crm.hr.pdf.PdfReport;
import static com.crm.hr.pdf.PdfReport.*;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/leaves")
public class LeavePdfController {
 private final ILeaveService service;
 public LeavePdfController(ILeaveService service) { this.service = service; }
 @GetMapping(value="/{id}/pdf", produces="application/pdf")
 public ResponseEntity<byte[]> download(@PathVariable Long id) {
  // The existing service checks the authenticated employee association before returning data.
  var r = service.getLeaveById(id);
  return attachment(render("Fiche de congé", "Référence interne : " + id, List.of(
   new Row("Employé (référence)", value(r.getEmployeeId())),
   new Row("Début", date(r.getStartDate())),
   new Row("Fin", date(r.getEndDate())),
   new Row("Motif", value(r.getReason())),
   new Row("Statut", value(r.getStatus())),
   new Row("Décision par", value(("APPROVED".equals(r.getStatus()) || "REJECTED".equals(r.getStatus())) ? r.getValidatorUsername() : null))
  )), "conge-" + id + ".pdf");
 }
}
