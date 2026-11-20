package com.crm.invoice.dto;

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
import jakarta.validation.constraints.AssertTrue;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRequest {

    @Size(max = 50, message = "Invoice number must be at most 50 characters")
    private String invoiceNumber;

    @NotBlank(message = "Customer ID is required")
    @Size(max = 50, message = "Customer ID must be at most 50 characters")
    private String customerId;

    private LocalDate issueDate;

    private LocalDate dueDate;

    @NotNull(message = "Subtotal is required")
    @Positive(message = "Subtotal must be positive")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal subtotal;

    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    private BigDecimal tax;

    @Size(max = 10, message = "Currency must be at most 10 characters")
    private String currency;

    @NotBlank(message = "Status is required")
    @Size(max = 50, message = "Status must be at most 50 characters")
    @Pattern(regexp = "DRAFT|PENDING|UNPAID|PARTIALLY_PAID|PAID", message = "Unsupported invoice status")
    private String status;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    private String notes;
    @AssertTrue(message = "Due date must not precede issue date")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isDateRangeValid() {
        return issueDate == null || dueDate == null || !dueDate.isBefore(issueDate);
    }
}
