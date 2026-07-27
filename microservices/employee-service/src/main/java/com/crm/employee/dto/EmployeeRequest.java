package com.crm.employee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequest {

    @NotBlank(message = "Employee code is required")
    @Size(max = 255, message = "Employee code must be at most 255 characters")
    private String employeeCode;

    @Size(max = 255, message = "First name must be at most 255 characters")
    private String firstName;

    @Size(max = 255, message = "Last name must be at most 255 characters")
    private String lastName;

    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @Size(max = 50, message = "Phone must be at most 50 characters")
    private String phone;

    @Size(max = 255, message = "Job title must be at most 255 characters")
    private String jobTitle;

    @PastOrPresent(message = "Hire date must be in the past or present")
    private LocalDate hireDate;

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Salary must be >= 0")
    private BigDecimal salary;

    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;

    private LocalDate dateOfBirth;

    @Size(max = 50, message = "Gender must be at most 50 characters")
    private String gender;

    @Size(max = 50, message = "Status must be at most 50 characters")
    private String status;
}

