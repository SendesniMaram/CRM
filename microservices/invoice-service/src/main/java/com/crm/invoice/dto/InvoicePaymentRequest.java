package com.crm.invoice.dto;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoicePaymentRequest(
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull LocalDate paidAt,
        @Size(max = 50) String method,
        @Size(max = 100) String reference,
        @Size(max = 500) String notes) { }
