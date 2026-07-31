package com.crm.payroll.controller;

import com.crm.payroll.dto.PayrollRequest;
import com.crm.payroll.dto.PayrollResponse;
import com.crm.payroll.service.IPayrollService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/payrolls", produces = MediaType.APPLICATION_JSON_VALUE)
public class PayrollController {

    private final IPayrollService payrollService;

    public PayrollController(IPayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @PostMapping
    public ResponseEntity<PayrollResponse> createPayroll(@Valid @RequestBody PayrollRequest request) {
        PayrollResponse response = payrollService.createPayroll(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllPayrolls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String keyword) {

        if (page == 0 && size == 10 && sortBy == null && direction.equals("asc") && keyword == null) {
            List<PayrollResponse> payrolls = payrollService.getAllPayrolls();
            return ResponseEntity.ok(payrolls);
        }

        Page<PayrollResponse> payrollPage = payrollService.getAllPayrollsPaged(page, size, sortBy, direction, keyword);
        return ResponseEntity.ok(payrollPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayrollResponse> getPayrollById(@PathVariable Long id) {
        PayrollResponse response = payrollService.getPayrollById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PayrollResponse> updatePayroll(
            @PathVariable Long id,
            @Valid @RequestBody PayrollRequest request) {
        PayrollResponse response = payrollService.updatePayroll(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayroll(@PathVariable Long id) {
        payrollService.deletePayroll(id);
        return ResponseEntity.noContent().build();
    }
}

