package com.crm.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
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
    @Digits(integer = 10, fraction = 2)
    private BigDecimal baseSalary;

    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    private BigDecimal bonuses;

    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    private BigDecimal deductions;

    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    private BigDecimal taxAmount;

    @PositiveOrZero @Digits(integer=6,fraction=2)
    private BigDecimal overtimeHours;
    @PositiveOrZero @Digits(integer=10,fraction=2)
    private BigDecimal overtimeHourlyRate;


    @NotBlank(message = "Pay period is required")
    @Size(max = 50, message = "Pay period must be at most 50 characters")
    @Pattern(regexp = "[0-9]{4}-(0[1-9]|1[0-2])", message = "Period must be YYYY-MM")
    private String payPeriod;

    private LocalDate paymentDate;

    @Size(max = 10, message = "Currency must be at most 10 characters")
    private String currency;

    @NotBlank(message = "Status is required")
    @Size(max = 50, message = "Status must be at most 50 characters")
    @Pattern(regexp = "PENDING|PAID", message = "Unsupported payroll status")
    private String status;
}

