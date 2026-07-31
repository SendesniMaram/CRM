package com.crm.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayrollResponse {

    private Long id;

    private String employeeId;

    private BigDecimal baseSalary;

    private BigDecimal bonuses;

    private BigDecimal deductions;

    private BigDecimal taxAmount;

    private BigDecimal netSalary;

    private String payPeriod;

    private LocalDate paymentDate;

    private String currency;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

