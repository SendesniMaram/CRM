package com.crm.hr.controller;

import com.crm.hr.dto.EmployeeContractRequest;
import com.crm.hr.entity.EmployeeContract;
import com.crm.hr.service.EmployeeContractService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class EmployeeContractController {
    private final EmployeeContractService service;
    public EmployeeContractController(EmployeeContractService service) { this.service = service; }
    @GetMapping public org.springframework.data.domain.Page<EmployeeContract> list(
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size,
            @RequestParam(required=false) String keyword, @RequestParam(required=false) String employeeId) {
        return service.page(page, size, keyword, employeeId);
    }
    @GetMapping("/{id}") public EmployeeContract get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public EmployeeContract create(@Valid @RequestBody EmployeeContractRequest request) { return service.create(request); }
    @PutMapping("/{id}") public EmployeeContract update(@PathVariable Long id, @Valid @RequestBody EmployeeContractRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { service.delete(id); }
}
