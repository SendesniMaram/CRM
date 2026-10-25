package com.crm.employee.service;

import com.crm.employee.dto.EmployeeProfileSummary;
import com.crm.employee.exception.ResourceNotFoundException;
import com.crm.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeProfileSummaryService {
    private final EmployeeRepository employees;
    public EmployeeProfileSummaryService(EmployeeRepository employees) { this.employees = employees; }

    @Transactional(readOnly = true)
    public EmployeeProfileSummary read(long employeeId) {
        if (employeeId <= 0) throw new IllegalArgumentException("Canonical Employee identifier required");
        var row = employees.findProfileSummaryById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        if (row.employeeId() == null || row.employeeId() != employeeId
                || (row.departmentId() != null && row.departmentId() <= 0)) {
            throw new IllegalStateException("Invalid professional projection");
        }
        return new EmployeeProfileSummary(row.employeeId().toString(), row.firstName(), row.lastName(), row.jobTitle(),
                row.departmentId() == null ? null : row.departmentId().toString(), row.hireDate());
    }
}
