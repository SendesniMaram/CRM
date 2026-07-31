package com.crm.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private Long id;

    private String invoiceNumber;

    private String customerId;

    private LocalDate issueDate;

    private LocalDate dueDate;

    private BigDecimal subtotal;

    private BigDecimal tax;

    private BigDecimal totalAmount;

    private String currency;

    private String status;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
