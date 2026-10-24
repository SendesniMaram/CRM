package com.crm.hr.dto;

import java.time.LocalDate;

public record EmployeeProfileSummary(String employeeId, String firstName, String lastName,
        String jobTitle, String departmentId, LocalDate hireDate) {}
