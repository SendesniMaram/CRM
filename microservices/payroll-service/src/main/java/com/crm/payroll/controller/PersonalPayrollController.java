package com.crm.payroll.controller;

import com.crm.payroll.dto.PayrollResponse;
import com.crm.payroll.service.PersonalPayrollService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.crm.payroll.pdf.PdfReport.*;

@RestController
public class PersonalPayrollController {
    private final PersonalPayrollService payrolls;

    public PersonalPayrollController(PersonalPayrollService payrolls) {
        this.payrolls = payrolls;
    }

    @GetMapping("/api/payrolls/me")
    public ResponseEntity<Page<PayrollResponse>> list(Authentication authentication,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String period, HttpServletRequest request) {
        if (request.getParameterMap().keySet().stream()
                .anyMatch(key -> !List.of("page", "size", "period").contains(key))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Unsupported payroll filter");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(payrolls.list(authentication, authorization, page, size, period));
    }

    @GetMapping("/api/payrolls/me/{id}")
    public ResponseEntity<PayrollResponse> read(Authentication authentication,
            @RequestHeader("Authorization") String authorization, @PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(payrolls.read(authentication, authorization, id));
    }

    @GetMapping(value = "/api/payrolls/me/{id}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> pdf(Authentication authentication,
            @RequestHeader("Authorization") String authorization, @PathVariable Long id) {
        var row = payrolls.read(authentication, authorization, id);
        byte[] document = render("Bulletin de paie", "Période : " + value(row.getPayPeriod()), List.of(
                new Row("Date de paiement", date(row.getPaymentDate())),
                new Row("Salaire de base", amount(row.getBaseSalary(), row.getCurrency())),
                new Row("Primes", amount(row.getBonuses(), row.getCurrency())),
                new Row("Retenues", amount(row.getDeductions(), row.getCurrency())),
                new Row("Impôt", amount(row.getTaxAmount(), row.getCurrency())),
                new Row("Net enregistré", amount(row.getNetSalary(), row.getCurrency())),
                new Row("Statut", value(row.getStatus()))));
        return attachment(document, "bulletin-" + row.getPayPeriod() + ".pdf");
    }
}
