package com.crm.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayrollRequest {

    @NotBlank(message = "Employee ID is required")
    @Size(max = 50, message = "Employee ID must be at most 50 characters")
    private String employeeId;

    @NotNull(message = "Base salary is required")
    @Positive(message = "Base salary must be positive")
    private BigDecimal baseSalary;

    private BigDecimal bonuses;

    private BigDecimal deductions;

    private BigDecimal taxAmount;

    @NotBlank(message = "Pay period is required")
    @Size(max = 50, message = "Pay period must be at most 50 characters")
    private String payPeriod;

    private LocalDate paymentDate;

    @Size(max = 10, message = "Currency must be at most 10 characters")
    private String currency;

    @NotBlank(message = "Status is required")
    @Size(max = 50, message = "Status must be at most 50 characters")
    private String status;
}

