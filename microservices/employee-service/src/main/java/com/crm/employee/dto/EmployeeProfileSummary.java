package com.crm.employee.dto;

import java.time.LocalDate;

/** Internal professional contract, intentionally independent of EmployeeResponse. */
public record EmployeeProfileSummary(String employeeId, String firstName, String lastName,
        String jobTitle, String departmentId, LocalDate hireDate) {}
