package com.crm.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {

    private Long id;

    private String employeeCode;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private String jobTitle;

    private LocalDate hireDate;

    private BigDecimal salary;

    private String address;

    private LocalDate dateOfBirth;

    private String gender;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

