package com.crm.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record QuoteRequest(
        @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}") String customerId,
        LocalDate issueDate,
        LocalDate expiryDate,
        @NotEmpty List<@Valid Line> lines,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal tax,
        @Size(max = 10) String currency,
        @Pattern(regexp = "DRAFT|SENT|ACCEPTED|REJECTED") String status,
        @Size(max = 500) String notes) {
    public record Line(
            @NotBlank @Size(max = 255) String description,
            @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal quantity,
            @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal unitPrice) { }
}
