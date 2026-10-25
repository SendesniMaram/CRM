package com.crm.employee.repository;

import java.time.LocalDate;

/** Scalar constructor projection: no Employee entity or sensitive columns are hydrated. */
public record EmployeeProfileProjection(Long employeeId, String firstName, String lastName,
        String jobTitle, Long departmentId, LocalDate hireDate) {}
