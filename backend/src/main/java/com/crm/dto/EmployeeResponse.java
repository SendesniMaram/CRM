package com.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.crm.enums.EmployeeStatus;
import com.crm.enums.Gender;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {

    private Long id;

    private String employeeCode;

    private String jobTitle;

    private LocalDate hireDate;

    private BigDecimal salary;

    private String address;

    private LocalDate dateOfBirth;

    private Gender gender;

    private EmployeeStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long departmentId;
    private String departmentName;

    private Long userId;
    private String username;
}

