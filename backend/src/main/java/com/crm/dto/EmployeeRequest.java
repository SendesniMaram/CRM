package com.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.crm.enums.EmployeeStatus;
import com.crm.enums.Gender;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequest {

    @NotBlank(message = "employeeCode is required")
    @Size(max = 255, message = "employeeCode must be at most 255 characters")
    private String employeeCode;

    @Size(max = 255, message = "jobTitle must be at most 255 characters")
    private String jobTitle;

    @PastOrPresent(message = "hireDate must be in the past or present")
    private LocalDate hireDate;

    @NotNull(message = "salary is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "salary must be >= 0")
    private BigDecimal salary;

    @Size(max = 500, message = "address must be at most 500 characters")
    private String address;

    private LocalDate dateOfBirth;

    @NotNull(message = "gender is required")
    private Gender gender;

    @NotNull(message = "status is required")
    private EmployeeStatus status;

    @NotNull(message = "departmentId is required")
    private Long departmentId;

    @NotNull(message = "userId is required")
    private Long userId;

    // For API symmetry (not used by persistence directly, but kept for compatibility if frontend sends it)
    private LocalDateTime createdAt;
}

