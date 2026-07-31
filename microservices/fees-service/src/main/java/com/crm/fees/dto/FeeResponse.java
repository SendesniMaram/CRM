package com.crm.fees.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeResponse {

    private Long id;

    private String employeeId;

    private String customerId;

    private String serviceName;

    private String description;

    private BigDecimal hourlyRate;

    private BigDecimal workedHours;

    private BigDecimal amount;

    private String invoiceStatus;

    private String paymentStatus;

    private LocalDate workDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
